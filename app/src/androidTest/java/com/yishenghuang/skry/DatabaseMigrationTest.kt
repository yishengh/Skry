package com.yishenghuang.skry

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yishenghuang.skry.data.SkryDatabase
import com.yishenghuang.skry.data.UserReviewStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @Test
    fun version4VaultAndReviewsSurviveLatestMigration() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "synthetic-migration-${System.nanoTime()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("""CREATE TABLE photos (
                            id TEXT NOT NULL PRIMARY KEY, uri TEXT NOT NULL, displayName TEXT,
                            dateAdded INTEGER NOT NULL, size INTEGER NOT NULL, width INTEGER NOT NULL,
                            height INTEGER NOT NULL, mimeType TEXT, isScreenshot INTEGER NOT NULL,
                            pHash TEXT, scanStatus TEXT NOT NULL, findingsJson TEXT NOT NULL,
                            isBlurry INTEGER NOT NULL, isLowQuality INTEGER NOT NULL,
                            isOverExposed INTEGER NOT NULL, isUnderExposed INTEGER NOT NULL,
                            qualityScore REAL NOT NULL, exifHasGps INTEGER NOT NULL,
                            latitude REAL, longitude REAL, vaultStatus TEXT NOT NULL,
                            vaultFileName TEXT, vaultedAt INTEGER, suggestedDelete INTEGER NOT NULL,
                            isStarredPick INTEGER NOT NULL, userReview TEXT NOT NULL,
                            isLongScreenshot INTEGER NOT NULL, isExpiredScreenshot INTEGER NOT NULL
                        )""")
                        db.execSQL("""INSERT INTO photos VALUES (
                            'test', 'content://synthetic/1', 'synthetic', 0, 1, 1, 1, 'image/jpeg',
                            0, NULL, 'DONE', '[]', 0, 0, 0, 0, 1.0, 0, NULL, NULL, 'REDACTED',
                            'synthetic.enc', 100, 0, 0, 'CONFIRMED_LEAK', 0, 0
                        )""")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build())
        helper.writableDatabase
        helper.close()
        val database = Room.databaseBuilder(context, SkryDatabase::class.java, name)
            .addMigrations(SkryDatabase.MIGRATION_4_5, SkryDatabase.MIGRATION_5_6).build()
        try {
            val photo = database.photoDao().getById("test")!!
            assertTrue(photo.galleryAvailable)
            assertEquals("synthetic.enc", photo.vaultFileName)
            assertEquals(UserReviewStatus.CONFIRMED_LEAK, photo.userReview)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
