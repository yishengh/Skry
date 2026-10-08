package com.yishenghuang.skry

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.yishenghuang.skry.util.MediaAccess
import com.yishenghuang.skry.worker.FullScanWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class WorkerFlowTest {
    @Test fun repeatedStartAndContinuationProcessMoreThanOneWorkerQuota() = runBlocking {
        check(InstrumentationRegistry.getArguments().getString("skrySyntheticOnly") == "true")
        check(Build.HARDWARE in setOf("ranchu", "goldfish"))
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SkryApplication
        check(MediaAccess.hasFullGalleryAccess(app))
        app.mediaRepository.syncGallery()
        check(app.database.photoDao().observeCount().first() == 0) { "Use an empty isolated emulator" }
        val created = mutableListOf<Uri>()
        val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        val work = WorkManager.getInstance(app)
        try {
            repeat(41) { index ->
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "skry_synthetic_worker_${System.nanoTime()}_$index.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SkrySynthetic")
                }
                val uri = app.contentResolver.insert(MediaAccess.imagesCollectionUri(), values)!!
                created += uri
                app.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            }
            app.mediaRepository.syncGallery()
            app.scanPreferences.beginUserScan()
            FullScanWorker.enqueue(app, userInitiated = true)
            withTimeout(60_000) {
                while (app.database.photoDao().observeAuditedCount().first() == 0) delay(25)
            }
            // Same preference/cancellation order used by the dashboard pause action.
            app.scanPreferences.completeScan()
            work.cancelUniqueWork(FullScanWorker.UNIQUE_NAME).result.get()
            delay(500)
            FullScanWorker.resumeIfNeeded(app)
            assertFalse("A cancelled worker must not undo the user's pause", app.scanPreferences.isScanActive)
            assertTrue(work.getWorkInfosForUniqueWork(FullScanWorker.UNIQUE_NAME).get().all { it.state.isFinished })
            app.scanPreferences.beginUserScan()
            FullScanWorker.enqueue(app, userInitiated = true)
            FullScanWorker.enqueue(app, userInitiated = true)
            withTimeout(120_000) {
                while (app.database.photoDao().observeAuditedCount().first() < 41) delay(200)
                while (work.getWorkInfosForUniqueWork(FullScanWorker.UNIQUE_NAME).get().any { !it.state.isFinished }) delay(200)
            }
            assertEquals(0, app.mediaRepository.pendingCount())
            assertEquals(41, app.database.photoDao().observeAuditedCount().first())
            assertEquals(40, app.database.photoDao().observeDuplicateCandidateCount().first())
            assertFalse(app.scanPreferences.isScanActive)
        } finally {
            work.cancelUniqueWork(FullScanWorker.UNIQUE_NAME).result.get()
            app.scanPreferences.completeScan()
            bitmap.recycle()
            created.forEach { app.contentResolver.delete(it, null, null) }
            app.mediaRepository.syncGallery()
        }
    }
}
