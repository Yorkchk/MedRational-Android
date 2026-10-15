package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.TestStrings.en
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.StudyFile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
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

class ShareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val api: MedRationalApi = mockk()
    private val shareDir by lazy { File(tempFolder.root, "shared") }

    private fun studyFile(
        id: Long = FILE_ID,
        fileName: String = "notes.docx",
        fileType: String? = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ) = StudyFile(
        id = id,
        reasoningId = 7,
        fileName = fileName,
        fileType = fileType,
        publicUrl = "https://cdn.example/$fileName",
        fileSizeBytes = 2048
    )

    private fun newViewModel() = ShareViewModel(api, shareDir, mainDispatcherRule.dispatcher)

    private fun body(text: String) = Response.success(text.toByteArray().toResponseBody(null))

    private fun errorResponse(code: Int) = Response.error<ResponseBody>(code, "".toResponseBody(null))

    private fun ShareViewModel.ready() = state.value as ShareState.Ready

    private fun ShareViewModel.error() = state.value as ShareState.Error

    private fun noPartFiles() = shareDir.walk().none { it.name.endsWith(".part") }

    @Test
    fun `sharing a file fetches it under its real name`() {
        coEvery { api.downloadStudyFile(FILE_ID) } returns body("original docx")
        val viewModel = newViewModel()

        viewModel.shareFiles(listOf(studyFile(fileName = "Cardio: ECG.docx")))

        val item = viewModel.ready().items.single()
        assertEquals("Cardio_ ECG.docx", item.file.name)
        assertEquals("original docx", item.file.readText())
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", item.mimeType)
        assertEquals(0, viewModel.ready().failedCount)
        assertTrue(noPartFiles())
    }

    @Test
    fun `sharing the same file again reuses the cached copy`() {
        coEvery { api.downloadStudyFile(FILE_ID) } returns body("bytes")
        val viewModel = newViewModel()

        viewModel.shareFiles(listOf(studyFile()))
        viewModel.onShareHandled()
        viewModel.shareFiles(listOf(studyFile()))

        assertTrue(viewModel.state.value is ShareState.Ready)
        coVerify(exactly = 1) { api.downloadStudyFile(FILE_ID) }
    }

    @Test
    fun `a selection is shared as separate files in order`() {
        coEvery { api.downloadStudyFile(1) } returns body("one")
        coEvery { api.downloadStudyFile(2) } returns body("two")
        coEvery { api.downloadStudyFile(3) } returns body("three")
        val viewModel = newViewModel()

        viewModel.shareFiles(
            listOf(
                studyFile(id = 1, fileName = "a.pdf", fileType = null),
                studyFile(id = 2, fileName = "b.png", fileType = null),
                studyFile(id = 3, fileName = "c.pdf", fileType = null)
            )
        )

        val items = viewModel.ready().items
        assertEquals(listOf("a.pdf", "b.png", "c.pdf"), items.map { it.file.name })
        assertEquals(listOf("one", "two", "three"), items.map { it.file.readText() })
        assertEquals(listOf("application/pdf", "image/png", "application/pdf"), items.map { it.mimeType })
        assertTrue(noPartFiles())
    }

    @Test
    fun `files that fail are left out and counted`() {
        coEvery { api.downloadStudyFile(1) } returns body("one")
        coEvery { api.downloadStudyFile(2) } returns errorResponse(404)
        coEvery { api.downloadStudyFile(3) } throws IOException("offline")
        val viewModel = newViewModel()

        viewModel.shareFiles(listOf(studyFile(id = 1, fileName = "a.pdf"), studyFile(id = 2), studyFile(id = 3)))

        assertEquals(listOf("a.pdf"), viewModel.ready().items.map { it.file.name })
        assertEquals(2, viewModel.ready().failedCount)
    }

    @Test
    fun `a refused share shows the app's own message`() {
        coEvery { api.downloadStudyFile(any()) } returns errorResponse(500)
        val viewModel = newViewModel()

        viewModel.shareFiles(listOf(studyFile(id = 1), studyFile(id = 2)))

        assertEquals("Couldn't share this file.", en(viewModel.error().message))
    }

    @Test
    fun `sharing offline shows the network error`() {
        coEvery { api.downloadStudyFile(FILE_ID) } throws IOException("offline")
        val viewModel = newViewModel()

        viewModel.shareFiles(listOf(studyFile()))

        assertEquals("Network error. Check your connection and try again.", en(viewModel.error().message))
    }

    @Test
    fun `a new share drops earlier files but keeps the ones it reuses`() {
        coEvery { api.downloadStudyFile(any()) } coAnswers { body("bytes") }
        val viewModel = newViewModel()
        viewModel.shareFiles(listOf(studyFile(id = 1, fileName = "a.pdf"), studyFile(id = 2, fileName = "b.pdf")))
        val (first, second) = viewModel.ready().items.map { it.file }
        viewModel.onShareHandled()

        viewModel.shareFiles(listOf(studyFile(id = 2, fileName = "b.pdf"), studyFile(id = 3, fileName = "c.pdf")))

        assertFalse(first.exists())
        assertFalse(first.parentFile!!.exists())
        assertTrue(second.exists())
        coVerify(exactly = 1) { api.downloadStudyFile(2) }
    }

    @Test
    fun `a reasoning is shared as one zip`() {
        coEvery { api.downloadReasoningZip(REASONING_ID) } returns body("zip bytes")
        val viewModel = newViewModel()

        viewModel.shareReasoning(REASONING_ID, "Douleur thoracique_Fichiers.zip")

        val item = viewModel.ready().items.single()
        assertEquals(File(File(shareDir, "zips"), "Douleur thoracique_Fichiers.zip"), item.file)
        assertEquals("zip bytes", item.file.readText())
        assertEquals("application/zip", item.mimeType)
        assertTrue(noPartFiles())
    }

    @Test
    fun `a category is shared as one zip, fetched again every time`() {
        coEvery { api.downloadCategoryZip(CATEGORY_ID) } returns body("first") andThen body("second")
        val viewModel = newViewModel()

        viewModel.shareCategory(CATEGORY_ID, "Cardiologie_Documents.zip")
        viewModel.onShareHandled()
        viewModel.shareCategory(CATEGORY_ID, "Cardiologie_Documents.zip")

        assertEquals("second", viewModel.ready().items.single().file.readText())
        coVerify(exactly = 2) { api.downloadCategoryZip(CATEGORY_ID) }
    }

    @Test
    fun `a failed zip shows the share error`() {
        coEvery { api.downloadReasoningZip(REASONING_ID) } returns errorResponse(500)
        val viewModel = newViewModel()

        viewModel.shareReasoning(REASONING_ID, "Douleur_Fichiers.zip")

        assertEquals("Couldn't share this file.", en(viewModel.error().message))
    }

    @Test
    fun `a share in progress ignores new requests`() {
        coEvery { api.downloadCategoryZip(CATEGORY_ID) } coAnswers { awaitCancellation() }
        val viewModel = newViewModel()

        viewModel.shareCategory(CATEGORY_ID, "Cardiologie_Documents.zip")
        viewModel.shareReasoning(REASONING_ID, "Douleur_Fichiers.zip")

        assertEquals(ShareState.Preparing(0, 0), viewModel.state.value)
        coVerify(exactly = 0) { api.downloadReasoningZip(any()) }
        viewModel.cancel()
    }

    @Test
    fun `cancelling stops the share`() {
        coEvery { api.downloadCategoryZip(CATEGORY_ID) } coAnswers { awaitCancellation() }
        val viewModel = newViewModel()
        viewModel.shareCategory(CATEGORY_ID, "Cardiologie_Documents.zip")

        viewModel.cancel()

        assertEquals(ShareState.Idle, viewModel.state.value)
    }

    @Test
    fun `handling the share resets the state`() {
        coEvery { api.downloadStudyFile(FILE_ID) } returns body("bytes")
        val viewModel = newViewModel()

        viewModel.shareFiles(listOf(studyFile()))
        viewModel.onShareHandled()

        assertEquals(ShareState.Idle, viewModel.state.value)
    }

    private companion object {
        const val FILE_ID = 42L
        const val REASONING_ID = 7L
        const val CATEGORY_ID = 3L
    }
}
