package com.yishenghuang.skry.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val displayName: String?,
    val dateAdded: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String?,
    val isScreenshot: Boolean = false,
    val pHash: String? = null,
    val scanStatus: ScanStatus = ScanStatus.PENDING,
    val findingsJson: String = "[]",
    val isBlurry: Boolean = false,
    val isLowQuality: Boolean = false,
    val isOverExposed: Boolean = false,
    val isUnderExposed: Boolean = false,
    val qualityScore: Float = 0f,
    val exifHasGps: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val vaultStatus: VaultStatus = VaultStatus.NONE,
    val vaultFileName: String? = null,
    val vaultedAt: Long? = null,
    val suggestedDelete: Boolean = false,
    val isStarredPick: Boolean = false,
    val userReview: UserReviewStatus = UserReviewStatus.NONE,
    val isLongScreenshot: Boolean = false,
    val isExpiredScreenshot: Boolean = false
)
