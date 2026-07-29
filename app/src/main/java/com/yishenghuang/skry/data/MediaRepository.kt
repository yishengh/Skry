package com.yishenghuang.skry.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.yishenghuang.skry.domain.FindingType
import com.yishenghuang.skry.domain.FindingsJson
import com.yishenghuang.skry.domain.PrivacyScanner
import com.yishenghuang.skry.domain.QualityAnalyzer
import com.yishenghuang.skry.domain.VaultService
import com.yishenghuang.skry.util.MediaAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class GalleryScanResult(
    val inserted: Int,
    val totalKnown: Int
)

data class PrivacyScanProgress(
    val processed: Int,
    val risksFound: Int,
    val remaining: Int,
    val finished: Boolean,
    val ocrCharsSeen: Int = 0
)

data class VaultMoveOutcome(
    val photoId: String,
    val originalUri: Uri,
    val fileName: String,
    val success: Boolean,
    val message: String? = null
)

class MediaRepository(
    private val context: Context,
    private val photoDao: PhotoDao = SkryDatabase.get(context).photoDao(),
    private val vaultService: VaultService = VaultService(context)
) {
    fun observeCount(): Flow<Int> = photoDao.observeCount()
    fun observeRiskCount(): Flow<Int> = photoDao.observeRiskCount()
    fun observeDuplicateCandidateCount(): Flow<Int> = photoDao.observeDuplicateCandidateCount()
    fun observeBlurryCount(): Flow<Int> = photoDao.observeBlurryCount()
    fun observeExpiredScreenshotCount(): Flow<Int> = photoDao.observeExpiredScreenshotCount()
    fun observeRiskPhotos(): Flow<List<PhotoEntity>> = photoDao.observeRiskPhotos()
    fun observeClearedRiskPhotos(): Flow<List<PhotoEntity>> = photoDao.observeClearedRiskPhotos()
    fun observeConfirmedRiskPhotos(): Flow<List<PhotoEntity>> = photoDao.observeConfirmedRiskPhotos()
    fun observeAllFindingPhotos(): Flow<List<PhotoEntity>> = photoDao.observeAllFindingPhotos()
    fun observeBlurryPhotos(): Flow<List<PhotoEntity>> = photoDao.observeBlurryPhotos()
    fun observeSuggestedDeletes(): Flow<List<PhotoEntity>> = photoDao.observeSuggestedDeletes()
    fun observeExpiredScreenshots(): Flow<List<PhotoEntity>> = photoDao.observeExpiredScreenshots()
    fun observeLongScreenshots(): Flow<List<PhotoEntity>> = photoDao.observeLongScreenshots()
    fun observeVaultPhotos(): Flow<List<PhotoEntity>> = photoDao.observeVaultPhotos()
    fun observeVaultCount(): Flow<Int> = photoDao.observeVaultCount()
    fun observePendingCount(): Flow<Int> = photoDao.observeCountByStatus(ScanStatus.PENDING)
    fun observeAuditedCount(): Flow<Int> = photoDao.observeAuditedCount()

    suspend fun pendingCount(): Int = withContext(Dispatchers.IO) {
        photoDao.countByStatus(ScanStatus.PENDING)
    }

    suspend fun requeueMissingQuality() = withContext(Dispatchers.IO) {
        photoDao.requeueDoneWithoutQuality()
    }

    fun observePhoto(id: String): Flow<PhotoEntity?> = photoDao.observeById(id)

    suspend fun setUserReview(id: String, review: UserReviewStatus) = withContext(Dispatchers.IO) {
        photoDao.updateUserReview(id, review)
    }

    suspend fun getPhoto(id: String): PhotoEntity? = withContext(Dispatchers.IO) {
        photoDao.getById(id)
    }

    suspend fun moveToVault(photoId: String): VaultMoveOutcome = withContext(Dispatchers.IO) {
        val photo = photoDao.getById(photoId)
            ?: return@withContext VaultMoveOutcome(
                photoId = photoId,
                originalUri = Uri.EMPTY,
                fileName = "",
                success = false,
                message = "Photo not found"
            )
        if (!photo.vaultFileName.isNullOrBlank()) {
            return@withContext VaultMoveOutcome(
                photoId = photoId,
                originalUri = Uri.parse(photo.uri),
                fileName = photo.vaultFileName,
                success = true,
                message = "Already in vault"
            )
        }
        runCatching {
            val findings = FindingsJson.decode(photo.findingsJson)
            val stored = vaultService.storeRedactedCopy(
                photoId = photo.id,
                sourceUri = Uri.parse(photo.uri),
                findings = findings
            )
            photoDao.updateVault(
                id = photo.id,
                status = VaultStatus.REDACTED,
                fileName = stored.fileName,
                vaultedAt = System.currentTimeMillis()
            )
            photoDao.updateUserReview(photo.id, UserReviewStatus.CONFIRMED_LEAK)
            VaultMoveOutcome(
                photoId = photo.id,
                originalUri = Uri.parse(photo.uri),
                fileName = stored.fileName,
                success = true
            )
        }.getOrElse { error ->
            VaultMoveOutcome(
                photoId = photoId,
                originalUri = Uri.parse(photo.uri),
                fileName = "",
                success = false,
                message = error.message ?: "Vault write failed"
            )
        }
    }

    suspend fun readVaultBytes(fileName: String): ByteArray =
        vaultService.openDecryptedBytes(fileName)

    suspend fun deleteFromVault(photoId: String): Boolean = withContext(Dispatchers.IO) {
        val photo = photoDao.getById(photoId) ?: return@withContext false
        val fileName = photo.vaultFileName
        if (!fileName.isNullOrBlank()) {
            vaultService.deleteVaultFile(fileName)
        }
        photoDao.updateVault(
            id = photoId,
            status = VaultStatus.NONE,
            fileName = null,
            vaultedAt = null
        )
        true
    }

    suspend fun removeDeletedFromCleaner(photoIds: List<String>) = withContext(Dispatchers.IO) {
        if (photoIds.isEmpty()) return@withContext
        photoDao.deleteNonVaultedByIds(photoIds)
        photoDao.clearCleanerFlagsForVaulted(photoIds)
    }

    suspend fun syncGallery(): GalleryScanResult = withContext(Dispatchers.IO) {
        val existingIds = photoDao.getAllIds().toHashSet()
        val discovered = queryMediaStore()
        val fresh = discovered.filterNot { existingIds.contains(it.id) }
        if (fresh.isNotEmpty()) {
            photoDao.insertAll(fresh)
        }
        GalleryScanResult(
            inserted = fresh.size,
            totalKnown = existingIds.size + fresh.size
        )
    }

    /**
     * Privacy OCR + quality metrics for PENDING photos only.
     */
    suspend fun runPrivacyScan(
        maxPhotos: Int = 100,
        batchSize: Int = 20,
        forceRescan: Boolean = false,
        shouldAbort: () -> Boolean = { false },
        onProgress: (PrivacyScanProgress) -> Unit = {}
    ): PrivacyScanProgress = withContext(Dispatchers.IO) {
        if (forceRescan) {
            photoDao.setAllScanStatus(ScanStatus.PENDING)
        }

        val scanner = PrivacyScanner(context)
        var processed = 0
        var risksFound = 0
        var ocrCharsSeen = 0
        try {
            while (processed < maxPhotos) {
                if (shouldAbort()) break
                val remainingQuota = maxPhotos - processed
                val batch = photoDao.getByStatus(
                    status = ScanStatus.PENDING,
                    limit = minOf(batchSize, remainingQuota)
                )
                if (batch.isEmpty()) break

                batch.forEach { photo ->
                    if (shouldAbort()) return@forEach
                    val uri = Uri.parse(photo.uri)
                    val result = runCatching {
                        scanner.scan(
                            uri = uri,
                            isScreenshot = photo.isScreenshot,
                            intrinsicWidth = photo.width,
                            intrinsicHeight = photo.height,
                            dateAddedSeconds = photo.dateAdded
                        )
                    }
                    if (result.isSuccess) {
                        val outcome = result.getOrThrow()
                        ocrCharsSeen += outcome.ocrTextLength
                        val quality = outcome.quality
                        photoDao.updateScanResult(
                            id = photo.id,
                            findingsJson = FindingsJson.encode(outcome.findings),
                            scanStatus = ScanStatus.DONE,
                            exifHasGps = outcome.findings.any { it.type == FindingType.LOCATION_EXIF },
                            latitude = outcome.latitude,
                            longitude = outcome.longitude,
                            pHash = quality?.pHash,
                            isBlurry = quality?.isBlurry == true,
                            isLowQuality = quality?.isLowQuality == true,
                            isOverExposed = quality?.isOverExposed == true,
                            isUnderExposed = quality?.isUnderExposed == true,
                            qualityScore = quality?.qualityScore ?: 0f,
                            isLongScreenshot = quality?.isLongScreenshot == true,
                            isExpiredScreenshot = quality?.isExpiredScreenshot == true
                        )
                        if (outcome.findings.isNotEmpty()) risksFound += 1
                    } else {
                        photoDao.updateScanResult(
                            id = photo.id,
                            findingsJson = "[]",
                            scanStatus = ScanStatus.ERROR,
                            exifHasGps = false,
                            latitude = null,
                            longitude = null,
                            pHash = null,
                            isBlurry = false,
                            isLowQuality = false,
                            isOverExposed = false,
                            isUnderExposed = false,
                            qualityScore = 0f,
                            isLongScreenshot = false,
                            isExpiredScreenshot = false
                        )
                    }
                    processed += 1
                }

                regroupDuplicates()

                val remaining = photoDao.countByStatus(ScanStatus.PENDING)
                onProgress(
                    PrivacyScanProgress(
                        processed = processed,
                        risksFound = risksFound,
                        remaining = remaining,
                        finished = false,
                        ocrCharsSeen = ocrCharsSeen
                    )
                )
                if (shouldAbort()) break
            }
        } finally {
            scanner.close()
        }

        regroupDuplicates()
        val remaining = photoDao.countByStatus(ScanStatus.PENDING)
        PrivacyScanProgress(
            processed = processed,
            risksFound = risksFound,
            remaining = remaining,
            finished = true,
            ocrCharsSeen = ocrCharsSeen
        ).also(onProgress)
    }

    /**
     * Cluster near-duplicate pHashes; keep highest qualityScore as starred pick,
     * mark the rest suggestedDelete.
     */
    suspend fun regroupDuplicates(hammingThreshold: Int = 8) = withContext(Dispatchers.IO) {
        photoDao.clearDuplicateFlags()
        val hashed = photoDao.getHashedPhotos().filter { !it.pHash.isNullOrBlank() }
        if (hashed.size < 2) return@withContext

        val assigned = hashSetOf<String>()
        hashed.forEach { seed ->
            if (seed.id in assigned) return@forEach
            val seedHash = seed.pHash ?: return@forEach
            val group = mutableListOf(seed)
            hashed.forEach { other ->
                if (other.id == seed.id || other.id in assigned) return@forEach
                val otherHash = other.pHash ?: return@forEach
                if (QualityAnalyzer.hammingDistance(seedHash, otherHash) <= hammingThreshold) {
                    group += other
                }
            }
            if (group.size < 2) return@forEach
            group.forEach { assigned += it.id }
            val best = group.maxBy { it.qualityScore }
            group.forEach { photo ->
                photoDao.updateDuplicateFlags(
                    id = photo.id,
                    suggested = photo.id != best.id,
                    starred = photo.id == best.id
                )
            }
        }
    }

    suspend fun findRelatedDuplicates(
        photoId: String,
        hammingThreshold: Int = 8
    ): List<PhotoEntity> = withContext(Dispatchers.IO) {
        val seed = photoDao.getById(photoId) ?: return@withContext emptyList()
        val seedHash = seed.pHash ?: return@withContext emptyList()
        photoDao.getHashedPhotos()
            .filter { it.id != seed.id && !it.pHash.isNullOrBlank() }
            .mapNotNull { other ->
                val distance = QualityAnalyzer.hammingDistance(seedHash, other.pHash!!)
                if (distance <= hammingThreshold) other to distance else null
            }
            .sortedBy { it.second }
            .map { it.first }
    }

    private fun queryMediaStore(): List<PhotoEntity> {
        val collection = MediaAccess.imagesCollectionUri()
        val useRelativePath = Build.VERSION.SDK_INT >= 29
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.DATE_ADDED)
            add(MediaStore.Images.Media.SIZE)
            add(MediaStore.Images.Media.WIDTH)
            add(MediaStore.Images.Media.HEIGHT)
            add(MediaStore.Images.Media.MIME_TYPE)
            if (useRelativePath) {
                add(MediaStore.Images.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Images.Media.DATA)
            }
        }.toTypedArray()
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val items = mutableListOf<PhotoEntity>()

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val pathCol = if (useRelativePath) {
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri: Uri = MediaAccess.contentUriForId(id)
                val displayName = cursor.getString(nameCol).orEmpty()
                val pathHint = cursor.getString(pathCol).orEmpty()
                val isScreenshot = displayName.contains("Screenshot", ignoreCase = true) ||
                    pathHint.contains("Screenshot", ignoreCase = true)

                items += PhotoEntity(
                    id = id.toString(),
                    uri = uri.toString(),
                    displayName = displayName,
                    dateAdded = cursor.getLong(dateCol),
                    size = cursor.getLong(sizeCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    mimeType = cursor.getString(mimeCol),
                    isScreenshot = isScreenshot
                )
            }
        }
        return items
    }
}
