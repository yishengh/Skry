package com.yishenghuang.skry

import com.yishenghuang.skry.domain.QualityAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityAnalyzerTest {

    @Test
    fun hamming_identicalHashes_zero() {
        assertEquals(
            0,
            QualityAnalyzer.hammingDistance("ffffffffffffffff", "ffffffffffffffff")
        )
    }

    @Test
    fun hamming_oppositeHashes_nonzero() {
        val distance = QualityAnalyzer.hammingDistance(
            "0000000000000000",
            "ffffffffffffffff"
        )
        assertEquals(64, distance)
    }

    @Test
    fun hamming_invalidHex_isMax() {
        assertTrue(
            QualityAnalyzer.hammingDistance("zz", "aa") > 60
        )
    }
}
