package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.R
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.data.preview.saveToCache
import com.example.medrational_android.ui.text.UiText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException

/** A file ready to hand to the share sheet. */
data class SharedItem(val file: File, val mimeType: String)

sealed interface ShareState {
    data object Idle : ShareState
    /** [total] is 0 for a zip, whose progress isn't known. */
    data class Preparing(val done: Int, val total: Int) : ShareState
    /** [failedCount] files of a selection couldn't be fetched and are left out. */
    data class Ready(val items: List<SharedItem>, val failedCount: Int = 0) : ShareState
    data class Error(val message: UiText) : ShareState
}

private const val ZIP_MIME_TYPE = "application/zip"
private val UNSAFE_FILE_NAME_CHARS = Regex("""[\\/:*?"<>|\p{Cntrl}]""")

/**
 * The name the shared file carries into Gmail, Drive…: the real one, accents and spaces kept, with only
 * the characters a file system rejects replaced.
 */
internal fun shareFileName(fileName: String?, fileId: Long): String {
    val cleaned = fileName.orEmpty().replace(UNSAFE_FILE_NAME_CHARS, "_").trim().trimStart('.')
    return cleaned.ifBlank { "file_$fileId" }
}

/** The backend's content type, else a guess from the extension, so receiving apps know what they get. */
internal fun shareMimeType(fileType: String?, fileName: String): String {
    if (!fileType.isNullOrBlank() && fileType.contains('/')) return fileType
    return when (fileName.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "zip" -> ZIP_MIME_TYPE
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }
}

/** One type for a share of several files: theirs if they agree, else the widest one that covers them all. */
internal fun commonMimeType(types: List<String>): String {
    val distinct = types.distinct()
    if (distinct.size == 1) return distinct.single()
    val topLevels = distinct.map { it.substringBefore('/') }.distinct()
    return if (topLevels.size == 1) "${topLevels.single()}/*" else "*/*"
}

/**
 * Fetches files for the share sheet: one file or a selection (shared as separate files), or a whole
 * reasoning or category (shared as one zip). Files go into [shareDir], the only folder the FileProvider
 * exposes (res/xml/file_paths.xml).
 */
class ShareViewModel(
    private val api: MedRationalApi,
    private val shareDir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow<ShareState>(ShareState.Idle)
    val state: StateFlow<ShareState> = _state.asStateFlow()

    private var job: Job? = null

    /**
     * Shares [files] as separate files. They go through the download endpoint, so the backend lists them
     * in Recent downloads like any download. A file that fails is left out; the share fails only when
     * none could be fetched.
     */
    fun shareFiles(files: List<StudyFile>) {
        if (files.isEmpty() || !start(ShareState.Preparing(0, files.size))) return
        job = viewModelScope.launch {
            val targets = files.map { file ->
                File(File(File(shareDir, FILES_DIR), file.id.toString()), shareFileName(file.fileName, file.id))
            }
            removeOtherShares(keep = targets.toSet())
            val items = mutableListOf<SharedItem>()
            var lastError: UiText = UiText.Res(R.string.share_failed)
            files.zip(targets).forEachIndexed { index, (file, target) ->
                try {
                    // A file's content never changes for a given id, so an earlier copy is reused
                    val saved = if (target.exists() && target.length() > 0) {
                        target
                    } else {
                        save(api.downloadStudyFile(file.id), target)
                    }
                    if (saved != null) {
                        items += SharedItem(saved, shareMimeType(file.fileType, saved.name))
                    }
                } catch (e: IOException) {
                    lastError = UiText.Res(R.string.error_network)
                } catch (e: HttpException) {
                    lastError = UiText.Res(R.string.share_failed)
                }
                _state.value = ShareState.Preparing(index + 1, files.size)
            }
            _state.value = if (items.isEmpty()) {
                ShareState.Error(lastError)
            } else {
                ShareState.Ready(items, failedCount = files.size - items.size)
            }
        }
    }

    /** Shares the reasoning as one zip of its files. [zipName] is what the student sees, e.g. "Douleur_Fichiers.zip". */
    fun shareReasoning(reasoningId: Long, zipName: String) {
        shareZip(zipName, reasoningId) { api.downloadReasoningZip(reasoningId) }
    }

    /** Shares the category as one zip with a folder per reasoning. */
    fun shareCategory(categoryId: Long, zipName: String) {
        shareZip(zipName, categoryId) { api.downloadCategoryZip(categoryId) }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = ShareState.Idle
    }

    // The screen opened the share sheet or showed the error
    fun onShareHandled() {
        _state.value = ShareState.Idle
    }

    private fun shareZip(zipName: String, id: Long, fetch: suspend () -> Response<ResponseBody>) {
        if (!start(ShareState.Preparing(0, 0))) return
        job = viewModelScope.launch {
            val target = File(File(shareDir, ZIPS_DIR), shareFileName(zipName, id))
            // Always fetched again: the zip changes whenever a file is added
            removeOtherShares(keep = emptySet())
            _state.value = try {
                val saved = save(fetch(), target)
                if (saved != null) {
                    ShareState.Ready(listOf(SharedItem(saved, ZIP_MIME_TYPE)))
                } else {
                    ShareState.Error(UiText.Res(R.string.share_failed))
                }
            } catch (e: IOException) {
                ShareState.Error(UiText.Res(R.string.error_network))
            } catch (e: HttpException) {
                ShareState.Error(UiText.Res(R.string.share_failed))
            }
        }
    }

    private fun start(preparing: ShareState.Preparing): Boolean {
        if (_state.value is ShareState.Preparing) return false
        _state.value = preparing
        return true
    }

    private suspend fun save(response: Response<ResponseBody>, target: File): File? {
        val body = response.body()
        return if (response.isSuccessful && body != null) saveToCache(body, target, ioDispatcher) else null
    }

    // Earlier shares aren't deleted right after sharing, because the receiving app reads them later
    private suspend fun removeOtherShares(keep: Set<File>) = withContext(ioDispatcher) {
        val keptPaths = keep.map { it.absolutePath }
        shareDir.walkBottomUp()
            .filter { it != shareDir }
            .filter { file -> keptPaths.none { it == file.absolutePath || it.startsWith(file.absolutePath + File.separator) } }
            .forEach { it.delete() }
    }

    private companion object {
        const val FILES_DIR = "files"
        const val ZIPS_DIR = "zips"
    }
}
