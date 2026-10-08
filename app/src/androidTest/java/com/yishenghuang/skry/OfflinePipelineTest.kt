package com.yishenghuang.skry

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yishenghuang.skry.data.MediaRepository
import com.yishenghuang.skry.data.SkryDatabase
import com.yishenghuang.skry.domain.FindingType
import com.yishenghuang.skry.domain.Finding
import com.yishenghuang.skry.domain.MosaicEngine
import com.yishenghuang.skry.domain.PrivacyScanner
import com.yishenghuang.skry.domain.VaultService
import com.yishenghuang.skry.util.MediaAccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Invoke only with -e skrySyntheticOnly true on a dedicated Skry emulator. */
@RunWith(AndroidJUnit4::class)
class OfflinePipelineTest {
    @Test
    fun unlocatedFindingRedactsBeyondOtherBoxesAndGpsOnlyPreservesPixels() {
        requireIsolation()
        val bitmap = sample()
        val original = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        MosaicEngine.apply(bitmap, listOf(
            Finding(FindingType.CREDIT_CARD, "Card", 1f, boxLeft = 0.1f, boxTop = 0.1f,
                boxRight = 0.2f, boxBottom = 0.2f),
            Finding(FindingType.EMAIL_ADDRESS, "Email", 1f)
        ))
        // The email text is outside the card box and must not be left untouched.
        var changed = false
        for (y in 190 until 240) for (x in 50 until 900) {
            if (bitmap.getPixel(x, y) != original.getPixel(x, y)) changed = true
        }
        assertTrue(changed)
        val gpsOnly = original.copy(Bitmap.Config.ARGB_8888, true)
        MosaicEngine.apply(gpsOnly, listOf(Finding(FindingType.LOCATION_EXIF, "GPS", 1f)))
        assertTrue(original.sameAs(gpsOnly))
        bitmap.recycle(); original.recycle(); gpsOnly.recycle()
    }
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    private fun requireIsolation() {
        check(InstrumentationRegistry.getArguments().getString("skrySyntheticOnly") == "true")
        check(Build.HARDWARE in setOf("ranchu", "goldfish"))
    }

    @Test
    fun bundledOcrVaultRoundTripAndMalformedInput() = runBlocking {
        requireIsolation()
        val input = File(context.cacheDir, "synthetic-ocr.jpg")
        val bitmap = sample()
        input.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bitmap.recycle()
        val scanner = PrivacyScanner(context)
        val vault = VaultService(context)
        var stored: String? = null
        try {
            val result = scanner.scan(Uri.fromFile(input), true, 1200, 800, 0)
            assertTrue("Bundled OCR must read synthetic English text", result.ocrTextLength > 20)
            assertTrue(result.findings.any { it.type == FindingType.EMAIL_ADDRESS })
            assertTrue(result.findings.any { it.type == FindingType.CREDIT_CARD })
            assertNotNull(result.quality?.pHash)
            stored = vault.storeRedactedCopy("synthetic-test", Uri.fromFile(input), result.findings).fileName
            val decoded = android.graphics.BitmapFactory.decodeByteArray(
                vault.openDecryptedBytes(stored), 0, vault.openDecryptedBytes(stored).size)
            assertNotNull(decoded)
            decoded.recycle()
            val ciphertext = File(context.filesDir, "vault/$stored").readBytes()
            assertFalse("Ciphertext cannot be a plaintext JPEG", ciphertext.take(2) == listOf(0xff.toByte(), 0xd8.toByte()))
            input.writeText("this is not an image")
            var failed = false
            try { scanner.scan(Uri.fromFile(input), false, 0, 0, 0) } catch (_: Exception) { failed = true }
            assertTrue("Unreadable image must fail rather than be labelled audited", failed)
        } finally {
            scanner.close()
            stored?.let { vault.deleteVaultFile(it) }
            input.delete()
        }
    }

    @Test
    fun orientationIsNormalizedOnLegacyAndModernDecoders() {
        requireIsolation()
        val file = File(context.cacheDir, "synthetic-rotated.jpg")
        val source = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.RED)
        file.outputStream().use { source.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        source.recycle()
        try {
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            val decoded = MediaAccess.decodeSampledBitmap(context, Uri.fromFile(file), 200, false, true)!!
            assertEquals(80, decoded.width)
            assertEquals(120, decoded.height)
            decoded.recycle()
        } finally { file.delete() }
    }

    @Test
    fun mediaStoreAdditionExternalDeletionAndStatisticsReconcile() = runBlocking {
        requireIsolation()
        check(MediaAccess.hasGalleryAccess(context))
        val db = Room.inMemoryDatabaseBuilder(context, SkryDatabase::class.java).build()
        val repository = MediaRepository(context, db.photoDao())
        var uri: Uri? = null
        try {
            val name = "skry_synthetic_${System.nanoTime()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SkrySynthetic")
            }
            uri = context.contentResolver.insert(MediaAccess.imagesCollectionUri(), values)!!
            val bitmap = sample()
            context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            bitmap.recycle()
            repository.syncGallery()
            val id = uri.lastPathSegment!!
            assertTrue(db.photoDao().getById(id)!!.galleryAvailable)
            val before = db.photoDao().observeCount().first()
            // Delete only the URI created by this test, never any query result or user asset.
            assertEquals(1, context.contentResolver.delete(uri, null, null))
            uri = null
            repository.syncGallery()
            assertEquals(before - 1, db.photoDao().observeCount().first())
            assertFalse(db.photoDao().getById(id)!!.galleryAvailable)
            repository.syncGallery()
            assertEquals(before - 1, db.photoDao().observeCount().first())
        } finally {
            uri?.let { context.contentResolver.delete(it, null, null) }
            db.close()
        }
    }

    private fun sample(): Bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 52f }
        canvas.drawText("SYNTHETIC TEST DOCUMENT", 50f, 100f, paint)
        canvas.drawText("Email: test@example.com", 50f, 230f, paint)
        canvas.drawText("Card: 4111 1111 1111 1111", 50f, 360f, paint)
        canvas.drawText("For offline testing only", 50f, 490f, paint)
    }
}
