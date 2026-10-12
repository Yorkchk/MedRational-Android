package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.WorkshopAttachment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.File
import java.io.IOException

class WorkshopAttachmentPreviewsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val api: MedRationalApi = mockk()
    private val pdfBytes = "%PDF-1.7 fake".toByteArray()

    private fun attachment(id: Long = ID, fileName: String = "program.pdf", previewable: Boolean = true) =
        WorkshopAttachment(
            id = id,
            fileName = fileName,
            fileUrl = "https://account.r2.cloudflarestorage.com/bucket/WorkshopAttachments/Project_1/$fileName",
            previewable = previewable
        )

    private fun newViewModel(previewDir: File = tempFolder.newFolder("previews")) =
        WorkshopAttachmentPreviewsViewModel(api, previewDir, mainDispatcherRule.dispatcher)

    private fun pdfResponse() = Response.success(pdfBytes.toResponseBody(null))

    private fun errorResponse(code: Int) = Response.error<ResponseBody>(code, "".toResponseBody(null))

    @Test
    fun `a pdf is downloaded through the backend and cached`() {
        val previewDir = tempFolder.newFolder("previews")
        coEvery { api.getWorkshopAttachmentPreview(ID) } returns pdfResponse()

        val viewModel = newViewModel(previewDir)
        viewModel.load(listOf(attachment()))

        val preview = viewModel.previews.value[ID] as PreviewState.Pdf
        assertEquals(File(previewDir, "workshop-$ID.pdf"), preview.file)
        assertTrue(preview.file.readBytes().contentEquals(pdfBytes))
        assertFalse(File(previewDir, "workshop-$ID.pdf.part").exists())
    }

    @Test
    fun `a cached preview is reused without a request`() {
        val previewDir = tempFolder.newFolder("previews")
        File(previewDir, "workshop-$ID.pdf").writeBytes(pdfBytes)

        val viewModel = newViewModel(previewDir)
        viewModel.load(listOf(attachment(fileName = "schedule.docx")))

        assertTrue(viewModel.previews.value[ID] is PreviewState.Pdf)
        coVerify(exactly = 0) { api.getWorkshopAttachmentPreview(any()) }
    }

    @Test
    fun `an image is shown from the backend, not from its storage url`() {
        val viewModel = newViewModel()
        viewModel.load(listOf(attachment(fileName = "room.png")))

        assertEquals(
            PreviewState.Image(ApiClient.workshopAttachmentPreviewUrl(ID)),
            viewModel.previews.value[ID]
        )
        coVerify(exactly = 0) { api.getWorkshopAttachmentPreview(any()) }
    }

    @Test
    fun `a file that can't be previewed makes no request`() {
        val viewModel = newViewModel()
        viewModel.load(listOf(attachment(fileName = "bundle.zip", previewable = false)))

        assertEquals(PreviewState.NotSupported, viewModel.previews.value[ID])
        coVerify(exactly = 0) { api.getWorkshopAttachmentPreview(any()) }
    }

    @Test
    fun `a format the backend refuses is not supported`() {
        coEvery { api.getWorkshopAttachmentPreview(ID) } returns errorResponse(415)

        val viewModel = newViewModel()
        viewModel.load(listOf(attachment()))

        assertEquals(PreviewState.NotSupported, viewModel.previews.value[ID])
    }

    @Test
    fun `a failed conversion is unavailable`() {
        coEvery { api.getWorkshopAttachmentPreview(ID) } returns errorResponse(503)

        val viewModel = newViewModel()
        viewModel.load(listOf(attachment()))

        assertEquals(PreviewState.Unavailable("The preview can't be generated right now."), viewModel.previews.value[ID])
    }

    @Test
    fun `retry after a network error loads the preview`() {
        coEvery { api.getWorkshopAttachmentPreview(ID) } throws IOException("offline") andThen pdfResponse()

        val viewModel = newViewModel()
        viewModel.load(listOf(attachment()))
        assertTrue(viewModel.previews.value[ID] is PreviewState.Unavailable)

        viewModel.retry(ID)

        assertTrue(viewModel.previews.value[ID] is PreviewState.Pdf)
    }

    @Test
    fun `loading the same attachments again doesn't refetch them`() {
        coEvery { api.getWorkshopAttachmentPreview(ID) } returns pdfResponse()

        val viewModel = newViewModel()
        viewModel.load(listOf(attachment()))
        viewModel.load(listOf(attachment()))

        coVerify(exactly = 1) { api.getWorkshopAttachmentPreview(ID) }
    }

    @Test
    fun `a removed attachment's preview is dropped`() {
        coEvery { api.getWorkshopAttachmentPreview(any()) } returns pdfResponse()

        val viewModel = newViewModel()
        viewModel.load(listOf(attachment(1), attachment(2)))
        viewModel.load(listOf(attachment(2)))

        assertEquals(setOf(2L), viewModel.previews.value.keys)
    }

    @Test
    fun `a corrupted cached preview is deleted and can be retried`() {
        coEvery { api.getWorkshopAttachmentPreview(ID) } returns pdfResponse()
        val viewModel = newViewModel()
        viewModel.load(listOf(attachment()))
        val cached = (viewModel.previews.value[ID] as PreviewState.Pdf).file

        viewModel.onPreviewCorrupted(ID)

        assertFalse(cached.exists())
        assertTrue(viewModel.previews.value[ID] is PreviewState.Unavailable)
    }

    private companion object {
        const val ID = 4L
    }
}
