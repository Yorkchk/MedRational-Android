package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.PageResponse
import com.example.medrational_android.data.model.SuggestionDetail
import com.example.medrational_android.data.model.SuggestionMessage
import com.example.medrational_android.data.model.SuggestionSender
import com.example.medrational_android.data.model.SuggestionStatus
import com.example.medrational_android.data.model.SuggestionSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SuggestionViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()

    private fun <T> error(code: Int): Response<T> = Response.error(code, "".toResponseBody(null))

    private fun RequestBody.text(): String = Buffer().also { writeTo(it) }.readUtf8()

    private fun summary(id: Long, status: SuggestionStatus = SuggestionStatus.PENDING) = SuggestionSummary(
        id = id, title = "Suggestion $id", status = status, createdAt = null, lastActivityAt = null,
        senderName = "Sara", senderEmail = "sara@example.com"
    )

    private fun detail(status: SuggestionStatus = SuggestionStatus.PENDING, messages: List<SuggestionMessage> = emptyList()) =
        SuggestionDetail(
            id = ID, title = "Add ECG atlas", description = "Please", status = status,
            createdAt = "2026-10-10T09:00:00", lastActivityAt = "2026-10-10T09:00:00", closedAt = null,
            attachment = null, sender = SuggestionSender(1, "Sara", "sara@example.com", null), messages = messages
        )

    private fun message(id: Long, body: String = "Reply $id") = SuggestionMessage(
        id = id, fromAdmin = false, authorName = "Sara", body = body, attachment = null,
        createdAt = "2026-10-10T10:00:00"
    )

    private fun page(vararg items: SuggestionSummary) =
        Response.success(PageResponse(content = items.toList(), totalElements = items.size.toLong(), totalPages = 1, number = 0))

    // --- list --------------------------------------------------------------------------------------

    @Test
    fun `admins load the inbox`() {
        coEvery { api.getSuggestionInbox(SuggestionStatus.PENDING, any()) } returns page(summary(1))

        val viewModel = SuggestionListViewModel(api, isAdmin = true)
        viewModel.load()

        assertEquals(SuggestionListUiState.Success(listOf(summary(1))), viewModel.uiState.value)
        coVerify(exactly = 0) { api.getMySuggestions(any(), any()) }
    }

    @Test
    fun `users load only their own suggestions and never the admin inbox`() {
        coEvery { api.getMySuggestions(SuggestionStatus.PENDING, any()) } returns page(summary(2))

        val viewModel = SuggestionListViewModel(api, isAdmin = false)
        viewModel.load()

        assertEquals(SuggestionListUiState.Success(listOf(summary(2))), viewModel.uiState.value)
        coVerify(exactly = 0) { api.getSuggestionInbox(any(), any()) }
    }

    @Test
    fun `switching to the archived tab loads closed suggestions`() {
        coEvery { api.getMySuggestions(SuggestionStatus.PENDING, any()) } returns page(summary(1))
        coEvery { api.getMySuggestions(SuggestionStatus.CLOSED, any()) } returns page(summary(9, SuggestionStatus.CLOSED))

        val viewModel = SuggestionListViewModel(api, isAdmin = false)
        viewModel.load()
        viewModel.selectTab(SuggestionStatus.CLOSED)

        assertEquals(SuggestionStatus.CLOSED, viewModel.tab.value)
        assertEquals(
            SuggestionListUiState.Success(listOf(summary(9, SuggestionStatus.CLOSED))),
            viewModel.uiState.value
        )
    }

    @Test
    fun `list network failure is an Error`() {
        coEvery { api.getMySuggestions(any(), any()) } throws IOException("offline")

        val viewModel = SuggestionListViewModel(api, isAdmin = false)
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
    fun `sending trims the fields and passes the attachment`() {
        val title = slot<RequestBody>()
        val description = slot<RequestBody>()
        val file = MultipartBody.Part.createFormData("file", "notes.pdf", "%PDF".toByteArray().toRequestBody())
        coEvery { api.createSuggestion(capture(title), capture(description), file) } returns Response.success(detail())

        val viewModel = NewSuggestionViewModel(api)
        viewModel.submit("  Add ECG atlas ", " Please add more ECGs ", file)

        assertEquals(NewSuggestionUiState.Sent(ID), viewModel.uiState.value)
        assertEquals("Add ECG atlas", title.captured.text())
        assertEquals("Please add more ECGs", description.captured.text())
    }

    @Test
    fun `oversized attachment rejected by the backend gets a clear message`() {
        coEvery { api.createSuggestion(any(), any(), any()) } returns error(413)

        val viewModel = NewSuggestionViewModel(api)
        viewModel.submit("Title", "Description", null)

        assertEquals(NewSuggestionUiState.Error("Files must be 10 MB or smaller."), viewModel.uiState.value)
        viewModel.onErrorShown()
        assertEquals(NewSuggestionUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `unsupported file type gets a clear message`() {
        coEvery { api.createSuggestion(any(), any(), any()) } returns error(415)

        val viewModel = NewSuggestionViewModel(api)
        viewModel.submit("Title", "Description", null)

        assertTrue((viewModel.uiState.value as NewSuggestionUiState.Error).message.contains("file type"))
    }

    // --- detail ------------------------------------------------------------------------------------

    private fun loadedDetail(state: SuggestionDetail = detail()): SuggestionDetailViewModel {
        coEvery { api.getSuggestion(ID) } returns Response.success(state)
        return SuggestionDetailViewModel(api).apply { load(ID) }
    }

    private fun SuggestionDetailViewModel.success() = uiState.value as SuggestionDetailUiState.Success

    @Test
    fun `detail loads the conversation`() {
        val viewModel = loadedDetail(detail(messages = listOf(message(1))))

        assertEquals(listOf(message(1)), viewModel.success().suggestion.messages)
        assertTrue(viewModel.success().canPost)
    }

    @Test
    fun `detail failure is an Error`() {
        coEvery { api.getSuggestion(ID) } returns error(404)

        val viewModel = SuggestionDetailViewModel(api)
        viewModel.load(ID)

        assertEquals(SuggestionDetailUiState.Error("This suggestion no longer exists."), viewModel.uiState.value)
    }

    @Test
    fun `sent reply is appended and the composer is told to clear`() {
        val viewModel = loadedDetail()
        coEvery { api.postSuggestionMessage(ID, any(), null) } returns Response.success(message(5, "Any update?"))
        var cleared = false

        viewModel.sendMessage("Any update?", null) { cleared = true }

        assertEquals(listOf(message(5, "Any update?")), viewModel.success().suggestion.messages)
        assertFalse(viewModel.success().busy)
        assertTrue(cleared)
    }

    @Test
    fun `blank reply makes no request`() {
        val viewModel = loadedDetail()

        viewModel.sendMessage("   ", null)

        coVerify(exactly = 0) { api.postSuggestionMessage(any(), any(), any()) }
    }

    @Test
    fun `closed suggestion never sends replies`() {
        val viewModel = loadedDetail(detail(status = SuggestionStatus.CLOSED))

        assertFalse(viewModel.success().canPost)
        viewModel.sendMessage("Hello?", null)

        coVerify(exactly = 0) { api.postSuggestionMessage(any(), any(), any()) }
    }

    @Test
    fun `reply rejected because it was closed meanwhile reloads into the archived state`() {
        coEvery { api.getSuggestion(ID) } returnsMany listOf(
            Response.success(detail()),
            Response.success(detail(status = SuggestionStatus.CLOSED))
        )
        coEvery { api.postSuggestionMessage(ID, any(), any()) } returns error(409)
        val viewModel = SuggestionDetailViewModel(api).apply { load(ID) }
        var cleared = false

        viewModel.sendMessage("Hello?", null) { cleared = true }

        assertFalse(viewModel.success().canPost)
        assertTrue(viewModel.message.value!!.contains("closed"))
        assertFalse(cleared)
    }

    @Test
    fun `network failure while replying keeps the conversation and reports it`() {
        val viewModel = loadedDetail(detail(messages = listOf(message(1))))
        coEvery { api.postSuggestionMessage(ID, any(), any()) } throws IOException("offline")

        viewModel.sendMessage("Hello?", null)

        assertEquals(listOf(message(1)), viewModel.success().suggestion.messages)
        assertFalse(viewModel.success().busy)
        assertTrue(viewModel.message.value!!.contains("Network"))
        viewModel.onMessageShown()
        assertNull(viewModel.message.value)
    }

    @Test
    fun `closing archives the suggestion`() {
        val viewModel = loadedDetail()
        coEvery { api.closeSuggestion(ID) } returns Response.success(detail(status = SuggestionStatus.CLOSED))

        viewModel.close()

        assertEquals(SuggestionStatus.CLOSED, viewModel.success().suggestion.status)
        assertFalse(viewModel.success().canPost)
    }

    @Test
    fun `failed close keeps the suggestion open and reports it`() {
        val viewModel = loadedDetail()
        coEvery { api.closeSuggestion(ID) } returns error(500)

        viewModel.close()

        assertTrue(viewModel.success().canPost)
        assertTrue(viewModel.message.value!!.contains("HTTP 500"))
    }

    private companion object {
        const val ID = 10L
    }
}
