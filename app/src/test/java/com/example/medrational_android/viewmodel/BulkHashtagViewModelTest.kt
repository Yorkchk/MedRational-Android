package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.R
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.AttachHashtagsRequest
import com.example.medrational_android.data.model.Hashtag
import com.example.medrational_android.ui.text.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class BulkHashtagViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()
    private val viewModel by lazy { BulkHashtagViewModel(api) }

    private val cardio = Hashtag(1, "cardio")
    private val ecg = Hashtag(2, "ecg")
    private val exam = Hashtag(3, "exam")

    @Test
    fun `add attaches the tags to every selected file`() {
        coEvery { api.attachHashtags(any(), any()) } returns Response.success(emptyList())

        viewModel.addTags(setOf(10, 11, 12), listOf("exam", "ecg"))

        listOf(10L, 11L, 12L).forEach { fileId ->
            coVerify(exactly = 1) { api.attachHashtags(fileId, AttachHashtagsRequest(listOf("exam", "ecg"))) }
        }
        assertEquals(
            UiText.Plural(R.plurals.bulk_tags_added, 2, listOf(2, tags(2), files(3))),
            viewModel.message.value
        )
        assertEquals(BulkHashtagUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `partial add failure is reported`() {
        coEvery { api.attachHashtags(10, any()) } returns Response.success(emptyList())
        coEvery { api.attachHashtags(11, any()) } throws IOException("offline")
        coEvery { api.attachHashtags(12, any()) } returns Response.error(500, "".toResponseBody(null))

        viewModel.addTags(setOf(10, 11, 12), listOf("exam"))

        assertEquals(UiText.Res(R.string.bulk_tag_failed, listOf(2, files(3))), viewModel.message.value)
    }

    @Test
    fun `loading tags builds the union with per-file ids, most shared first`() {
        coEvery { api.getFileHashtags(10) } returns Response.success(listOf(cardio, exam))
        coEvery { api.getFileHashtags(11) } returns Response.success(listOf(exam))
        coEvery { api.getFileHashtags(12) } returns Response.success(listOf(ecg, exam))

        viewModel.loadTagsFor(setOf(10, 11, 12))

        assertEquals(
            BulkHashtagUiState.RemoveChoices(
                fileCount = 3,
                tags = listOf(
                    BulkTag("exam", mapOf(10L to 3L, 11L to 3L, 12L to 3L)),
                    BulkTag("cardio", mapOf(10L to 1L)),
                    BulkTag("ecg", mapOf(12L to 2L))
                )
            ),
            viewModel.uiState.value
        )
    }

    @Test
    fun `failing to load a file's tags reports it instead of showing partial choices`() {
        coEvery { api.getFileHashtags(10) } returns Response.success(listOf(exam))
        coEvery { api.getFileHashtags(11) } throws IOException("offline")

        viewModel.loadTagsFor(setOf(10, 11))

        assertEquals(BulkHashtagUiState.Idle, viewModel.uiState.value)
        assertEquals(UiText.Res(R.string.bulk_load_failed), viewModel.message.value)
    }

    @Test
    fun `remove detaches the chosen tags only from files that carry them`() {
        coEvery { api.detachHashtag(any(), any()) } returns Response.success(Unit)

        viewModel.removeTags(listOf(BulkTag("exam", mapOf(10L to 3L, 11L to 3L)), BulkTag("ecg", mapOf(12L to 2L))))

        coVerify(exactly = 1) { api.detachHashtag(10, 3) }
        coVerify(exactly = 1) { api.detachHashtag(11, 3) }
        coVerify(exactly = 1) { api.detachHashtag(12, 2) }
        coVerify(exactly = 3) { api.detachHashtag(any(), any()) }
        assertEquals(
            UiText.Plural(R.plurals.bulk_tags_removed, 2, listOf(2, tags(2), files(3))),
            viewModel.message.value
        )
        assertEquals(BulkHashtagUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `partial remove failure is reported`() {
        coEvery { api.detachHashtag(10, 3) } returns Response.success(Unit)
        coEvery { api.detachHashtag(11, 3) } returns Response.error(500, "".toResponseBody(null))

        viewModel.removeTags(listOf(BulkTag("exam", mapOf(10L to 3L, 11L to 3L))))

        assertEquals(UiText.Res(R.string.bulk_remove_failed, listOf(1, tags(2))), viewModel.message.value)
    }

    private fun tags(count: Int) = UiText.Plural(R.plurals.hashtag_count, count)

    private fun files(count: Int) = UiText.Plural(R.plurals.file_count, count)
}
