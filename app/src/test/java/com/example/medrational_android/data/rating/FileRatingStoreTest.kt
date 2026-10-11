package com.example.medrational_android.data.rating

import com.example.medrational_android.data.model.FileRatingSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileRatingStoreTest {

    private val store = FileRatingStore()

    @Test
    fun `put stores the latest summary`() {
        store.put(FileRatingSummary(1, 3.0, 2, userRating = 4))
        store.put(FileRatingSummary(1, 3.5, 2, userRating = 5))

        assertEquals(FileRatingSummary(1, 3.5, 2, userRating = 5), store[1])
    }

    @Test
    fun `seeding refreshes averages but keeps the user's own score`() {
        store.put(FileRatingSummary(1, 3.0, 2, userRating = 4))

        store.seed(listOf(FileRatingSummary(1, 3.7, 3), FileRatingSummary(2, 5.0, 1)))

        assertEquals(FileRatingSummary(1, 3.7, 3, userRating = 4), store[1])
        assertEquals(FileRatingSummary(2, 5.0, 1, userRating = null), store[2])
    }

    @Test
    fun `clear forgets everything`() {
        store.put(FileRatingSummary(1, 3.0, 2, userRating = 4))

        store.clear()

        assertNull(store[1])
        assertTrue(store.ratings.value.isEmpty())
    }
}
