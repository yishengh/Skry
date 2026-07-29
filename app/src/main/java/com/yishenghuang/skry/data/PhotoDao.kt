package com.yishenghuang.skry.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(photos: List<PhotoEntity>): List<Long>

    @Query("SELECT * FROM photos ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Query("SELECT COUNT(*) FROM photos")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE findingsJson != '[]' AND userReview != 'DISMISSED'")
    fun observeRiskCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE suggestedDelete = 1")
    fun observeDuplicateCandidateCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE isBlurry = 1")
    fun observeBlurryCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE isExpiredScreenshot = 1")
    fun observeExpiredScreenshotCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE scanStatus = :status")
    suspend fun countByStatus(status: ScanStatus): Int

    @Query("SELECT COUNT(*) FROM photos WHERE scanStatus = :status")
    fun observeCountByStatus(status: ScanStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE scanStatus = 'DONE' OR scanStatus = 'ERROR'")
    fun observeAuditedCount(): Flow<Int>

    @Query("UPDATE photos SET scanStatus = :status")
    suspend fun setAllScanStatus(status: ScanStatus)

    @Query("SELECT id FROM photos")
    suspend fun getAllIds(): List<String>

    @Query(
        """
        SELECT * FROM photos
        WHERE findingsJson != '[]' AND userReview != 'DISMISSED'
        ORDER BY dateAdded DESC
        """
    )
    fun observeRiskPhotos(): Flow<List<PhotoEntity>>

    @Query(
        """
        SELECT * FROM photos
        WHERE findingsJson != '[]' AND userReview = 'DISMISSED'
        ORDER BY dateAdded DESC
        """
    )
    fun observeClearedRiskPhotos(): Flow<List<PhotoEntity>>

    @Query(
        """
        SELECT * FROM photos
        WHERE findingsJson != '[]' AND userReview = 'CONFIRMED_LEAK'
        ORDER BY dateAdded DESC
        """
    )
    fun observeConfirmedRiskPhotos(): Flow<List<PhotoEntity>>

    @Query(
        """
        SELECT * FROM photos
        WHERE findingsJson != '[]'
        ORDER BY dateAdded DESC
        """
    )
    fun observeAllFindingPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE isBlurry = 1 ORDER BY dateAdded DESC")
    fun observeBlurryPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE suggestedDelete = 1 ORDER BY dateAdded DESC")
    fun observeSuggestedDeletes(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE isExpiredScreenshot = 1 ORDER BY dateAdded DESC")
    fun observeExpiredScreenshots(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE isLongScreenshot = 1 ORDER BY dateAdded DESC")
    fun observeLongScreenshots(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE pHash IS NOT NULL")
    suspend fun getHashedPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<PhotoEntity?>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PhotoEntity?

    @Query("UPDATE photos SET userReview = :review WHERE id = :id")
    suspend fun updateUserReview(id: String, review: UserReviewStatus)

    @Query(
        """
        SELECT * FROM photos
        WHERE scanStatus = :status
        ORDER BY dateAdded DESC
        LIMIT :limit
        """
    )
    suspend fun getByStatus(status: ScanStatus, limit: Int): List<PhotoEntity>

    @Query(
        """
        UPDATE photos SET
            findingsJson = :findingsJson,
            scanStatus = :scanStatus,
            exifHasGps = :exifHasGps,
            latitude = :latitude,
            longitude = :longitude,
            pHash = :pHash,
            isBlurry = :isBlurry,
            isLowQuality = :isLowQuality,
            isOverExposed = :isOverExposed,
            isUnderExposed = :isUnderExposed,
            qualityScore = :qualityScore,
            isLongScreenshot = :isLongScreenshot,
            isExpiredScreenshot = :isExpiredScreenshot
        WHERE id = :id
        """
    )
    suspend fun updateScanResult(
        id: String,
        findingsJson: String,
        scanStatus: ScanStatus,
        exifHasGps: Boolean,
        latitude: Double?,
        longitude: Double?,
        pHash: String?,
        isBlurry: Boolean,
        isLowQuality: Boolean,
        isOverExposed: Boolean,
        isUnderExposed: Boolean,
        qualityScore: Float,
        isLongScreenshot: Boolean,
        isExpiredScreenshot: Boolean
    )

    @Query("UPDATE photos SET suggestedDelete = :suggested, isStarredPick = :starred WHERE id = :id")
    suspend fun updateDuplicateFlags(id: String, suggested: Boolean, starred: Boolean)

    @Query("UPDATE photos SET suggestedDelete = 0, isStarredPick = 0")
    suspend fun clearDuplicateFlags()

    @Query("UPDATE photos SET scanStatus = 'PENDING' WHERE (pHash IS NULL OR pHash = '') AND scanStatus = 'DONE'")
    suspend fun requeueDoneWithoutQuality()

    @Query(
        """
        SELECT * FROM photos
        WHERE vaultStatus != 'NONE' AND vaultFileName IS NOT NULL
        ORDER BY vaultedAt DESC
        """
    )
    fun observeVaultPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT COUNT(*) FROM photos WHERE vaultStatus != 'NONE' AND vaultFileName IS NOT NULL")
    fun observeVaultCount(): Flow<Int>

    @Query(
        """
        UPDATE photos SET
            vaultStatus = :status,
            vaultFileName = :fileName,
            vaultedAt = :vaultedAt
        WHERE id = :id
        """
    )
    suspend fun updateVault(
        id: String,
        status: VaultStatus,
        fileName: String?,
        vaultedAt: Long?
    )

    @Query("SELECT MAX(dateAdded) FROM photos")
    suspend fun maxDateAdded(): Long?
}
