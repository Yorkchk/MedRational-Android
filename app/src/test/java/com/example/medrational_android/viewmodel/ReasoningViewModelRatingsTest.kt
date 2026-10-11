package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.FileRatingSummary
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.data.rating.FileRatingStore
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class ReasoningViewModelRatingsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()
    private val store = FileRatingStore()

    private fun file(id: Long, avg: Double, total: Int) = StudyFile(
        id = id,
        reasoningId = 7,
        fileName = "file$id.pdf",
        fileType = "application/pdf",
        publicUrl = "https://cdn.example/file$id.pdf",
        fileSizeBytes = 1,
        avgRating = avg,
        totalRatings = total
    )

    @Test
    fun `every file's rating goes into the shared store`() {
        coEvery { api.getReasoningsByCategory(1) } returns Response.success(
            listOf(
                Reasoning(id = 7, categoryId = 1, title = "A", content = null, files = listOf(file(1, 4.0, 2))),
                Reasoning(id = 8, categoryId = 1, title = "B", content = null, files = listOf(file(2, 0.0, 0)))
            )
        )

        ReasoningViewModel(api, store).loadReasoningsForCategory(1)

        assertEquals(
            mapOf(1L to FileRatingSummary(1, 4.0, 2), 2L to FileRatingSummary(2, 0.0, 0)),
            store.ratings.value
        )
    }
}
