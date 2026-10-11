package com.example.medrational_android.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatingFieldsGsonTest {

    private val gson = Gson()

    @Test
    fun `study file reads its rating stats`() {
        val file = gson.fromJson(
            """{"id":1,"reasoningId":7,"fileName":"ecg.pdf","publicUrl":"u","avgRating":4.5,"totalRatings":12}""",
            StudyFile::class.java
        )

        assertEquals(4.5, file.avgRating, 0.0)
        assertEquals(12, file.totalRatings)
    }

    // Gson skips Kotlin defaults for missing keys, so these must come back as 0 rather than crash
    @Test
    fun `missing rating stats read as unrated`() {
        val file = gson.fromJson(
            """{"id":1,"reasoningId":7,"fileName":"ecg.pdf","publicUrl":"u"}""",
            StudyFile::class.java
        )

        assertEquals(0.0, file.avgRating, 0.0)
        assertEquals(0, file.totalRatings)
    }

    @Test
    fun `summary without a user score has a null userRating`() {
        val summary = gson.fromJson(
            """{"fileId":1,"avgRating":3.7,"totalRatings":3,"userRating":null}""",
            FileRatingSummary::class.java
        )

        assertEquals(FileRatingSummary(1, 3.7, 3), summary)
        assertNull(summary.userRating)
    }
}
