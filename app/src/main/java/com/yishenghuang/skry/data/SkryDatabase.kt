package com.yishenghuang.skry.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class SkryConverters {
    @TypeConverter
    fun scanStatusToString(value: ScanStatus): String = value.name

    @TypeConverter
    fun stringToScanStatus(value: String): ScanStatus = ScanStatus.valueOf(value)

    @TypeConverter
    fun vaultStatusToString(value: VaultStatus): String = value.name

    @TypeConverter
    fun stringToVaultStatus(value: String): VaultStatus = VaultStatus.valueOf(value)

    @TypeConverter
    fun userReviewToString(value: UserReviewStatus): String = value.name

    @TypeConverter
    fun stringToUserReview(value: String): UserReviewStatus = UserReviewStatus.valueOf(value)
}

@Database(
    entities = [PhotoEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(SkryConverters::class)
abstract class SkryDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE photos ADD COLUMN userReview TEXT NOT NULL DEFAULT 'NONE'"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE photos ADD COLUMN isLongScreenshot INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE photos ADD COLUMN isExpiredScreenshot INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE photos ADD COLUMN vaultFileName TEXT")
                db.execSQL("ALTER TABLE photos ADD COLUMN vaultedAt INTEGER")
            }
        }

        @Volatile
        private var instance: SkryDatabase? = null

        fun get(context: Context): SkryDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SkryDatabase::class.java,
                    "skry.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
