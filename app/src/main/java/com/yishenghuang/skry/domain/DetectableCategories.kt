package com.yishenghuang.skry.domain

import androidx.annotation.StringRes
import com.yishenghuang.skry.R

/** Localized display labels for [FindingType]. Stored finding JSON may stay English. */
object FindingLabels {
    @StringRes
    fun titleRes(type: FindingType): Int = when (type) {
        FindingType.PASSPORT -> R.string.finding_passport
        FindingType.IDENTITY_CARD -> R.string.finding_identity_card
        FindingType.CREDIT_CARD -> R.string.finding_credit_card
        FindingType.PHONE_NUMBER -> R.string.finding_phone
        FindingType.EMAIL_ADDRESS -> R.string.finding_email
        FindingType.PHYSICAL_ADDRESS -> R.string.finding_address
        FindingType.SSN_LIKE -> R.string.finding_ssn
        FindingType.IBAN -> R.string.finding_iban
        FindingType.DATE_OF_BIRTH -> R.string.finding_dob
        FindingType.IP_ADDRESS -> R.string.finding_ip
        FindingType.SECRET_TOKEN -> R.string.finding_secret
        FindingType.SENSITIVE_SCREENSHOT -> R.string.finding_sensitive_screenshot
        FindingType.LOCATION_EXIF -> R.string.finding_location_exif
        FindingType.POSSIBLE_ID_PHOTO -> R.string.finding_possible_id
    }

    @StringRes
    fun descriptionRes(type: FindingType): Int = when (type) {
        FindingType.PASSPORT -> R.string.finding_desc_passport
        FindingType.IDENTITY_CARD -> R.string.finding_desc_identity_card
        FindingType.CREDIT_CARD -> R.string.finding_desc_credit_card
        FindingType.PHONE_NUMBER -> R.string.finding_desc_phone
        FindingType.EMAIL_ADDRESS -> R.string.finding_desc_email
        FindingType.PHYSICAL_ADDRESS -> R.string.finding_desc_address
        FindingType.SSN_LIKE -> R.string.finding_desc_ssn
        FindingType.IBAN -> R.string.finding_desc_iban
        FindingType.DATE_OF_BIRTH -> R.string.finding_desc_dob
        FindingType.IP_ADDRESS -> R.string.finding_desc_ip
        FindingType.SECRET_TOKEN -> R.string.finding_desc_secret
        FindingType.SENSITIVE_SCREENSHOT -> R.string.finding_desc_sensitive_screenshot
        FindingType.LOCATION_EXIF -> R.string.finding_desc_location_exif
        FindingType.POSSIBLE_ID_PHOTO -> R.string.finding_desc_possible_id
    }
}

/**
 * Catalog of privacy categories Skry can detect today (OCR + rules + EXIF).
 */
data class DetectableCategory(
    val type: FindingType,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)

object DetectableCategories {
    val all: List<DetectableCategory> = FindingType.entries.map { type ->
        DetectableCategory(
            type = type,
            titleRes = FindingLabels.titleRes(type),
            descriptionRes = FindingLabels.descriptionRes(type)
        )
    }
}
