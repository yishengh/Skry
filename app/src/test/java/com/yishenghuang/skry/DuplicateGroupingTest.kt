package com.yishenghuang.skry

import com.yishenghuang.skry.domain.DuplicateGrouping
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class DuplicateGroupingTest {
    @Test fun indexedGroupingMatchesExhaustiveSearchIncludingBoundaryAndMalformedHashes() {
        val random = Random(1234)
        val input = (0 until 500).flatMap { index ->
            val hash = random.nextLong().toULong()
            listOf(
                DuplicateGrouping.Photo("$index-a", hash.toString(16).padStart(16, '0'), 100f),
                DuplicateGrouping.Photo("$index-b", (hash xor 255uL).toString(16).padStart(16, '0'), 10f)
            )
        }
        val assigned = hashSetOf<String>()
        val expected = mutableListOf<DuplicateGrouping.Pick>()
        for (seed in input) {
            if (seed.id in assigned) continue
            val members = input.filter { it.id !in assigned &&
                (seed.hash.toULong(16) xor it.hash.toULong(16)).countOneBits() <= 8 }
            if (members.size < 2) continue
            val best = members.maxBy { it.quality }
            members.forEach {
                assigned += it.id
                expected += DuplicateGrouping.Pick(it.id, it.id != best.id, it.id == best.id)
            }
        }
        assertEquals(expected.toSet(), DuplicateGrouping.group(input + DuplicateGrouping.Photo("bad", "invalid", 0f)).toSet())
    }

    @Test fun largeLibraryKeepsOneBestPhotoPerIdenticalCluster() {
        val input = (0 until 10_000).map {
            DuplicateGrouping.Photo(it.toString(), "abcdef0123456789", it.toFloat())
        }
        val result = DuplicateGrouping.group(input)
        assertEquals(9999, result.count { it.suggested })
        assertEquals("9999", result.single { it.starred }.id)
        assertTrue(DuplicateGrouping.group(emptyList()).isEmpty())
        assertTrue(DuplicateGrouping.group(input.take(1)).isEmpty())
    }
}
