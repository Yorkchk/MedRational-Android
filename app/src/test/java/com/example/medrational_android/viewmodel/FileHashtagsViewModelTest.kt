package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.AttachHashtagsRequest
import com.example.medrational_android.data.model.Hashtag
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class FileHashtagsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()

    private val cardio = Hashtag(1, "cardio")
    private val ecg = Hashtag(2, "ecg")

    private fun <T> errorResponse(code: Int) = Response.error<T>(code, "".toResponseBody(null))

    private fun loadedViewModel(vararg hashtags: Hashtag): FileHashtagsViewModel {
        coEvery { api.getFileHashtags(FILE_ID) } returns Response.success(hashtags.toList())
        return FileHashtagsViewModel(api).apply { load(FILE_ID) }
    }

    private fun FileHashtagsViewModel.success() = uiState.value as HashtagsUiState.Success

    @Test
    fun `load sorts hashtags by name`() {
        val viewModel = loadedViewModel(ecg, cardio)

        assertEquals(listOf(cardio, ecg), viewModel.success().hashtags)
    }

    @Test
    fun `load HTTP failure is an Error`() {
        coEvery { api.getFileHashtags(FILE_ID) } returns errorResponse(404)

        val viewModel = FileHashtagsViewModel(api)
        viewModel.load(FILE_ID)

        assertTrue(viewModel.uiState.value is HashtagsUiState.Error)
    }

    @Test
    fun `load network failure is an Error`() {
        coEvery { api.getFileHashtags(FILE_ID) } throws IOException("offline")

        val viewModel = FileHashtagsViewModel(api)
        viewModel.load(FILE_ID)

        assertTrue(viewModel.uiState.value is HashtagsUiState.Error)
    }

    @Test
    fun `add sanitises and dedupes input then shows the backend's list`() {
        val viewModel = loadedViewModel()
        coEvery {
            api.attachHashtags(FILE_ID, AttachHashtagsRequest(listOf("cardio", "ecg")))
        } returns Response.success(listOf(ecg, cardio))

        viewModel.addHashtags("#Cardio, ecg  ##ECG")

        coVerify(exactly = 1) { api.attachHashtags(FILE_ID, AttachHashtagsRequest(listOf("cardio", "ecg"))) }
        val state = viewModel.success()
        assertEquals(listOf(cardio, ecg), state.hashtags)
        assertFalse(state.busy)
    }

    @Test
    fun `blank add makes no request`() {
        val viewModel = loadedViewModel(cardio)

        viewModel.addHashtags("  #, ## ")

        coVerify(exactly = 0) { api.attachHashtags(any(), any()) }
    }

    @Test
    fun `add failure keeps the list and reports a message`() {
        val viewModel = loadedViewModel(cardio)
        coEvery { api.attachHashtags(FILE_ID, any()) } throws IOException("offline")

        viewModel.addHashtags("ecg")

        val state = viewModel.success()
        assertEquals(listOf(cardio), state.hashtags)
        assertFalse(state.busy)
        assertNotNull(state.message)

        viewModel.onMessageShown()
        assertNull(viewModel.success().message)
    }

    @Test
    fun `remove detaches the hashtag`() {
        val viewModel = loadedViewModel(cardio, ecg)
        coEvery { api.detachHashtag(FILE_ID, cardio.id) } returns Response.success(Unit)

        viewModel.removeHashtag(cardio)

        coVerify(exactly = 1) { api.detachHashtag(FILE_ID, cardio.id) }
        assertEquals(listOf(ecg), viewModel.success().hashtags)
        assertNull(viewModel.success().message)
    }

    @Test
    fun `failed remove restores the hashtag and reports a message`() {
        val viewModel = loadedViewModel(cardio, ecg)
        coEvery { api.detachHashtag(FILE_ID, cardio.id) } returns errorResponse(500)

        viewModel.removeHashtag(cardio)

        val state = viewModel.success()
        assertEquals(listOf(cardio, ecg), state.hashtags)
        assertNotNull(state.message)
    }

    @Test
    fun `rename attaches the new name before detaching the old one`() {
        val ekg = Hashtag(3, "ekg")
        val viewModel = loadedViewModel(cardio, ecg)
        coEvery {
            api.attachHashtags(FILE_ID, AttachHashtagsRequest(listOf("ekg")))
        } returns Response.success(listOf(cardio, ecg, ekg))
        coEvery { api.detachHashtag(FILE_ID, ecg.id) } returns Response.success(Unit)

        viewModel.renameHashtag(ecg, "#EKG")

        coVerifyOrder {
            api.attachHashtags(FILE_ID, AttachHashtagsRequest(listOf("ekg")))
            api.detachHashtag(FILE_ID, ecg.id)
        }
        val state = viewModel.success()
        assertEquals(listOf(cardio, ekg), state.hashtags)
        assertFalse(state.busy)
    }

    @Test
    fun `rename keeps the old hashtag when attaching the new name fails`() {
        val viewModel = loadedViewModel(cardio, ecg)
        coEvery { api.attachHashtags(FILE_ID, any()) } returns errorResponse(500)

        viewModel.renameHashtag(ecg, "ekg")

        coVerify(exactly = 0) { api.detachHashtag(any(), any()) }
        val state = viewModel.success()
        assertEquals(listOf(cardio, ecg), state.hashtags)
        assertNotNull(state.message)
    }

    @Test
    fun `rename to the same or several names makes no request`() {
        val viewModel = loadedViewModel(ecg)

        viewModel.renameHashtag(ecg, "#ECG")
        viewModel.renameHashtag(ecg, "ekg cardio")
        viewModel.renameHashtag(ecg, "  ")

        coVerify(exactly = 0) { api.attachHashtags(any(), any()) }
        coVerify(exactly = 0) { api.detachHashtag(any(), any()) }
    }

    private companion object {
        const val FILE_ID = 42L
    }
}
