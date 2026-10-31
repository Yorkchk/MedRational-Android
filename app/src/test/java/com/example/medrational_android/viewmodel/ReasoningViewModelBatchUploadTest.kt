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
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ReasoningViewModelBatchUploadTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()

    private fun part(name: String) = MultipartBody.Part.createFormData("files", name, "pdf".toByteArray().toRequestBody())

    private fun studyFile(id: Long, name: String) = StudyFile(
        id = id,
        reasoningId = REASONING_ID,
        fileName = name,
        fileType = "application/pdf",
        publicUrl = "https://cdn.example/$name",
        fileSizeBytes = 3
    )

    private val a = part("a.pdf")
    private val b = part("b.pdf")
    private val c = part("c.pdf")

    private fun batch(vararg parts: MultipartBody.Part): suspend () -> List<MultipartBody.Part> = { parts.toList() }

    private fun fileCount(count: Int) = UiText.Plural(R.plurals.file_count, count)

    @Before
    fun stubReload() {
        coEvery { api.getReasoningsByCategory(any()) } returns Response.success(emptyList())
    }

    // One request for the batch, so students get one push
    @Test
    fun `files picked together go in one batch request`() = runTest {
        coEvery { api.uploadFiles(REASONING_ID, listOf(a, b, c), null) } returns
            Response.success(listOf(studyFile(1, "a.pdf"), studyFile(2, "b.pdf"), studyFile(3, "c.pdf")))
        val viewModel = ReasoningViewModel(api)

        viewModel.uploadBatchesAndTag(REASONING_ID, listOf(batch(a, b, c)), emptyList(), total = 3)

        coVerify(exactly = 1) { api.uploadFiles(REASONING_ID, listOf(a, b, c), null) }
        coVerify(exactly = 0) { api.uploadFile(any(), any(), any()) }
        coVerify(exactly = 0) { api.attachHashtags(any(), any()) }
        assertEquals(UiText.Plural(R.plurals.upload_many_done, 3), viewModel.message.value)
        coVerify(exactly = 1) { api.getReasoningsByCategory(any()) }
    }

    @Test
    fun `every uploaded file gets the chosen hashtags`() = runTest {
        coEvery { api.uploadFiles(REASONING_ID, any(), null) } returns
            Response.success(listOf(studyFile(1, "a.pdf"), studyFile(2, "b.pdf")))
        coEvery { api.attachHashtags(any(), any()) } returns Response.success(emptyList())
        val viewModel = ReasoningViewModel(api)

        viewModel.uploadBatchesAndTag(REASONING_ID, listOf(batch(a, b)), listOf("ecg"), total = 2)

        coVerify(exactly = 1) { api.attachHashtags(1, AttachHashtagsRequest(listOf("ecg"))) }
        coVerify(exactly = 1) { api.attachHashtags(2, AttachHashtagsRequest(listOf("ecg"))) }
        assertEquals(UiText.Plural(R.plurals.upload_many_done_tagged, 2), viewModel.message.value)
    }

    @Test
    fun `a failed batch doesn't stop the others`() = runTest {
        coEvery { api.uploadFiles(REASONING_ID, listOf(a, b), null) } throws IOException("offline")
        coEvery { api.uploadFiles(REASONING_ID, listOf(c), null) } returns Response.success(listOf(studyFile(3, "c.pdf")))
        val viewModel = ReasoningViewModel(api)

        viewModel.uploadBatchesAndTag(REASONING_ID, listOf(batch(a, b), batch(c)), emptyList(), total = 3)

        assertEquals(UiText.Res(R.string.upload_many_partial, listOf(1, fileCount(3))), viewModel.message.value)
    }

    // A request over the backend's limit answers 413
    @Test
    fun `a refused batch counts as not sent`() = runTest {
        coEvery { api.uploadFiles(REASONING_ID, any(), null) } returns Response.error(413, "".toResponseBody(null))
        val viewModel = ReasoningViewModel(api)

        viewModel.uploadBatchesAndTag(REASONING_ID, listOf(batch(a, b)), emptyList(), total = 2)

        assertEquals(UiText.Res(R.string.upload_error_failed), viewModel.message.value)
    }

    // Files too big or unreadable never reach a batch, but the admin picked them
    @Test
    fun `files left out count as not sent`() = runTest {
        coEvery { api.uploadFiles(REASONING_ID, any(), null) } returns
            Response.success(listOf(studyFile(1, "a.pdf"), studyFile(2, "b.pdf")))
        val viewModel = ReasoningViewModel(api)

        viewModel.uploadBatchesAndTag(REASONING_ID, listOf(batch(a, b)), emptyList(), total = 3)

        assertEquals(UiText.Res(R.string.upload_many_partial, listOf(2, fileCount(3))), viewModel.message.value)
    }

    @Test
    fun `a file that can't be read fails only its batch`() = runTest {
        coEvery { api.uploadFiles(REASONING_ID, listOf(c), null) } returns Response.success(listOf(studyFile(3, "c.pdf")))
        val unreadable: suspend () -> List<MultipartBody.Part> = { throw SecurityException("permission revoked") }
        val viewModel = ReasoningViewModel(api)

        viewModel.uploadBatchesAndTag(REASONING_ID, listOf(unreadable, batch(c)), emptyList(), total = 2)

        assertEquals(UiText.Res(R.string.upload_many_partial, listOf(1, fileCount(2))), viewModel.message.value)
    }

    @Test
    fun `failed tagging keeps the uploads and says so`() = runTest {
        coEvery { api.uploadFiles(REASONING_ID, any(), null) } returns
            Response.success(listOf(studyFile(1, "a.pdf"), studyFile(2, "b.pdf")))
        coEvery { api.attachHashtags(1, any()) } returns Response.success(emptyList())
        coEvery { api.attachHashtags(2, any()) } returns Response.error(500, "".toResponseBody(null))
        val viewModel = ReasoningViewModel(api)

        viewModel.uploadBatchesAndTag(REASONING_ID, listOf(batch(a, b)), listOf("ecg"), total = 2)

        assertEquals(UiText.Res(R.string.upload_many_tags_failed, listOf(fileCount(2), fileCount(1))), viewModel.message.value)
        coVerify { api.getReasoningsByCategory(any()) }
    }

    // Like the hashtags, one description goes with every batch of the selection
    @Test
    fun `description is sent with every batch`() = runTest {
        val descriptions = mutableListOf<RequestBody?>()
        coEvery { api.uploadFiles(REASONING_ID, listOf(a, b), captureNullable(descriptions)) } returns
            Response.success(listOf(studyFile(1, "a.pdf"), studyFile(2, "b.pdf")))
        coEvery { api.uploadFiles(REASONING_ID, listOf(c), captureNullable(descriptions)) } returns
            Response.success(listOf(studyFile(3, "c.pdf")))
        val viewModel = ReasoningViewModel(api)

        viewModel.uploadBatchesAndTag(
            REASONING_ID, listOf(batch(a, b), batch(c)), emptyList(), total = 3, description = "Week 3 slides"
        )

        assertEquals(listOf("Week 3 slides", "Week 3 slides"), descriptions.map { it.text() })
        assertEquals(UiText.Plural(R.plurals.upload_many_done, 3), viewModel.message.value)
    }

    private fun RequestBody?.text(): String? = this?.let { body -> Buffer().also { body.writeTo(it) }.readUtf8() }

    private companion object {
        const val REASONING_ID = 7L
    }
}
