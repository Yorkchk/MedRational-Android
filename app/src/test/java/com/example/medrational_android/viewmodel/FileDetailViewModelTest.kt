package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.TestStrings.en
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.model.StudyFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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

class FileDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val api: MedRationalApi = mockk()
    private val pdfBytes = "%PDF-1.7 fake".toByteArray()

    private fun studyFile(
        id: Long = FILE_ID,
        fileName: String = "notes.docx",
        fileType: String? = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        previewable: Boolean = true
    ) = StudyFile(
        id = id,
        reasoningId = REASONING_ID,
        fileName = fileName,
        fileType = fileType,
        publicUrl = "https://cdn.example/$fileName",
        fileSizeBytes = 2048,
        previewable = previewable
    )

    private fun reasoningWith(vararg files: StudyFile) = Reasoning(
        id = REASONING_ID,
        categoryId = 1,
        title = "Chest pain",
        content = "Rule out ACS first",
        files = files.toList()
    )

    private fun newViewModel(
        previewDir: File = tempFolder.newFolder("previews"),
        recordViews: Boolean = false,
        shareDir: File = File(tempFolder.root, "shared")
    ) = FileDetailViewModel(api, previewDir, mainDispatcherRule.dispatcher, recordViews, shareDir)

    private fun pdfResponse() = Response.success(pdfBytes.toResponseBody(null))

    private fun errorResponse(code: Int) = Response.error<okhttp3.ResponseBody>(code, "".toResponseBody(null))

    private fun FileDetailViewModel.success() = uiState.value as FileDetailUiState.Success

    @Test
    fun `previewable file downloads the PDF and caches it`() {
        val previewDir = tempFolder.newFolder("previews")
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith(studyFile()))
        coEvery { api.getFilePreview(FILE_ID) } returns pdfResponse()

        val viewModel = newViewModel(previewDir)
        viewModel.load(REASONING_ID, FILE_ID)

        val state = viewModel.success()
        assertEquals("notes.docx", state.file.fileName)
        assertEquals("Chest pain", state.reasoning.title)
        val preview = state.preview as PreviewState.Pdf
        assertEquals(File(previewDir, "$FILE_ID.pdf"), preview.file)
        assertTrue(preview.file.readBytes().contentEquals(pdfBytes))
        assertFalse(File(previewDir, "$FILE_ID.pdf.part").exists())
    }

    @Test
    fun `cached PDF is reused without calling the preview endpoint`() {
        val previewDir = tempFolder.newFolder("previews")
        File(previewDir, "$FILE_ID.pdf").writeBytes(pdfBytes)
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith(studyFile()))

        val viewModel = newViewModel(previewDir)
        viewModel.load(REASONING_ID, FILE_ID)

        assertTrue(viewModel.success().preview is PreviewState.Pdf)
        coVerify(exactly = 0) { api.getFilePreview(any()) }
    }

    @Test
    fun `non previewable file is NotSupported and never requests a preview`() {
        val zip = studyFile(fileName = "bundle.zip", fileType = "application/zip", previewable = false)
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith(zip))

        val viewModel = newViewModel()
        viewModel.load(REASONING_ID, FILE_ID)

        assertEquals(PreviewState.NotSupported, viewModel.success().preview)
        coVerify(exactly = 0) { api.getFilePreview(any()) }
    }

    @Test
    fun `image file is previewed through the backend, not its private storage URL`() {
        val image = studyFile(fileName = "ecg.jpg", fileType = "image/jpeg", previewable = true)
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith(image))

        val viewModel = newViewModel()
        viewModel.load(REASONING_ID, FILE_ID)

        val preview = viewModel.success().preview as PreviewState.Image
        assertTrue(preview.url.endsWith("api/v1/files/$FILE_ID/preview"))
        assertFalse(preview.url.contains("cdn.example"))
        // Coil loads the image itself; the PDF preview path must not run
        coVerify(exactly = 0) { api.getFilePreview(any()) }
    }

    @Test
    fun `415 from the backend means NotSupported`() {
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith(studyFile()))
        coEvery { api.getFilePreview(FILE_ID) } returns errorResponse(415)

        val viewModel = newViewModel()
        viewModel.load(REASONING_ID, FILE_ID)

        assertEquals(PreviewState.NotSupported, viewModel.success().preview)
    }

    @Test
    fun `503 is Unavailable and retry recovers`() {
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith(studyFile()))
        coEvery { api.getFilePreview(FILE_ID) } returnsMany listOf(errorResponse(503), pdfResponse())

        val viewModel = newViewModel()
        viewModel.load(REASONING_ID, FILE_ID)
        assertTrue(viewModel.success().preview is PreviewState.Unavailable)

        viewModel.retryPreview()
        assertTrue(viewModel.success().preview is PreviewState.Pdf)
    }

    @Test
    fun `network failure while loading the preview is Unavailable`() {
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith(studyFile()))
        coEvery { api.getFilePreview(FILE_ID) } throws IOException("timeout")

        val viewModel = newViewModel()
        viewModel.load(REASONING_ID, FILE_ID)

        assertTrue(viewModel.success().preview is PreviewState.Unavailable)
    }

    @Test
    fun `corrupted cached PDF is deleted and becomes Unavailable`() {
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith(studyFile()))
        coEvery { api.getFilePreview(FILE_ID) } returns pdfResponse()

        val viewModel = newViewModel()
        viewModel.load(REASONING_ID, FILE_ID)
        val cached = (viewModel.success().preview as PreviewState.Pdf).file

        viewModel.onPreviewCorrupted()

        assertFalse(cached.exists())
        assertTrue(viewModel.success().preview is PreviewState.Unavailable)
    }

    @Test
    fun `file missing from the reasoning is an Error`() {
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith(studyFile(id = 99)))

        val viewModel = newViewModel()
        viewModel.load(REASONING_ID, FILE_ID)

        assertEquals("This file no longer exists.", en((viewModel.uiState.value as FileDetailUiState.Error).message))
    }

    @Test
    fun `failed reasoning request is an Error`() {
        coEvery { api.getReasoning(REASONING_ID) } returns Response.error(404, "".toResponseBody(null))

        val viewModel = newViewModel()
        viewModel.load(REASONING_ID, FILE_ID)

        assertTrue(viewModel.uiState.value is FileDetailUiState.Error)
    }

    @Test
    fun `network failure loading the reasoning is an Error`() {
        coEvery { api.getReasoning(REASONING_ID) } throws IOException("offline")

        val viewModel = newViewModel()
        viewModel.load(REASONING_ID, FILE_ID)

        assertTrue(viewModel.uiState.value is FileDetailUiState.Error)
    }

    @Test
    fun `opening a file records one view however often it reloads`() {
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith(studyFile(previewable = false)))
        coEvery { api.recordFileView(FILE_ID) } returns Response.success(Unit)
        val viewModel = newViewModel(recordViews = true)

        viewModel.load(REASONING_ID, FILE_ID)
        viewModel.load(REASONING_ID, FILE_ID)

        coVerify(exactly = 1) { api.recordFileView(FILE_ID) }
    }

    @Test
    fun `views are not recorded for signed-out users and admins`() {
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith(studyFile(previewable = false)))

        newViewModel(recordViews = false).load(REASONING_ID, FILE_ID)

        coVerify(exactly = 0) { api.recordFileView(any()) }
    }

    @Test
    fun `a failed view never disturbs the file`() {
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith(studyFile(previewable = false)))
        coEvery { api.recordFileView(FILE_ID) } throws IOException("offline")
        val viewModel = newViewModel(recordViews = true)

        viewModel.load(REASONING_ID, FILE_ID)

        assertEquals(PreviewState.NotSupported, viewModel.success().preview)
    }

    @Test
    fun `a missing file records no view`() {
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith())

        newViewModel(recordViews = true).load(REASONING_ID, FILE_ID)

        coVerify(exactly = 0) { api.recordFileView(any()) }
    }

    @Test
    fun `sharing fetches the original file under its real name`() {
        val shareDir = File(tempFolder.root, "shared")
        val docBytes = "original docx".toByteArray()
        coEvery { api.downloadStudyFile(FILE_ID) } returns Response.success(docBytes.toResponseBody(null))
        val viewModel = newViewModel(shareDir = shareDir)

        viewModel.share(studyFile(fileName = "Cardio: ECG.docx"))

        val ready = viewModel.shareState.value as ShareState.Ready
        assertEquals(File(File(shareDir, "$FILE_ID"), "Cardio_ ECG.docx"), ready.file)
        assertTrue(ready.file.readBytes().contentEquals(docBytes))
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ready.mimeType)
        assertFalse(File(ready.file.parentFile, "${ready.file.name}.part").exists())
    }

    @Test
    fun `sharing the same file again reuses the cached copy`() {
        coEvery { api.downloadStudyFile(FILE_ID) } returns Response.success("bytes".toByteArray().toResponseBody(null))
        val viewModel = newViewModel()

        viewModel.share(studyFile())
        viewModel.onShareHandled()
        viewModel.share(studyFile())

        assertTrue(viewModel.shareState.value is ShareState.Ready)
        coVerify(exactly = 1) { api.downloadStudyFile(FILE_ID) }
    }

    @Test
    fun `sharing another file drops the previous shared copy`() {
        coEvery { api.downloadStudyFile(any()) } returns Response.success("bytes".toByteArray().toResponseBody(null))
        val viewModel = newViewModel()

        viewModel.share(studyFile(id = 1, fileName = "a.pdf"))
        val first = (viewModel.shareState.value as ShareState.Ready).file
        viewModel.onShareHandled()
        viewModel.share(studyFile(id = 2, fileName = "b.pdf"))

        assertFalse(first.exists())
        assertTrue((viewModel.shareState.value as ShareState.Ready).file.exists())
    }

    @Test
    fun `a refused share shows the app's own message`() {
        coEvery { api.downloadStudyFile(FILE_ID) } returns errorResponse(404)
        val viewModel = newViewModel()

        viewModel.share(studyFile())

        val error = viewModel.shareState.value as ShareState.Error
        assertEquals("Couldn't share this file.", en(error.message))
    }

    @Test
    fun `sharing offline shows the network error`() {
        coEvery { api.downloadStudyFile(FILE_ID) } throws IOException("offline")
        val viewModel = newViewModel()

        viewModel.share(studyFile())

        val error = viewModel.shareState.value as ShareState.Error
        assertEquals("Network error. Check your connection and try again.", en(error.message))
    }

    @Test
    fun `handling the share resets the state`() {
        coEvery { api.downloadStudyFile(FILE_ID) } returns Response.success("bytes".toByteArray().toResponseBody(null))
        val viewModel = newViewModel()

        viewModel.share(studyFile())
        viewModel.onShareHandled()

        assertEquals(ShareState.Idle, viewModel.shareState.value)
    }

    private companion object {
        const val REASONING_ID = 7L
        const val FILE_ID = 42L
    }
}
