package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.FileRatingSummary
import com.example.medrational_android.data.model.RecentDownload
import com.example.medrational_android.data.rating.FileRatingStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RecentDownloadsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()

    private fun download(fileId: Long?, reasoningId: Long? = 7, name: String = "file$fileId.pdf") = RecentDownload(
        fileId = fileId,
        fileName = name,
        fileType = "application/pdf",
        fileSizeBytes = 1024,
        downloadedAt = "2026-10-10T09:30:00",
        categoryId = 1,
        categoryName = "Cardiology",
        reasoningId = reasoningId,
        reasoningTitle = "Chest pain"
    )

    private fun newViewModel() = RecentDownloadsViewModel(api, USER_ID)

    @Test
    fun `downloads keep the backend's newest-first order`() {
        val newest = download(3)
        val older = download(1)
        coEvery { api.getRecentDownloads(USER_ID, any()) } returns Response.success(listOf(newest, older))

        val viewModel = newViewModel()
        viewModel.load()

        assertEquals(RecentDownloadsUiState.Success(listOf(newest, older)), viewModel.uiState.value)
    }

    @Test
    fun `entries that can't be opened are dropped`() {
        val openable = download(3)
        coEvery { api.getRecentDownloads(USER_ID, any()) } returns Response.success(
            listOf(openable, download(fileId = null), download(fileId = 5, reasoningId = null))
        )

        val viewModel = newViewModel()
        viewModel.load()

        assertEquals(RecentDownloadsUiState.Success(listOf(openable)), viewModel.uiState.value)
    }

    @Test
    fun `requests the signed-in user's downloads with the backend default limit`() {
        coEvery { api.getRecentDownloads(any(), any()) } returns Response.success(emptyList())

        newViewModel().load()

        coVerify(exactly = 1) { api.getRecentDownloads(USER_ID, null) }
    }

    @Test
    fun `HTTP failure is an Error`() {
        coEvery { api.getRecentDownloads(USER_ID, any()) } returns Response.error(500, "".toResponseBody(null))

        val viewModel = newViewModel()
        viewModel.load()

        assertTrue(viewModel.uiState.value is RecentDownloadsUiState.Error)
    }

    @Test
    fun `network failure is an Error`() {
        coEvery { api.getRecentDownloads(USER_ID, any()) } throws IOException("offline")

        val viewModel = newViewModel()
        viewModel.load()

        assertTrue(viewModel.uiState.value is RecentDownloadsUiState.Error)
    }

    @Test
    fun `retry after an error recovers`() {
        val file = download(3)
        coEvery { api.getRecentDownloads(USER_ID, any()) } throws IOException("offline") andThen Response.success(listOf(file))

        val viewModel = newViewModel()
        viewModel.load()
        viewModel.load()

        assertEquals(RecentDownloadsUiState.Success(listOf(file)), viewModel.uiState.value)
    }

    @Test
    fun `loaded ratings go into the shared store`() {
        val store = FileRatingStore()
        store.put(FileRatingSummary(3, 2.0, 1, userRating = 2))
        coEvery { api.getRecentDownloads(USER_ID, any()) } returns Response.success(
            listOf(download(3).copy(avgRating = 3.5, totalRatings = 2), download(fileId = null))
        )

        RecentDownloadsViewModel(api, USER_ID, store).load()

        assertEquals(mapOf(3L to FileRatingSummary(3, 3.5, 2, userRating = 2)), store.ratings.value)
    }

    private companion object {
        const val USER_ID = 11L
    }
}
