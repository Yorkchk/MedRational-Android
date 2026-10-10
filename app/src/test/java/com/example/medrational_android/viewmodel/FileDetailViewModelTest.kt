package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
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

    private fun newViewModel(previewDir: File = tempFolder.newFolder("previews")) =
        FileDetailViewModel(api, previewDir, mainDispatcherRule.dispatcher)

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
    fun `image file is previewed from its public URL`() {
        val image = studyFile(fileName = "ecg.png", fileType = "image/png", previewable = false)
        coEvery { api.getReasoning(REASONING_ID) } returns Response.success(reasoningWith(image))

        val viewModel = newViewModel()
        viewModel.load(REASONING_ID, FILE_ID)

        assertEquals(PreviewState.Image("https://cdn.example/ecg.png"), viewModel.success().preview)
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

        assertEquals(FileDetailUiState.Error("This file no longer exists."), viewModel.uiState.value)
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

    private companion object {
        const val REASONING_ID = 7L
        const val FILE_ID = 42L
    }
}
