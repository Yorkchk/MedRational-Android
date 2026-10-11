package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.NoveltiesFeed
import com.example.medrational_android.data.model.NoveltyItem
import com.example.medrational_android.data.model.PageResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class NoveltiesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()

    private fun item(fileId: Long, isNew: Boolean = false, reasoningId: Long? = 7) = NoveltyItem(
        fileId = fileId,
        fileName = "file$fileId.pdf",
        fileType = "application/pdf",
        fileSizeBytes = 1024,
        uploadedAt = "2026-10-10T09:30:00",
        categoryId = 1,
        categoryName = "Cardiology",
        reasoningId = reasoningId,
        reasoningTitle = "Chest pain",
        isNew = isNew
    )

    private fun page(vararg items: NoveltyItem, number: Int = 0, totalPages: Int = 1, newCount: Long = 0) =
        Response.success(
            NoveltiesFeed(
                newFilesCount = newCount,
                feed = PageResponse(items.toList(), totalElements = items.size.toLong(), totalPages = totalPages, number = number)
            )
        )

    private fun stubMarkSeen() {
        coEvery { api.markNoveltiesSeen() } returns Response.success(Unit)
    }

    private fun success(viewModel: NoveltiesViewModel) = viewModel.uiState.value as NoveltiesUiState.Success

    @Test
    fun `feed keeps the backend order and new flags`() {
        val newest = item(3, isNew = true)
        val older = item(1)
        coEvery { api.getNovelties(0, any()) } returns page(newest, older, newCount = 1)
        stubMarkSeen()

        val viewModel = NoveltiesViewModel(api)

        assertEquals(
            NoveltiesUiState.Success(listOf(newest, older), newCount = 1, canLoadMore = false),
            viewModel.uiState.value
        )
    }

    @Test
    fun `feed is marked seen once after it loads`() {
        coEvery { api.getNovelties(any(), any()) } returns page(item(1), totalPages = 2)
        stubMarkSeen()

        val viewModel = NoveltiesViewModel(api)
        viewModel.loadMore()

        coVerify(exactly = 1) { api.markNoveltiesSeen() }
    }

    @Test
    fun `feed is not marked seen when it fails to load`() {
        coEvery { api.getNovelties(any(), any()) } throws IOException("offline")

        val viewModel = NoveltiesViewModel(api)

        assertTrue(viewModel.uiState.value is NoveltiesUiState.Error)
        coVerify(exactly = 0) { api.markNoveltiesSeen() }
    }

    @Test
    fun `failing to mark seen still shows the feed`() {
        val file = item(1, isNew = true)
        coEvery { api.getNovelties(0, any()) } returns page(file, newCount = 1)
        coEvery { api.markNoveltiesSeen() } throws IOException("offline")

        val viewModel = NoveltiesViewModel(api)

        assertEquals(listOf(file), success(viewModel).items)
    }

    @Test
    fun `load more appends the next page and stops at the last one`() {
        val first = item(3, isNew = true)
        val second = item(2)
        coEvery { api.getNovelties(0, any()) } returns page(first, number = 0, totalPages = 2)
        coEvery { api.getNovelties(1, any()) } returns page(second, number = 1, totalPages = 2)
        stubMarkSeen()

        val viewModel = NoveltiesViewModel(api)
        assertTrue(success(viewModel).canLoadMore)
        viewModel.loadMore()

        assertEquals(listOf(first, second), success(viewModel).items)
        assertFalse(success(viewModel).canLoadMore)

        viewModel.loadMore()
        coVerify(exactly = 1) { api.getNovelties(1, any()) }
        coVerify(exactly = 0) { api.getNovelties(2, any()) }
    }

    @Test
    fun `files shifted onto the next page by a new upload are not shown twice`() {
        val first = item(3)
        val shifted = item(2)
        coEvery { api.getNovelties(0, any()) } returns page(first, shifted, number = 0, totalPages = 2)
        coEvery { api.getNovelties(1, any()) } returns page(shifted, item(1), number = 1, totalPages = 2)
        stubMarkSeen()

        val viewModel = NoveltiesViewModel(api)
        viewModel.loadMore()

        assertEquals(listOf(3L, 2L, 1L), success(viewModel).items.map { it.fileId })
    }

    @Test
    fun `failed load more keeps the list and can be retried`() {
        val first = item(3)
        val second = item(2)
        coEvery { api.getNovelties(0, any()) } returns page(first, totalPages = 2)
        coEvery { api.getNovelties(1, any()) } throws IOException("offline") andThen page(second, number = 1, totalPages = 2)
        stubMarkSeen()

        val viewModel = NoveltiesViewModel(api)
        viewModel.loadMore()

        assertEquals(listOf(first), success(viewModel).items)
        assertTrue(success(viewModel).loadMoreFailed)

        viewModel.loadMore()

        assertEquals(listOf(first, second), success(viewModel).items)
        assertFalse(success(viewModel).loadMoreFailed)
    }

    @Test
    fun `files that can't be opened are dropped`() {
        val openable = item(3)
        coEvery { api.getNovelties(0, any()) } returns page(openable, item(4, reasoningId = null))
        stubMarkSeen()

        assertEquals(listOf(openable), success(NoveltiesViewModel(api)).items)
    }

    @Test
    fun `HTTP failure is an Error`() {
        coEvery { api.getNovelties(any(), any()) } returns Response.error(500, "".toResponseBody(null))

        assertTrue(NoveltiesViewModel(api).uiState.value is NoveltiesUiState.Error)
    }

    @Test
    fun `retry after an error recovers`() {
        val file = item(1)
        coEvery { api.getNovelties(0, any()) } throws IOException("offline") andThen page(file)
        stubMarkSeen()

        val viewModel = NoveltiesViewModel(api)
        viewModel.retry()

        assertEquals(listOf(file), success(viewModel).items)
    }
}
