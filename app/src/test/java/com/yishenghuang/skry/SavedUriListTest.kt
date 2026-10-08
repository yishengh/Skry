package com.yishenghuang.skry

import com.yishenghuang.skry.util.SavedUriList
import org.junit.Assert.*
import org.junit.Test

class SavedUriListTest {
    @Test fun tenThousandSelectionsSurviveStateRoundTripWithinBinderBudget() {
        val uris = (1..10_000).map { "content://media/external/images/media/$it" }
        val saved = SavedUriList.encode(uris)
        assertTrue("Leave room for the rest of the activity state", saved.size < 100_000)
        assertEquals(uris, SavedUriList.decode(saved))
    }

    @Test fun corruptStateCannotBecomeADeletionRequest() {
        assertTrue(SavedUriList.decode(byteArrayOf(1, 2, 3)).isEmpty())
        assertTrue(SavedUriList.decode(null).isEmpty())
    }
}
