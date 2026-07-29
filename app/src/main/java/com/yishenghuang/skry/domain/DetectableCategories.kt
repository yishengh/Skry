package com.yishenghuang.skry.domain

/**
 * Catalog of privacy categories Skry can detect today (OCR + rules + EXIF).
 */
data class DetectableCategory(
    val type: FindingType,
    val title: String,
    val description: String
)

object DetectableCategories {
    val all: List<DetectableCategory> = listOf(
        DetectableCategory(
            type = FindingType.PASSPORT,
            title = "Passport",
            description = "MRZ lines and passport keywords"
        ),
        DetectableCategory(
            type = FindingType.IDENTITY_CARD,
            title = "Identity Card",
            description = "ID / driver license document keywords"
        ),
        DetectableCategory(
            type = FindingType.CREDIT_CARD,
            title = "Credit Card",
            description = "13–19 digit PANs (Luhn or card-like)"
        ),
        DetectableCategory(
            type = FindingType.PHONE_NUMBER,
            title = "Phone Number",
            description = "US / international phone-like numbers"
        ),
        DetectableCategory(
            type = FindingType.EMAIL_ADDRESS,
            title = "Email Address",
            description = "Standard email patterns"
        ),
        DetectableCategory(
            type = FindingType.PHYSICAL_ADDRESS,
            title = "Physical Address",
            description = "Street address, city/state/ZIP heuristics"
        ),
        DetectableCategory(
            type = FindingType.SSN_LIKE,
            title = "SSN-like",
            description = "Patterns like 123-45-6789"
        ),
        DetectableCategory(
            type = FindingType.IBAN,
            title = "IBAN",
            description = "International bank account numbers"
        ),
        DetectableCategory(
            type = FindingType.DATE_OF_BIRTH,
            title = "Date of Birth",
            description = "DOB keywords near a date"
        ),
        DetectableCategory(
            type = FindingType.IP_ADDRESS,
            title = "IP Address",
            description = "IPv4 addresses in screenshots / docs"
        ),
        DetectableCategory(
            type = FindingType.SECRET_TOKEN,
            title = "Secret / API Token",
            description = "API keys, bearer tokens, sk_live / AKIA"
        ),
        DetectableCategory(
            type = FindingType.SENSITIVE_SCREENSHOT,
            title = "Sensitive Screenshot",
            description = "OTP, password, account, verification codes"
        ),
        DetectableCategory(
            type = FindingType.LOCATION_EXIF,
            title = "Location EXIF",
            description = "Precise GPS still embedded in the file"
        ),
        DetectableCategory(
            type = FindingType.POSSIBLE_ID_PHOTO,
            title = "Possible ID Photo",
            description = "Low-confidence document heuristic"
        )
    )
}
