package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.R
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.AttachHashtagsRequest
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.ui.text.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ReasoningViewModelUploadTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()
    private val part = MultipartBody.Part.createFormData("file", "ecg.pdf", "pdf".toByteArray().toRequestBody())
    private val uploaded = StudyFile(
        id = FILE_ID,
        reasoningId = REASONING_ID,
        fileName = "ecg.pdf",
        fileType = "application/pdf",
        publicUrl = "https://cdn.example/ecg.pdf",
        fileSizeBytes = 3
    )

    @Before
    fun stubReload() {
        coEvery { api.getReasoningsByCategory(any()) } returns Response.success(emptyList())
    }

    @Test
    fun `uploaded file gets the chosen hashtags`() = runTest {
        coEvery { api.uploadFile(REASONING_ID, part) } returns Response.success(uploaded)
        coEvery { api.attachHashtags(FILE_ID, any()) } returns Response.success(emptyList())
        val viewModel = ReasoningViewModel(api)

        viewModel.uploadAndTag(REASONING_ID, part, listOf("ecg", "cardio"))

        coVerify(exactly = 1) { api.attachHashtags(FILE_ID, AttachHashtagsRequest(listOf("ecg", "cardio"))) }
        assertEquals(UiText.Res(R.string.upload_done_tagged, listOf("ecg.pdf")), viewModel.message.value)
    }

    @Test
    fun `upload without hashtags never calls attach`() = runTest {
        coEvery { api.uploadFile(REASONING_ID, part) } returns Response.success(uploaded)
        val viewModel = ReasoningViewModel(api)

        viewModel.uploadAndTag(REASONING_ID, part, emptyList())

        coVerify(exactly = 0) { api.attachHashtags(any(), any()) }
        assertEquals(UiText.Res(R.string.upload_done, listOf("ecg.pdf")), viewModel.message.value)
    }

    @Test
    fun `failed upload reports it and never attaches`() = runTest {
        coEvery { api.uploadFile(REASONING_ID, part) } throws IOException("offline")
        val viewModel = ReasoningViewModel(api)

        viewModel.uploadAndTag(REASONING_ID, part, listOf("ecg"))

        coVerify(exactly = 0) { api.attachHashtags(any(), any()) }
        assertEquals(UiText.Res(R.string.upload_error_failed), viewModel.message.value)
    }

    @Test
    fun `failed tagging still keeps the upload and says so`() = runTest {
        coEvery { api.uploadFile(REASONING_ID, part) } returns Response.success(uploaded)
        coEvery { api.attachHashtags(FILE_ID, any()) } returns Response.error(500, "".toResponseBody(null))
        val viewModel = ReasoningViewModel(api)

        viewModel.uploadAndTag(REASONING_ID, part, listOf("ecg"))

        assertEquals(UiText.Res(R.string.upload_done_tags_failed, listOf("ecg.pdf")), viewModel.message.value)
        coVerify { api.getReasoningsByCategory(any()) }
    }

    private companion object {
        const val REASONING_ID = 7L
        const val FILE_ID = 42L
    }
}
