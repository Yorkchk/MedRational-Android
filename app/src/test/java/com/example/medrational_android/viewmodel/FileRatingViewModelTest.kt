package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.TestStrings.en
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.FileRatingSummary
import com.example.medrational_android.data.model.RateFileRequest
import com.example.medrational_android.data.rating.FileRatingStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class FileRatingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()
    private val store = FileRatingStore()

    private fun summary(avg: Double, total: Int, mine: Int? = null) = FileRatingSummary(FILE_ID, avg, total, mine)

    private fun loadedViewModel(initial: FileRatingSummary): FileRatingViewModel {
        coEvery { api.getFileRating(FILE_ID) } returns Response.success(initial)
        return FileRatingViewModel(api, store).apply { load(FILE_ID) }
    }

    private fun FileRatingViewModel.success() = uiState.value as FileRatingUiState.Success

    @Test
    fun `load puts the summary in the shared store`() {
        val viewModel = loadedViewModel(summary(4.0, 3, mine = 5))

        assertEquals(summary(4.0, 3, mine = 5), viewModel.success().summary)
        assertEquals(summary(4.0, 3, mine = 5), store[FILE_ID])
    }

    @Test
    fun `a score cached for someone else isn't shown before the load returns`() {
        store.put(summary(4.0, 3, mine = 2))
        val pending = CompletableDeferred<Response<FileRatingSummary>>()
        coEvery { api.getFileRating(FILE_ID) } coAnswers { pending.await() }

        val viewModel = FileRatingViewModel(api, store).apply { load(FILE_ID) }
        assertEquals(FileRatingUiState.Loading, viewModel.uiState.value)

        pending.complete(Response.success(summary(4.0, 3, mine = null)))
        assertNull(viewModel.success().summary.userRating)
    }

    @Test
    fun `failed load is an error`() {
        coEvery { api.getFileRating(FILE_ID) } throws IOException("offline")

        val viewModel = FileRatingViewModel(api, store).apply { load(FILE_ID) }

        assertTrue(viewModel.uiState.value is FileRatingUiState.Error)
    }

    @Test
    fun `first rating shows at once and then takes the server figures`() {
        val viewModel = loadedViewModel(summary(3.0, 2))
        val pending = CompletableDeferred<Response<FileRatingSummary>>()
        coEvery { api.rateFile(FILE_ID, RateFileRequest(5)) } coAnswers { pending.await() }

        viewModel.rate(5)

        // (3 + 3 + 5) / 3
        assertEquals(summary(3.7, 3, mine = 5), store[FILE_ID])
        assertTrue(viewModel.success().isSubmitting)

        pending.complete(Response.success(summary(3.6, 3, mine = 5)))
        assertEquals(summary(3.6, 3, mine = 5), store[FILE_ID])
        assertFalse(viewModel.success().isSubmitting)
    }

    @Test
    fun `changing a rating keeps the count and moves the average`() {
        val viewModel = loadedViewModel(summary(3.0, 2, mine = 2))
        val pending = CompletableDeferred<Response<FileRatingSummary>>()
        coEvery { api.rateFile(FILE_ID, RateFileRequest(5)) } coAnswers { pending.await() }

        viewModel.rate(5)

        // The other rating is a 4: (4 + 5) / 2
        assertEquals(summary(4.5, 2, mine = 5), store[FILE_ID])
        pending.complete(Response.success(summary(4.5, 2, mine = 5)))
        coVerify(exactly = 1) { api.rateFile(FILE_ID, RateFileRequest(5)) }
    }

    @Test
    fun `removing a rating drops it from the count and average`() {
        val viewModel = loadedViewModel(summary(3.0, 2, mine = 2))
        coEvery { api.removeFileRating(FILE_ID) } returns Response.success(summary(4.0, 1, mine = null))

        viewModel.removeRating()

        assertEquals(summary(4.0, 1, mine = null), store[FILE_ID])
    }

    @Test
    fun `removing the only rating leaves the file unrated`() {
        val viewModel = loadedViewModel(summary(4.0, 1, mine = 4))
        val pending = CompletableDeferred<Response<FileRatingSummary>>()
        coEvery { api.removeFileRating(FILE_ID) } coAnswers { pending.await() }

        viewModel.removeRating()

        assertEquals(summary(0.0, 0, mine = null), store[FILE_ID])
        pending.complete(Response.success(summary(0.0, 0)))
    }

    @Test
    fun `failed rating restores the previous one and reports it`() {
        val viewModel = loadedViewModel(summary(3.0, 2, mine = 2))
        coEvery { api.rateFile(FILE_ID, any()) } throws IOException("offline")

        viewModel.rate(5)

        assertEquals(summary(3.0, 2, mine = 2), store[FILE_ID])
        assertEquals("Network error. Your rating wasn't saved.", en(viewModel.success().message))
        assertFalse(viewModel.success().isSubmitting)

        viewModel.onMessageShown()
        assertNull(viewModel.success().message)
    }

    @Test
    fun `rejected removal restores the rating`() {
        val viewModel = loadedViewModel(summary(3.0, 2, mine = 2))
        coEvery { api.removeFileRating(FILE_ID) } returns Response.error(500, "".toResponseBody(null))

        viewModel.removeRating()

        assertEquals(summary(3.0, 2, mine = 2), store[FILE_ID])
        assertEquals("Couldn't save your rating (HTTP 500).", en(viewModel.success().message))
    }

    @Test
    fun `taps while a request is in flight are ignored`() {
        val viewModel = loadedViewModel(summary(3.0, 2))
        val pending = CompletableDeferred<Response<FileRatingSummary>>()
        coEvery { api.rateFile(FILE_ID, any()) } coAnswers { pending.await() }

        viewModel.rate(4)
        viewModel.rate(1)
        viewModel.removeRating()

        pending.complete(Response.success(summary(3.3, 3, mine = 4)))
        coVerify(exactly = 1) { api.rateFile(FILE_ID, any()) }
        coVerify(exactly = 0) { api.removeFileRating(any()) }
        assertEquals(4, store[FILE_ID]?.userRating)
    }

    @Test
    fun `tapping the current score sends nothing`() {
        val viewModel = loadedViewModel(summary(3.0, 2, mine = 3))

        viewModel.rate(3)

        coVerify(exactly = 0) { api.rateFile(any(), any()) }
    }

    @Test
    fun `optimistic averages stay within the star range`() {
        // An average of 1.04 rounded to 1.0 would otherwise give a negative sum for the other ratings
        assertEquals(1.0, withUserRating(FileRatingSummary(FILE_ID, 1.0, 2, userRating = 2), 1).avgRating, 0.0)
        assertEquals(5.0, withUserRating(FileRatingSummary(FILE_ID, 5.0, 1, userRating = 5), 5).avgRating, 0.0)
    }

    private companion object {
        const val FILE_ID = 42L
    }
}
