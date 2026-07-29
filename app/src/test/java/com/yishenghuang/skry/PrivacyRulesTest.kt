package com.yishenghuang.skry

import com.yishenghuang.skry.domain.FindingType
import com.yishenghuang.skry.domain.Luhn
import com.yishenghuang.skry.domain.PrivacyRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyRulesTest {

    @Test
    fun luhn_acceptsValidVisaTestNumber() {
        assertTrue(Luhn.isValid("4111111111111111"))
    }

    @Test
    fun luhn_rejectsInvalidNumber() {
        assertFalse(Luhn.isValid("4111111111111112"))
    }

    @Test
    fun rules_detectPassportMrz() {
        val text = "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<\nL898902C36UTO7408122F1204159ZE184226B<<<<<10"
        val findings = PrivacyRules.analyze(
            fullText = text,
            blocks = emptyList(),
            imageWidth = 1000,
            imageHeight = 1000,
            isScreenshot = false
        )
        assertTrue(findings.any { it.type == FindingType.PASSPORT })
    }

    @Test
    fun rules_detectCreditCardWithLuhn() {
        val findings = PrivacyRules.analyze(
            fullText = "Card 4111 1111 1111 1111 exp 12/30",
            blocks = emptyList(),
            imageWidth = 1000,
            imageHeight = 1000,
            isScreenshot = false
        )
        assertTrue(findings.any { it.type == FindingType.CREDIT_CARD })
    }

    @Test
    fun rules_detectCardLikeWithoutLuhn() {
        // Random 16-digit starting with 4 — common test input that fails Luhn
        val findings = PrivacyRules.analyze(
            fullText = "4111 2222 3333 4444",
            blocks = emptyList(),
            imageWidth = 1000,
            imageHeight = 1000,
            isScreenshot = false
        )
        assertTrue(findings.any { it.type == FindingType.CREDIT_CARD })
    }

    @Test
    fun rules_detectCardAcrossNewlines() {
        val findings = PrivacyRules.analyze(
            fullText = "4111\n1111\n1111\n1111",
            blocks = emptyList(),
            imageWidth = 1000,
            imageHeight = 1000,
            isScreenshot = false
        )
        assertTrue(findings.any { it.type == FindingType.CREDIT_CARD })
    }

    @Test
    fun rules_detectOtpScreenshot() {
        val findings = PrivacyRules.analyze(
            fullText = "Your verification code is 482913",
            blocks = emptyList(),
            imageWidth = 800,
            imageHeight = 1600,
            isScreenshot = true
        )
        assertTrue(findings.any { it.type == FindingType.SENSITIVE_SCREENSHOT })
    }

    @Test
    fun detectCardNumbers_returnsFinding() {
        val finding = PrivacyRules.detectCardNumbers(
            normalized = "visa 5555555555554444",
            blocks = emptyList(),
            imageWidth = 100,
            imageHeight = 100
        )
        assertNotNull(finding)
    }

    @Test
    fun rules_detectPhoneNumber() {
        val findings = PrivacyRules.analyze(
            fullText = "Call me at +1 (415) 555-2671 tonight",
            blocks = emptyList(),
            imageWidth = 800,
            imageHeight = 600,
            isScreenshot = false
        )
        assertTrue(findings.any { it.type == FindingType.PHONE_NUMBER })
    }

    @Test
    fun rules_detectEmail() {
        val findings = PrivacyRules.analyze(
            fullText = "Contact alice.j@example.com for details",
            blocks = emptyList(),
            imageWidth = 800,
            imageHeight = 600,
            isScreenshot = true
        )
        assertTrue(findings.any { it.type == FindingType.EMAIL_ADDRESS })
    }

    @Test
    fun rules_detectPhysicalAddress() {
        val findings = PrivacyRules.analyze(
            fullText = "Ship to 1600 Amphitheatre Parkway, Mountain View, CA 94043",
            blocks = emptyList(),
            imageWidth = 1000,
            imageHeight = 800,
            isScreenshot = false
        )
        assertTrue(findings.any { it.type == FindingType.PHYSICAL_ADDRESS })
    }

    @Test
    fun rules_detectIban() {
        val findings = PrivacyRules.analyze(
            fullText = "IBAN GB82 WEST 1234 5698 7654 32",
            blocks = emptyList(),
            imageWidth = 800,
            imageHeight = 600,
            isScreenshot = true
        )
        assertTrue(findings.any { it.type == FindingType.IBAN })
    }

    @Test
    fun rules_detectDob() {
        val findings = PrivacyRules.analyze(
            fullText = "Date of birth: 12/05/1990",
            blocks = emptyList(),
            imageWidth = 800,
            imageHeight = 600,
            isScreenshot = false
        )
        assertTrue(findings.any { it.type == FindingType.DATE_OF_BIRTH })
    }

    @Test
    fun rules_detectSecretToken() {
        val findings = PrivacyRules.analyze(
            fullText = "Authorization: Bearer sk_live_51FakeTokenExample999",
            blocks = emptyList(),
            imageWidth = 800,
            imageHeight = 1200,
            isScreenshot = true
        )
        assertTrue(findings.any { it.type == FindingType.SECRET_TOKEN })
    }
}
