package com.yishenghuang.skry.domain

import com.google.mlkit.vision.text.Text
import kotlin.math.max

data class OcrBlock(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

object PrivacyRules {
    private val mrzLine = Regex("""^[A-Z0-9<]{30,44}$""")
    private val cardLikeRun = Regex("""(?<!\d)(?:\d[ \-]*){13,19}(?!\d)""")
    private val ssnLike = Regex("""\b\d{3}-\d{2}-\d{4}\b""")
    private val otpNear = Regex(
        """(?i)(otp|verification code|one[-\s]?time(?:\s+pass(?:word|code))?|security code)"""
    )
    private val shortCode = Regex("""\b\d{4,8}\b""")

    /** US / intl phone shapes; digit count validated after strip. */
    private val phonePattern = Regex(
        """(?<![\w@])(?:\+?\d{1,3}[\s\-.]*)?(?:\(?\d{2,4}\)?[\s\-.]*)?\d{3,4}[\s\-.]*\d{3,4}(?!\d)"""
    )
    private val emailPattern = Regex(
        """(?i)\b[a-z0-9._%+\-]+@[a-z0-9.\-]+\.[a-z]{2,}\b"""
    )
    private val addressPattern = Regex(
        """(?i)\b\d{1,5}\s+[a-z0-9.'\-]+\s+(?:street|st\.?|avenue|ave\.?|road|rd\.?|boulevard|blvd\.?|lane|ln\.?|drive|dr\.?|court|ct\.?|way|place|pl\.?|circle|cir\.?|parkway|pkwy\.?|terrace|ter\.?|highway|hwy\.?)\b(?:[,\s]+[a-z.\- ]{2,40})?(?:[,\s]+\d{5}(?:-\d{4})?)?"""
    )
    private val zipOnlyWithCity = Regex(
        """(?i)\b[A-Z][a-z]+(?:\s+[A-Z][a-z]+)?,?\s+[A-Z]{2}\s+\d{5}(?:-\d{4})?\b"""
    )
    private val ibanPattern = Regex(
        """(?<![A-Z0-9])[A-Z]{2}\d{2}(?:[\s]?[A-Z0-9]{4}){2,7}(?![A-Z0-9])"""
    )
    private val ipv4Pattern = Regex(
        """\b(?:(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\b"""
    )
    private val dobKeyword = Regex(
        """(?i)\b(?:d\.?o\.?b\.?|date of birth|birthday|born on|birth date)\b"""
    )
    private val datePattern = Regex(
        """\b(?:\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4}|\d{4}[/\-.]\d{1,2}[/\-.]\d{1,2}|(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\s+\d{1,2},?\s+\d{2,4})\b""",
        RegexOption.IGNORE_CASE
    )
    private val secretPattern = Regex(
        """(?i)\b(?:api[_-]?key|secret[_-]?key|access[_-]?token|bearer\s+[a-z0-9\-._~+/]+=*|sk_live_[a-z0-9]+|AKIA[0-9A-Z]{16})\b"""
    )

    private val sensitiveKeywords = listOf(
        "password", "passwd", "account", "verification code", "otp", "login"
    )
    private val passportKeywords = listOf("passport", "passeport")
    private val idKeywords = listOf(
        "identity card", "national id", "driver license", "driver's license",
        "driving licence", "identification card", "id card"
    )
    private val cardKeywords = listOf(
        "visa", "mastercard", "american express", "amex", "credit card",
        "debit card", "card number", "cvv", "cvc", "valid thru", "good thru"
    )
    private val addressKeywords = listOf(
        "home address", "mailing address", "shipping address", "billing address",
        "residential address"
    )

    fun analyze(
        fullText: String,
        blocks: List<OcrBlock>,
        imageWidth: Int,
        imageHeight: Int,
        isScreenshot: Boolean
    ): List<Finding> {
        val findings = linkedMapOf<FindingType, Finding>()
        val normalized = fullText.replace('\n', ' ').replace('\u00A0', ' ')
        val upperLines = fullText.lines().map { it.trim().uppercase().replace(" ", "") }

        upperLines.forEach { line ->
            if (mrzLine.matches(line)) {
                findings[FindingType.PASSPORT] = Finding(
                    type = FindingType.PASSPORT,
                    label = "Passport",
                    confidence = 0.92f,
                    snippet = "MRZ line detected"
                )
            }
        }

        val lower = normalized.lowercase()
        when {
            passportKeywords.any { lower.contains(it) } -> {
                findings.putIfAbsent(
                    FindingType.PASSPORT,
                    Finding(
                        type = FindingType.PASSPORT,
                        label = "Passport",
                        confidence = 0.75f,
                        snippet = "Passport keyword matched"
                    )
                )
            }
            idKeywords.any { lower.contains(it) } -> {
                findings[FindingType.IDENTITY_CARD] = Finding(
                    type = FindingType.IDENTITY_CARD,
                    label = "Identity Card",
                    confidence = 0.78f,
                    snippet = "Document keywords matched"
                )
            }
        }

        detectCardNumbers(normalized, blocks, imageWidth, imageHeight, lower)?.let { card ->
            findings[FindingType.CREDIT_CARD] = card
        }

        ssnLike.findAll(normalized).forEach { match ->
            // Avoid treating US phones like 123-45-6789 only when SSN shape; phones handled separately
            findings[FindingType.SSN_LIKE] = Finding(
                type = FindingType.SSN_LIKE,
                label = "SSN-like",
                confidence = 0.8f,
                snippet = "Pattern ${match.value.take(3)}-••-••••"
            )
        }

        detectPhone(normalized, blocks, imageWidth, imageHeight)?.let {
            findings[FindingType.PHONE_NUMBER] = it
        }
        detectEmail(normalized)?.let { findings[FindingType.EMAIL_ADDRESS] = it }
        detectAddress(normalized, lower)?.let { findings[FindingType.PHYSICAL_ADDRESS] = it }
        detectIban(normalized)?.let { findings[FindingType.IBAN] = it }
        detectIp(normalized)?.let { findings[FindingType.IP_ADDRESS] = it }
        detectDob(normalized)?.let { findings[FindingType.DATE_OF_BIRTH] = it }
        detectSecret(normalized)?.let { findings[FindingType.SECRET_TOKEN] = it }

        val hasSensitiveKeyword = sensitiveKeywords.any { lower.contains(it) }
        val hasOtpContext = otpNear.containsMatchIn(normalized)
        val hasCode = shortCode.containsMatchIn(normalized)
        if ((isScreenshot || hasOtpContext) && (hasSensitiveKeyword || hasOtpContext) && hasCode) {
            findings[FindingType.SENSITIVE_SCREENSHOT] = Finding(
                type = FindingType.SENSITIVE_SCREENSHOT,
                label = "OTP Screenshot",
                confidence = 0.7f,
                snippet = "Verification / account code pattern"
            )
        } else if (isScreenshot && hasSensitiveKeyword) {
            findings[FindingType.SENSITIVE_SCREENSHOT] = Finding(
                type = FindingType.SENSITIVE_SCREENSHOT,
                label = "Sensitive Screenshot",
                confidence = 0.62f,
                snippet = "Password / account keywords found"
            )
        }

        if (
            !findings.containsKey(FindingType.PASSPORT) &&
            !findings.containsKey(FindingType.IDENTITY_CARD) &&
            idKeywords.any { lower.contains(it.substringBefore(' ')) }
        ) {
            findings[FindingType.POSSIBLE_ID_PHOTO] = Finding(
                type = FindingType.POSSIBLE_ID_PHOTO,
                label = "Possible ID Photo",
                confidence = 0.45f,
                snippet = "Low-confidence document heuristic"
            )
        }

        return findings.values.toList()
    }

    internal fun detectPhone(
        normalized: String,
        blocks: List<OcrBlock>,
        imageWidth: Int,
        imageHeight: Int
    ): Finding? {
        var best: Finding? = null
        phonePattern.findAll(normalized).forEach { match ->
            val digits = match.value.filter { it.isDigit() }
            // Skip card-length runs; require typical phone length
            if (digits.length !in 7..15 || digits.length in 13..19) return@forEach
            if (digits.length == 7 && !match.value.contains(Regex("""[()\-+]"""))) return@forEach

            val confidence = when {
                match.value.contains('+') || digits.length >= 11 -> 0.82f
                digits.length in 10..11 -> 0.78f
                else -> 0.62f
            }
            val box = boxForText(match.value, blocks, imageWidth, imageHeight)
            val candidate = Finding(
                type = FindingType.PHONE_NUMBER,
                label = "Phone Number",
                confidence = confidence,
                snippet = "Phone-like ${maskPhone(digits)}",
                boxLeft = box?.get(0),
                boxTop = box?.get(1),
                boxRight = box?.get(2),
                boxBottom = box?.get(3)
            )
            if (best == null || candidate.confidence > best!!.confidence) best = candidate
        }
        return best
    }

    internal fun detectEmail(normalized: String): Finding? {
        val match = emailPattern.find(normalized) ?: return null
        val local = match.value.substringBefore('@')
        val domain = match.value.substringAfter('@')
        return Finding(
            type = FindingType.EMAIL_ADDRESS,
            label = "Email Address",
            confidence = 0.9f,
            snippet = "${local.take(2)}•••@$domain"
        )
    }

    internal fun detectAddress(normalized: String, lower: String = normalized.lowercase()): Finding? {
        addressPattern.find(normalized)?.let { match ->
            return Finding(
                type = FindingType.PHYSICAL_ADDRESS,
                label = "Physical Address",
                confidence = 0.8f,
                snippet = truncate(match.value, 48)
            )
        }
        zipOnlyWithCity.find(normalized)?.let { match ->
            return Finding(
                type = FindingType.PHYSICAL_ADDRESS,
                label = "Physical Address",
                confidence = 0.72f,
                snippet = truncate(match.value, 48)
            )
        }
        if (addressKeywords.any { lower.contains(it) }) {
            return Finding(
                type = FindingType.PHYSICAL_ADDRESS,
                label = "Physical Address",
                confidence = 0.55f,
                snippet = "Address keyword context"
            )
        }
        return null
    }

    internal fun detectIban(normalized: String): Finding? {
        val match = ibanPattern.find(normalized.uppercase()) ?: return null
        val value = match.value.replace(" ", "")
        if (value.length !in 15..34) return null
        return Finding(
            type = FindingType.IBAN,
            label = "IBAN",
            confidence = 0.85f,
            snippet = "${value.take(4)}••••${value.takeLast(4)}"
        )
    }

    internal fun detectIp(normalized: String): Finding? {
        val match = ipv4Pattern.find(normalized) ?: return null
        // Skip obvious non-routable noise like 0.0.0.0 / version-looking if desired; keep private IPs as sensitive too
        return Finding(
            type = FindingType.IP_ADDRESS,
            label = "IP Address",
            confidence = 0.7f,
            snippet = match.value
        )
    }

    internal fun detectDob(normalized: String): Finding? {
        if (!dobKeyword.containsMatchIn(normalized)) return null
        val date = datePattern.find(normalized)?.value
        return Finding(
            type = FindingType.DATE_OF_BIRTH,
            label = "Date of Birth",
            confidence = if (date != null) 0.8f else 0.58f,
            snippet = date?.let { "DOB context · $it" } ?: "DOB keyword detected"
        )
    }

    internal fun detectSecret(normalized: String): Finding? {
        val match = secretPattern.find(normalized) ?: return null
        return Finding(
            type = FindingType.SECRET_TOKEN,
            label = "Secret / API Token",
            confidence = 0.88f,
            snippet = truncate(match.value.replace(Regex("""(?i)bearer\s+"""), "bearer "), 40)
        )
    }

    /**
     * Pull card-like digit runs from OCR text. Luhn-valid → high confidence;
     * 15–16 digit / BIN-like / card-keyword context → still flag (test numbers often fail Luhn).
     */
    internal fun detectCardNumbers(
        normalized: String,
        blocks: List<OcrBlock>,
        imageWidth: Int,
        imageHeight: Int,
        lower: String = normalized.lowercase()
    ): Finding? {
        val candidates = linkedSetOf<String>()
        cardLikeRun.findAll(normalized).forEach { match ->
            val digits = match.value.filter { it.isDigit() }
            if (digits.length in 13..19) candidates += digits
        }
        normalized.split(Regex("""\s+""")).forEach { token ->
            val digits = token.filter { it.isDigit() }
            if (digits.length in 13..19) candidates += digits
        }
        val allDigits = normalized.filter { it.isDigit() }
        if (allDigits.length >= 13) {
            for (len in 16 downTo 13) {
                var i = 0
                while (i + len <= allDigits.length) {
                    candidates += allDigits.substring(i, i + len)
                    i += 1
                }
            }
        }

        val hasCardKeyword = cardKeywords.any { lower.contains(it) }
        var best: Finding? = null

        candidates.forEach { digits ->
            val luhnOk = Luhn.isValid(digits)
            val binLike = digits.first() in setOf('3', '4', '5', '6')
            val looksLikePan = digits.length in 15..16 && binLike
            if (!luhnOk && !looksLikePan && !hasCardKeyword) return@forEach
            if (!luhnOk && hasCardKeyword && digits.length !in 13..19) return@forEach

            val confidence = when {
                luhnOk -> 0.9f
                looksLikePan && hasCardKeyword -> 0.78f
                looksLikePan -> 0.68f
                hasCardKeyword -> 0.6f
                else -> return@forEach
            }
            val box = blocks.firstOrNull { block ->
                val blockDigits = block.text.filter { it.isDigit() }
                blockDigits.contains(digits.take(8)) || digits.contains(blockDigits.take(8))
            }?.toNormalized(imageWidth, imageHeight)

            val candidate = Finding(
                type = FindingType.CREDIT_CARD,
                label = "Credit Card",
                confidence = confidence,
                snippet = if (luhnOk) {
                    "Luhn-valid ${maskPan(digits)} · redact recommended"
                } else {
                    "Card-like ${maskPan(digits)} · review recommended"
                },
                boxLeft = box?.get(0),
                boxTop = box?.get(1),
                boxRight = box?.get(2),
                boxBottom = box?.get(3)
            )
            if (best == null || candidate.confidence > best!!.confidence) {
                best = candidate
            }
        }
        return best
    }

    private fun boxForText(
        needle: String,
        blocks: List<OcrBlock>,
        imageWidth: Int,
        imageHeight: Int
    ): FloatArray? {
        val compact = needle.filter { it.isLetterOrDigit() }
        if (compact.isEmpty()) return null
        return blocks.firstOrNull { block ->
            val blockCompact = block.text.filter { it.isLetterOrDigit() }
            blockCompact.contains(compact.take(8)) || compact.contains(blockCompact.take(6))
        }?.toNormalized(imageWidth, imageHeight)
    }

    private fun maskPan(digits: String): String {
        if (digits.length < 8) return "••••"
        return "${digits.take(4)} •••• •••• ${digits.takeLast(4)}"
    }

    private fun maskPhone(digits: String): String {
        if (digits.length < 4) return "••••"
        return "•••-•••-${digits.takeLast(4)}"
    }

    private fun truncate(value: String, max: Int): String =
        if (value.length <= max) value else value.take(max - 1) + "…"

    fun blocksFromMlKit(visionText: Text, imageWidth: Int, imageHeight: Int): List<OcrBlock> {
        if (imageWidth <= 0 || imageHeight <= 0) return emptyList()
        return visionText.textBlocks.mapNotNull { block ->
            val box = block.boundingBox ?: return@mapNotNull null
            OcrBlock(
                text = block.text,
                left = box.left.toFloat(),
                top = box.top.toFloat(),
                right = box.right.toFloat(),
                bottom = box.bottom.toFloat()
            )
        }
    }

    private fun OcrBlock.toNormalized(width: Int, height: Int): FloatArray {
        val w = max(width, 1).toFloat()
        val h = max(height, 1).toFloat()
        return floatArrayOf(
            (left / w).coerceIn(0f, 1f),
            (top / h).coerceIn(0f, 1f),
            (right / w).coerceIn(0f, 1f),
            (bottom / h).coerceIn(0f, 1f)
        )
    }
}
