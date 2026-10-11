package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.data.model.isImageFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
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
    data class Error(val message: String) : FileDetailUiState
}

sealed interface PreviewState {
    data object Loading : PreviewState
    data class Pdf(val file: File) : PreviewState
    data class Image(val url: String) : PreviewState
    data object NotSupported : PreviewState
    data class Unavailable(val message: String) : PreviewState
}

class FileDetailViewModel(
    private val api: MedRationalApi,
    // Converted PDFs are cached here as {fileId}.pdf so reopening a file is instant
    private val previewDir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow<FileDetailUiState>(FileDetailUiState.Loading)
    val uiState: StateFlow<FileDetailUiState> = _uiState.asStateFlow()

    private var previewJob: Job? = null

    fun load(reasoningId: Long, fileId: Long) {
        _uiState.value = FileDetailUiState.Loading
        viewModelScope.launch {
            try {
                val response = api.getReasoning(reasoningId)
                val reasoning = response.body()
                if (!response.isSuccessful || reasoning == null) {
                    _uiState.value = FileDetailUiState.Error("Couldn't load this file (HTTP ${response.code()}).")
                    return@launch
                }
                val file = reasoning.files.orEmpty().firstOrNull { it.id == fileId }
                if (file == null) {
                    _uiState.value = FileDetailUiState.Error("This file no longer exists.")
                    return@launch
                }
                _uiState.value = FileDetailUiState.Success(file, reasoning, initialPreview(file))
                if (file.previewable && !isImageFile(file.fileName, file.fileType)) {
                    loadPdfPreview(file.id)
                }
            } catch (e: IOException) {
                _uiState.value = FileDetailUiState.Error("Network error. Check your connection and try again.")
            } catch (e: HttpException) {
                _uiState.value = FileDetailUiState.Error("Couldn't load this file (HTTP ${e.code()}).")
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
        setPreview(PreviewState.Unavailable("The preview couldn't be opened."))
    }

    private fun initialPreview(file: StudyFile): PreviewState = when {
        isImageFile(file.fileName, file.fileType) -> PreviewState.Image(file.publicUrl)
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
                    response.isSuccessful && body != null -> PreviewState.Pdf(saveToCache(body, cached))
                    response.code() == 415 -> PreviewState.NotSupported
                    response.code() == 503 -> PreviewState.Unavailable("The preview can't be generated right now.")
                    else -> PreviewState.Unavailable("Preview failed to load (HTTP ${response.code()}).")
                }
            } catch (e: IOException) {
                PreviewState.Unavailable("Network error while loading the preview.")
            } catch (e: HttpException) {
                PreviewState.Unavailable("Preview failed to load (HTTP ${e.code()}).")
            }
            setPreview(preview)
        }
    }

    // Writes to a temp file first so an interrupted download never leaves a broken cached PDF
    private suspend fun saveToCache(body: ResponseBody, target: File): File = withContext(ioDispatcher) {
        previewDir.mkdirs()
        val temp = File(previewDir, "${target.name}.part")
        try {
            body.use { source ->
                temp.outputStream().use { sink -> source.byteStream().copyTo(sink) }
            }
            if (!temp.renameTo(target)) throw IOException("Couldn't save the preview.")
        } finally {
            temp.delete()
        }
        target
    }

    private fun setPreview(preview: PreviewState) {
        _uiState.update { state ->
            if (state is FileDetailUiState.Success) state.copy(preview = preview) else state
        }
    }
}
