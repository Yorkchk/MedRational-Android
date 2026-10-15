package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.R
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.data.model.isImageFile
import com.example.medrational_android.data.preview.saveToCache
import com.example.medrational_android.ui.text.UiText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.io.IOException

sealed interface FileDetailUiState {
    data object Loading : FileDetailUiState
    data class Success(
        val file: StudyFile,
        val reasoning: Reasoning,
        val preview: PreviewState
    ) : FileDetailUiState
    data class Error(val message: UiText) : FileDetailUiState
}

sealed interface PreviewState {
    data object Loading : PreviewState
    data class Pdf(val file: File) : PreviewState
    data class Image(val url: String) : PreviewState
    data object NotSupported : PreviewState
    data class Unavailable(val message: UiText) : PreviewState
}

sealed interface ShareState {
    data object Idle : ShareState
    data object Preparing : ShareState
    data class Ready(val file: File, val mimeType: String) : ShareState
    data class Error(val message: UiText) : ShareState
}

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
        "zip" -> "application/zip"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }
}

class FileDetailViewModel(
    private val api: MedRationalApi,
    // Converted PDFs are cached here as {fileId}.pdf so reopening a file is instant
    private val previewDir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    // Signed-in students only: the endpoint needs a user, and admins aren't counted
    private val recordViews: Boolean = false,
    // Files handed to other apps (Gmail, Drive…) through the FileProvider; only the latest one is kept
    private val shareDir: File = File(previewDir.parentFile ?: previewDir, "shared")
) : ViewModel() {

    private val _uiState = MutableStateFlow<FileDetailUiState>(FileDetailUiState.Loading)
    val uiState: StateFlow<FileDetailUiState> = _uiState.asStateFlow()

    private val _shareState = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState.asStateFlow()

    private var previewJob: Job? = null
    // Coming back to the screen or retrying reloads the file but isn't a new view
    private val recordedViews = HashSet<Long>()

    fun load(reasoningId: Long, fileId: Long) {
        _uiState.value = FileDetailUiState.Loading
        viewModelScope.launch {
            try {
                val response = api.getReasoning(reasoningId)
                val reasoning = response.body()
                if (!response.isSuccessful || reasoning == null) {
                    _uiState.value = FileDetailUiState.Error(UiText.Res(R.string.file_error_load, listOf(response.code())))
                    return@launch
                }
                val file = reasoning.files.orEmpty().firstOrNull { it.id == fileId }
                if (file == null) {
                    _uiState.value = FileDetailUiState.Error(UiText.Res(R.string.file_error_gone))
                    return@launch
                }
                _uiState.value = FileDetailUiState.Success(file, reasoning, initialPreview(file))
                recordView(file.id)
                if (file.previewable && !isImageFile(file.fileName, file.fileType)) {
                    loadPdfPreview(file.id)
                }
            } catch (e: IOException) {
                _uiState.value = FileDetailUiState.Error(UiText.Res(R.string.error_network))
            } catch (e: HttpException) {
                _uiState.value = FileDetailUiState.Error(UiText.Res(R.string.file_error_load, listOf(e.code())))
            }
        }
    }

    fun retryPreview() {
        val state = _uiState.value as? FileDetailUiState.Success ?: return
        if (state.preview !is PreviewState.Unavailable) return
        setPreview(PreviewState.Loading)
        loadPdfPreview(state.file.id)
    }

    // The cached PDF couldn't be opened (e.g. truncated); drop it so a retry downloads it again
    fun onPreviewCorrupted() {
        val state = _uiState.value as? FileDetailUiState.Success ?: return
        val preview = state.preview as? PreviewState.Pdf ?: return
        preview.file.delete()
        setPreview(PreviewState.Unavailable(UiText.Res(R.string.preview_error_corrupted)))
    }

    /**
     * Fetches the original file so the screen can hand it to the share sheet. It goes through the download
     * endpoint, so the backend lists it in Recent downloads like any download.
     */
    fun share(file: StudyFile) {
        if (_shareState.value == ShareState.Preparing) return
        _shareState.value = ShareState.Preparing
        viewModelScope.launch {
            val name = shareFileName(file.fileName, file.id)
            val target = File(File(shareDir, file.id.toString()), name)
            _shareState.value = try {
                if (target.exists() && target.length() > 0) {
                    ShareState.Ready(target, shareMimeType(file.fileType, name))
                } else {
                    val response = api.downloadStudyFile(file.id)
                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        // Older shares aren't deleted right after sharing: the receiving app reads them later
                        withContext(ioDispatcher) { shareDir.deleteRecursively() }
                        ShareState.Ready(saveToCache(body, target, ioDispatcher), shareMimeType(file.fileType, name))
                    } else {
                        ShareState.Error(UiText.Res(R.string.share_failed))
                    }
                }
            } catch (e: IOException) {
                ShareState.Error(UiText.Res(R.string.error_network))
            } catch (e: HttpException) {
                ShareState.Error(UiText.Res(R.string.share_failed))
            }
        }
    }

    // The screen opened the share sheet or showed the error
    fun onShareHandled() {
        _shareState.value = ShareState.Idle
    }

    // For the admin statistics; failures don't matter to the user, so they're ignored
    private fun recordView(fileId: Long) {
        if (!recordViews || !recordedViews.add(fileId)) return
        viewModelScope.launch {
            try {
                api.recordFileView(fileId)
            } catch (e: IOException) {
                // Not retried: a missed view only makes the statistics a little low
            } catch (e: HttpException) {
                // Not retried: a missed view only makes the statistics a little low
            }
        }
    }

    private fun initialPreview(file: StudyFile): PreviewState = when {
        isImageFile(file.fileName, file.fileType) -> PreviewState.Image(ApiClient.filePreviewUrl(file.id))
        file.previewable -> PreviewState.Loading
        else -> PreviewState.NotSupported
    }

    private fun loadPdfPreview(fileId: Long) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val cached = File(previewDir, "$fileId.pdf")
            if (cached.exists() && cached.length() > 0) {
                setPreview(PreviewState.Pdf(cached))
                return@launch
            }
            val preview = try {
                val response = api.getFilePreview(fileId)
                val body = response.body()
                when {
                    response.isSuccessful && body != null -> PreviewState.Pdf(saveToCache(body, cached, ioDispatcher))
                    response.code() == 415 -> PreviewState.NotSupported
                    response.code() == 503 -> PreviewState.Unavailable(UiText.Res(R.string.preview_error_busy))
                    else -> PreviewState.Unavailable(UiText.Res(R.string.preview_error_http, listOf(response.code())))
                }
            } catch (e: IOException) {
                PreviewState.Unavailable(UiText.Res(R.string.preview_error_network))
            } catch (e: HttpException) {
                PreviewState.Unavailable(UiText.Res(R.string.preview_error_http, listOf(e.code())))
            }
            setPreview(preview)
        }
    }

    private fun setPreview(preview: PreviewState) {
        _uiState.update { state ->
            if (state is FileDetailUiState.Success) state.copy(preview = preview) else state
        }
    }
}
