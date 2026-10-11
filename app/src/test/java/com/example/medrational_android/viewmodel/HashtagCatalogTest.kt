package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.HashtagUsage
import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class HashtagCatalogTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()

    private val catalog = listOf(
        HashtagUsage("ecg", 3),
        HashtagUsage("cardio", 12),
        HashtagUsage("pericarditis", 20),
        HashtagUsage("cardiology", 5),
        HashtagUsage("exam", 40)
    )

    private fun names(usages: List<HashtagUsage>) = usages.map { it.hashtag }

    @Test
    fun `prefix matches come before contains matches, then by usage`() {
        assertEquals(
            listOf("cardio", "cardiology", "pericarditis"),
            names(suggestHashtags(catalog, "card"))
        )
    }

    @Test
    fun `query ignores a leading hash and letter case`() {
        assertEquals(listOf("ecg"), names(suggestHashtags(catalog, "#ECG")))
    }

    @Test
    fun `empty query returns the most used tags`() {
        assertEquals(
            listOf("exam", "pericarditis", "cardio"),
            names(suggestHashtags(catalog, "", max = 3))
        )
    }

    @Test
    fun `excluded tags are never suggested`() {
        assertEquals(
            listOf("cardiology", "pericarditis"),
            names(suggestHashtags(catalog, "card", exclude = setOf("cardio")))
        )
    }

    @Test
    fun `suggestions are capped at max`() {
        assertEquals(2, suggestHashtags(catalog, "", max = 2).size)
    }

    @Test
    fun `catalog loads every hashtag in use`() {
        coEvery { api.getHashtagUsage(any()) } returns Response.success(catalog)

        val viewModel = HashtagCatalogViewModel(api)
        viewModel.refresh()

        assertEquals(catalog, viewModel.catalog.value)
    }

    @Test
    fun `catalog stays empty when the request fails`() {
        coEvery { api.getHashtagUsage(any()) } returns Response.error(500, "".toResponseBody(null))

        val viewModel = HashtagCatalogViewModel(api)
        viewModel.refresh()

        assertTrue(viewModel.catalog.value.isEmpty())
    }

    @Test
    fun `catalog keeps the last list after a network failure`() {
        coEvery { api.getHashtagUsage(any()) } returns Response.success(catalog) andThenThrows IOException("offline")

        val viewModel = HashtagCatalogViewModel(api)
        viewModel.refresh()
        viewModel.refresh()

        assertEquals(catalog, viewModel.catalog.value)
    }
}
