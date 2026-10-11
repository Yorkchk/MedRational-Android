package com.example.medrational_android.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoveltyItemGsonTest {

    private val gson = Gson()

    private fun parse(flag: String) = gson.fromJson(
        """{"fileId":5,"fileName":"ecg.pdf","reasoningId":7,"uploadedAt":"2026-10-10T09:30:00",$flag}""",
        NoveltyItem::class.java
    )

    @Test
    fun `isNew is read from the backend's isNew key`() {
        assertTrue(parse(""""isNew":true""").isNew)
    }

    // Lombok's isNew() getter made Jackson write the flag as "new"
    @Test
    fun `isNew is also read from the legacy new key`() {
        assertTrue(parse(""""new":true""").isNew)
    }

    @Test
    fun `missing flag means not new`() {
        assertFalse(parse(""""hashtags":[]""").isNew)
    }

    @Test
    fun `feed page is nested under feed`() {
        val feed = gson.fromJson(
            """{"newFilesCount":2,"feed":{"content":[{"fileId":5,"isNew":true}],"totalElements":1,"totalPages":1,"number":0}}""",
            NoveltiesFeed::class.java
        )

        assertEquals(2L, feed.newFilesCount)
        assertEquals(5L, feed.feed.content.single().fileId)
    }
}
