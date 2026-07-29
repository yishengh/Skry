package com.yishenghuang.skry.domain

/**
 * A single privacy finding for a photo. Bounding boxes are normalized 0..1
 * relative to the source image for UI frosting overlays.
 */
data class Finding(
    val type: FindingType,
    val label: String,
    val confidence: Float,
    val snippet: String? = null,
    val boxLeft: Float? = null,
    val boxTop: Float? = null,
    val boxRight: Float? = null,
    val boxBottom: Float? = null
)

enum class FindingType {
    PASSPORT,
    IDENTITY_CARD,
    CREDIT_CARD,
    SSN_LIKE,
    SENSITIVE_SCREENSHOT,
    LOCATION_EXIF,
    POSSIBLE_ID_PHOTO,
    PHONE_NUMBER,
    EMAIL_ADDRESS,
    PHYSICAL_ADDRESS,
    IBAN,
    IP_ADDRESS,
    DATE_OF_BIRTH,
    SECRET_TOKEN
}
