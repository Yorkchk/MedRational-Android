package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.Category
import com.example.medrational_android.data.model.FileSearchResult
import com.example.medrational_android.data.model.PageResponse
import com.example.medrational_android.data.rating.FileRatingStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

/** Advanced search from a quick search bar opens with the text and the category already set. */
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()
    private val ratingStore: FileRatingStore = mockk(relaxed = true)

    private val cardio = Category(id = 1, name = "Cardiologie", description = null)
    private val pneumo = Category(id = 2, name = "Pneumologie", description = null)
    private val result = FileSearchResult(
        id = 5, reasoningId = 3, reasoningTitle = "Syncope", categoryId = 2, categoryName = "Pneumologie",
        fileName = "ecg.pdf", fileType = null, publicUrl = "", fileSizeBytes = null
    )

    private fun stubApi() {
        coEvery { api.getCategories() } returns Response.success(listOf(cardio, pneumo))
        coEvery { api.searchFiles(any(), any()) } returns
            Response.success(PageResponse(content = listOf(result), totalElements = 1, totalPages = 1, number = 0))
    }

    @Test
    fun `opening it empty searches nothing`() {
        stubApi()

        val viewModel = SearchViewModel(api, ratingStore)

        assertEquals(SearchUiState.Idle, viewModel.uiState.value)
        coVerify(exactly = 0) { api.searchFiles(any(), any()) }
    }

    @Test
    fun `the typed text is searched right away`() {
        stubApi()

        val viewModel = SearchViewModel(api, ratingStore, initialQuery = "ecg")

        assertEquals("ecg", viewModel.searchQuery.value)
        assertEquals(SearchUiState.Success(listOf(result)), viewModel.uiState.value)
        coVerify { api.searchFiles("ecg", null) }
    }

    @Test
    fun `the category it came from is selected and searched`() {
        stubApi()

        val viewModel = SearchViewModel(api, ratingStore, initialQuery = "ecg", initialCategoryId = 2)

        assertEquals(pneumo, viewModel.selectedCategory.value)
        assertTrue(viewModel.uiState.value is SearchUiState.Success)
        coVerify { api.searchFiles("ecg", 2) }
    }
}
