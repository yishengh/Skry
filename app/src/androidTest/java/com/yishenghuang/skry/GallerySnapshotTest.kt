package com.yishenghuang.skry

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yishenghuang.skry.data.PhotoEntity
import com.yishenghuang.skry.data.ScanStatus
import com.yishenghuang.skry.data.SkryDatabase
import com.yishenghuang.skry.data.VaultStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Pure synthetic DB fixtures: never reads or deletes the device gallery. */
@RunWith(AndroidJUnit4::class)
class GallerySnapshotTest {
    @Test
    fun changedMediaRequeuesScanButRetainsEncryptedCopy() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SkryDatabase::class.java).build()
        try {
            val old = photo("edit").copy(scanStatus = ScanStatus.DONE, pHash = "abcd",
                dateModified = 1, vaultStatus = VaultStatus.REDACTED, vaultFileName = "saved.enc")
            db.photoDao().insertAll(listOf(old))
            db.photoDao().reconcileGallery(listOf(old.copy(dateModified = 2)))
            val changed = db.photoDao().getById("edit")!!
            assertEquals(ScanStatus.PENDING, changed.scanStatus)
            assertNull(changed.pHash)
            assertEquals("saved.enc", changed.vaultFileName)
        } finally { db.close() }
    }

    @Test
    fun reselectionAndRevocationPreserveVaultAndReviewData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SkryDatabase::class.java).build()
        try {
            val dao = db.photoDao()
            val vaulted = photo("1").copy(vaultStatus = VaultStatus.REDACTED,
                vaultFileName = "synthetic.enc", scanStatus = ScanStatus.DONE,
                findingsJson = "[{\"type\":\"EMAIL\"}]")
            dao.insertAll(listOf(vaulted, photo("2")))
            dao.reconcileGallery(listOf(photo("2")))
            assertEquals(1, dao.observeCount().first())
            assertEquals(0, dao.observeRiskCount().first())
            assertEquals(1, dao.observeVaultCount().first())
            assertFalse(dao.getById("1")!!.galleryAvailable)
            dao.reconcileGallery(listOf(photo("1"), photo("2")))
            assertEquals(ScanStatus.DONE, dao.getById("1")!!.scanStatus)
            assertEquals(1, dao.observeRiskCount().first())
            dao.hideGallery()
            assertEquals(0, dao.observeCount().first())
            assertEquals(1, dao.observeVaultCount().first())
        } finally { db.close() }
    }

    @Test
    fun largeSnapshotsAndEmptyAlbumUseBoundedQueries() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SkryDatabase::class.java).build()
        try {
            val dao = db.photoDao()
            val photos = (1..2200).map { photo(it.toString()) }
            dao.reconcileGallery(photos)
            dao.reconcileGallery(photos)
            assertEquals(2200, dao.observeCount().first())
            dao.reconcileGallery(emptyList())
            assertEquals(0, dao.observeCount().first())
            assertEquals(0, dao.countByStatus(ScanStatus.PENDING))
        } finally { db.close() }
    }

    private fun photo(id: String) = PhotoEntity(id, "content://synthetic/$id", "test-$id",
        0L, 100L, 10, 10, "image/png")
}
