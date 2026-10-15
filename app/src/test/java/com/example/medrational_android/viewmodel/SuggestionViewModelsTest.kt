package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.TestStrings.en
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.PageResponse
import com.example.medrational_android.data.model.SuggestionAttachment
import com.example.medrational_android.data.model.SuggestionDetail
import com.example.medrational_android.data.model.SuggestionSender
import com.example.medrational_android.data.model.SuggestionStatus
import com.example.medrational_android.data.model.SuggestionSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.File
import java.io.IOException

class SuggestionViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val api: MedRationalApi = mockk()

    private fun <T> error(code: Int): Response<T> = Response.error(code, "".toResponseBody(null))

    private fun RequestBody.text(): String = Buffer().also { writeTo(it) }.readUtf8()

    private fun summary(id: Long) = SuggestionSummary(
        id = id, title = "Suggestion $id", createdAt = null, senderName = "Sara", senderEmail = "sara@example.com"
    )

    private fun detail(attachment: SuggestionAttachment? = null) = SuggestionDetail(
        id = ID, title = "Add ECG atlas", description = "Please", createdAt = "2026-10-10T09:00:00",
        attachment = attachment, sender = SuggestionSender(1, "Sara", "sara@example.com", "0600000000")
    )

    private fun attachment(fileName: String, previewable: Boolean = true, contentType: String? = null) =
        SuggestionAttachment(fileName = fileName, contentType = contentType, sizeBytes = 4, previewable = previewable)

    private fun page(vararg items: SuggestionSummary) =
        Response.success(PageResponse(content = items.toList(), totalElements = items.size.toLong(), totalPages = 1, number = 0))

    // --- admin inbox -------------------------------------------------------------------------------

    @Test
    fun `inbox loads pending suggestions`() {
        coEvery { api.getSuggestionInbox(SuggestionStatus.PENDING, any()) } returns page(summary(1))

        val viewModel = SuggestionListViewModel(api)
        viewModel.load()

        assertEquals(SuggestionListUiState.Success(listOf(summary(1))), viewModel.uiState.value)
    }

    @Test
    fun `inbox network failure is an Error`() {
        coEvery { api.getSuggestionInbox(any(), any()) } throws IOException("offline")

        val viewModel = SuggestionListViewModel(api)
        viewModel.load()

        assertTrue(viewModel.uiState.value is SuggestionListUiState.Error)
    }

    // --- new suggestion ----------------------------------------------------------------------------

    @Test
    fun `blank title or description makes no request`() {
        val viewModel = NewSuggestionViewModel(api)

        viewModel.submit("  ", "Description", null)
        viewModel.submit("Title", "", null)

        assertEquals(NewSuggestionUiState.Idle, viewModel.uiState.value)
        coVerify(exactly = 0) { api.createSuggestion(any(), any(), any()) }
    }

    @Test
    fun `too long title makes no request`() {
        val viewModel = NewSuggestionViewModel(api)

        viewModel.submit("x".repeat(MAX_SUGGESTION_TITLE_LENGTH + 1), "Description", null)

        coVerify(exactly = 0) { api.createSuggestion(any(), any(), any()) }
    }

    @Test
    fun `sending trims the fields, passes the attachment and ends in Sent`() {
        val title = slot<RequestBody>()
        val description = slot<RequestBody>()
        val file = MultipartBody.Part.createFormData("file", "notes.pdf", "%PDF".toByteArray().toRequestBody())
        coEvery { api.createSuggestion(capture(title), capture(description), file) } returns Response.success(detail())

        val viewModel = NewSuggestionViewModel(api)
        viewModel.submit("  Add ECG atlas ", " Please add more ECGs ", file)

        assertEquals(NewSuggestionUiState.Sent, viewModel.uiState.value)
        assertEquals("Add ECG atlas", title.captured.text())
        assertEquals("Please add more ECGs", description.captured.text())
    }

    @Test
    fun `oversized attachment rejected by the backend gets a clear message`() {
        coEvery { api.createSuggestion(any(), any(), any()) } returns error(413)

        val viewModel = NewSuggestionViewModel(api)
        viewModel.submit("Title", "Description", null)

        assertEquals("Files must be 10 MB or smaller.", en((viewModel.uiState.value as NewSuggestionUiState.Error).message))
        viewModel.onErrorShown()
        assertEquals(NewSuggestionUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `unsupported file type gets a clear message`() {
        coEvery { api.createSuggestion(any(), any(), any()) } returns error(415)

        val viewModel = NewSuggestionViewModel(api)
        viewModel.submit("Title", "Description", null)

        assertTrue(en((viewModel.uiState.value as NewSuggestionUiState.Error).message)!!.contains("file type"))
    }

    // --- admin detail ------------------------------------------------------------------------------

    private val previewDir by lazy { tempFolder.newFolder("previews") }
    private val pdfBytes = "%PDF-1.7 fake".toByteArray()

    private fun newDetailViewModel() = SuggestionDetailViewModel(api, previewDir, mainDispatcherRule.dispatcher)

    private fun loadedDetail(suggestion: SuggestionDetail = detail()): SuggestionDetailViewModel {
        coEvery { api.getSuggestion(ID) } returns Response.success(suggestion)
        return newDetailViewModel().apply { load(ID) }
    }

    private fun SuggestionDetailViewModel.success() = uiState.value as SuggestionDetailUiState.Success

    @Test
    fun `detail loads the suggestion with its sender`() {
        val viewModel = loadedDetail()

        assertEquals(detail(), viewModel.success().suggestion)
        assertEquals("0600000000", viewModel.success().suggestion.sender.phoneNumber)
    }

    @Test
    fun `detail failure is an Error`() {
        coEvery { api.getSuggestion(ID) } returns error(404)

        val viewModel = newDetailViewModel()
        viewModel.load(ID)

        assertEquals("This suggestion no longer exists.", en((viewModel.uiState.value as SuggestionDetailUiState.Error).message))
    }

    @Test
    fun `deleting ends in Deleted`() {
        val viewModel = loadedDetail()
        coEvery { api.deleteSuggestion(ID) } returns Response.success(Unit)

        viewModel.delete()

        assertEquals(SuggestionDetailUiState.Deleted, viewModel.uiState.value)
    }

    @Test
    fun `deleting a suggestion that is already gone counts as deleted`() {
        val viewModel = loadedDetail()
        coEvery { api.deleteSuggestion(ID) } returns error(404)

        viewModel.delete()

        assertEquals(SuggestionDetailUiState.Deleted, viewModel.uiState.value)
    }

    @Test
    fun `failed delete keeps the suggestion and reports it`() {
        val viewModel = loadedDetail()
        coEvery { api.deleteSuggestion(ID) } returns error(500)

        viewModel.delete()

        assertEquals(detail(), viewModel.success().suggestion)
        assertFalse(viewModel.success().busy)
        assertTrue(en(viewModel.message.value)!!.contains("HTTP 500"))
        viewModel.onMessageShown()
        assertNull(viewModel.message.value)
    }

    @Test
    fun `network failure while deleting keeps the suggestion and reports it`() {
        val viewModel = loadedDetail()
        coEvery { api.deleteSuggestion(ID) } throws IOException("offline")

        viewModel.delete()

        assertFalse(viewModel.success().busy)
        assertTrue(en(viewModel.message.value)!!.contains("Network"))
    }

    @Test
    fun `a second delete while one is in flight makes no request`() {
        val viewModel = loadedDetail()
        val response = CompletableDeferred<Response<Unit>>()
        coEvery { api.deleteSuggestion(ID) } coAnswers { response.await() }

        viewModel.delete()
        assertTrue(viewModel.success().busy)
        viewModel.delete()
        response.complete(Response.success(Unit))

        coVerify(exactly = 1) { api.deleteSuggestion(ID) }
        assertEquals(SuggestionDetailUiState.Deleted, viewModel.uiState.value)
    }

    // --- attachment preview ------------------------------------------------------------------------

    private fun previewError(code: Int) = Response.error<ResponseBody>(code, "".toResponseBody(null))

    @Test
    fun `no attachment means no preview`() {
        val viewModel = loadedDetail()

        assertNull(viewModel.preview.value)
        coVerify(exactly = 0) { api.getSuggestionAttachmentPreview(any()) }
    }

    @Test
    fun `a pdf preview is downloaded and cached`() {
        coEvery { api.getSuggestionAttachmentPreview(ID) } returns Response.success(pdfBytes.toResponseBody(null))

        val viewModel = loadedDetail(detail(attachment("idea.docx")))

        val preview = viewModel.preview.value as PreviewState.Pdf
        assertEquals(File(previewDir, "suggestion-$ID.pdf"), preview.file)
        assertTrue(preview.file.readBytes().contentEquals(pdfBytes))
    }

    @Test
    fun `a cached preview is reused without a request`() {
        File(previewDir, "suggestion-$ID.pdf").writeBytes(pdfBytes)

        val viewModel = loadedDetail(detail(attachment("idea.pdf")))

        assertTrue(viewModel.preview.value is PreviewState.Pdf)
        coVerify(exactly = 0) { api.getSuggestionAttachmentPreview(any()) }
    }

    @Test
    fun `an image loads from the preview url without a request`() {
        val viewModel = loadedDetail(detail(attachment("screen.png", contentType = "image/png")))

        assertEquals(PreviewState.Image(ApiClient.suggestionAttachmentPreviewUrl(ID)), viewModel.preview.value)
        coVerify(exactly = 0) { api.getSuggestionAttachmentPreview(any()) }
    }

    @Test
    fun `a file that can't be previewed makes no request`() {
        val viewModel = loadedDetail(detail(attachment("notes.txt", previewable = false)))

        assertEquals(PreviewState.NotSupported, viewModel.preview.value)
        coVerify(exactly = 0) { api.getSuggestionAttachmentPreview(any()) }
    }

    @Test
    fun `a 415 from the backend means no preview`() {
        coEvery { api.getSuggestionAttachmentPreview(ID) } returns previewError(415)

        val viewModel = loadedDetail(detail(attachment("idea.pdf")))

        assertEquals(PreviewState.NotSupported, viewModel.preview.value)
    }

    @Test
    fun `a busy converter is reported and retrying fetches again`() {
        coEvery { api.getSuggestionAttachmentPreview(ID) } returnsMany listOf(
            previewError(503),
            Response.success(pdfBytes.toResponseBody(null))
        )
        val viewModel = loadedDetail(detail(attachment("idea.pptx")))

        val unavailable = viewModel.preview.value as PreviewState.Unavailable
        assertTrue(en(unavailable.message)!!.isNotBlank())

        viewModel.retryPreview()

        assertTrue(viewModel.preview.value is PreviewState.Pdf)
        coVerify(exactly = 2) { api.getSuggestionAttachmentPreview(ID) }
    }

    @Test
    fun `a network failure makes the preview unavailable`() {
        coEvery { api.getSuggestionAttachmentPreview(ID) } throws IOException("offline")

        val viewModel = loadedDetail(detail(attachment("idea.pdf")))

        assertTrue(viewModel.preview.value is PreviewState.Unavailable)
    }

    @Test
    fun `a corrupted cached preview is deleted`() {
        val cached = File(previewDir, "suggestion-$ID.pdf").apply { writeBytes(pdfBytes) }
        val viewModel = loadedDetail(detail(attachment("idea.pdf")))

        viewModel.onPreviewCorrupted()

        assertFalse(cached.exists())
        assertTrue(viewModel.preview.value is PreviewState.Unavailable)
    }

    @Test
    fun `deleting the suggestion drops its cached preview`() {
        val cached = File(previewDir, "suggestion-$ID.pdf").apply { writeBytes(pdfBytes) }
        val viewModel = loadedDetail(detail(attachment("idea.pdf")))
        coEvery { api.deleteSuggestion(ID) } returns Response.success(Unit)

        viewModel.delete()

        assertFalse(cached.exists())
    }

    private companion object {
        const val ID = 10L
    }
}
