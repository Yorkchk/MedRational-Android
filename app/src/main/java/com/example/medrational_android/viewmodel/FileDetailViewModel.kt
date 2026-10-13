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

class FileDetailViewModel(
    private val api: MedRationalApi,
    // Converted PDFs are cached here as {fileId}.pdf so reopening a file is instant
    private val previewDir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    // Signed-in students only: the endpoint needs a user, and admins aren't counted
    private val recordViews: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow<FileDetailUiState>(FileDetailUiState.Loading)
    val uiState: StateFlow<FileDetailUiState> = _uiState.asStateFlow()

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
