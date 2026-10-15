package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.R
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.SuggestionDetail
import com.example.medrational_android.data.model.SuggestionSummary
import com.example.medrational_android.data.model.isImageFile
import com.example.medrational_android.data.preview.saveToCache
import com.example.medrational_android.ui.text.UiText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException

const val MAX_SUGGESTION_TITLE_LENGTH = 150
private val NETWORK_ERROR = UiText.Res(R.string.error_network)

// Turns the backend's suggestion error codes into something a user can act on
internal fun suggestionErrorMessage(code: Int): UiText = when (code) {
    413 -> UiText.Res(R.string.error_file_too_large)
    415 -> UiText.Res(R.string.suggestion_error_file_type)
    400 -> UiText.Res(R.string.suggestion_error_fields)
    404 -> UiText.Res(R.string.suggestion_error_gone)
    else -> UiText.Res(R.string.error_generic_http, listOf(code))
}

private fun String.toTextPart(): RequestBody = toRequestBody("text/plain".toMediaType())

// --- Admin inbox ---------------------------------------------------------------------------------

sealed interface SuggestionListUiState {
    data object Loading : SuggestionListUiState
    data class Success(val suggestions: List<SuggestionSummary>) : SuggestionListUiState
    data class Error(val message: UiText) : SuggestionListUiState
}

/** Admin only: every suggestion users sent, newest activity first. */
class SuggestionListViewModel(private val api: MedRationalApi) : ViewModel() {

    private val _uiState = MutableStateFlow<SuggestionListUiState>(SuggestionListUiState.Loading)
    val uiState: StateFlow<SuggestionListUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = try {
                val response = api.getSuggestionInbox()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    SuggestionListUiState.Success(body.content)
                } else {
                    SuggestionListUiState.Error(suggestionErrorMessage(response.code()))
                }
            } catch (e: IOException) {
                SuggestionListUiState.Error(NETWORK_ERROR)
            } catch (e: HttpException) {
                SuggestionListUiState.Error(suggestionErrorMessage(e.code()))
            }
        }
    }
}

// --- New suggestion ------------------------------------------------------------------------------

sealed interface NewSuggestionUiState {
    data object Idle : NewSuggestionUiState
    data object Sending : NewSuggestionUiState
    data object Sent : NewSuggestionUiState
    data class Error(val message: UiText) : NewSuggestionUiState
}

class NewSuggestionViewModel(private val api: MedRationalApi) : ViewModel() {

    private val _uiState = MutableStateFlow<NewSuggestionUiState>(NewSuggestionUiState.Idle)
    val uiState: StateFlow<NewSuggestionUiState> = _uiState.asStateFlow()

    fun canSubmit(title: String, description: String): Boolean =
        title.isNotBlank() && description.isNotBlank() && title.trim().length <= MAX_SUGGESTION_TITLE_LENGTH

    fun submit(title: String, description: String, attachment: MultipartBody.Part?) {
        if (!canSubmit(title, description) || _uiState.value == NewSuggestionUiState.Sending) return
        _uiState.value = NewSuggestionUiState.Sending
        viewModelScope.launch {
            _uiState.value = try {
                val response = api.createSuggestion(
                    title.trim().toTextPart(),
                    description.trim().toTextPart(),
                    attachment
                )
                if (response.isSuccessful) {
                    NewSuggestionUiState.Sent
                } else {
                    NewSuggestionUiState.Error(suggestionErrorMessage(response.code()))
                }
            } catch (e: IOException) {
                NewSuggestionUiState.Error(NETWORK_ERROR)
            } catch (e: HttpException) {
                NewSuggestionUiState.Error(suggestionErrorMessage(e.code()))
            }
        }
    }

    // Lets the form be submitted again after an error was shown
    fun onErrorShown() {
        if (_uiState.value is NewSuggestionUiState.Error) _uiState.value = NewSuggestionUiState.Idle
    }
}

// --- Admin detail --------------------------------------------------------------------------------

sealed interface SuggestionDetailUiState {
    data object Loading : SuggestionDetailUiState
    data class Success(
        val suggestion: SuggestionDetail,
        // The delete request is in flight
        val busy: Boolean = false
    ) : SuggestionDetailUiState
    data object Deleted : SuggestionDetailUiState
    data class Error(val message: UiText) : SuggestionDetailUiState
}

/** Admin only: one suggestion with its sender's details and a preview of its attachment. */
class SuggestionDetailViewModel(
    private val api: MedRationalApi,
    // Converted PDFs are cached here as suggestion-{id}.pdf so reopening a suggestion is instant
    private val previewDir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow<SuggestionDetailUiState>(SuggestionDetailUiState.Loading)
    val uiState: StateFlow<SuggestionDetailUiState> = _uiState.asStateFlow()

    // null while loading, or when the suggestion has no attachment
    private val _preview = MutableStateFlow<PreviewState?>(null)
    val preview: StateFlow<PreviewState?> = _preview.asStateFlow()

    // One-shot feedback for a failed delete, cleared by onMessageShown()
    private val _message = MutableStateFlow<UiText?>(null)
    val message: StateFlow<UiText?> = _message.asStateFlow()

    fun load(id: Long) {
        _uiState.value = SuggestionDetailUiState.Loading
        _preview.value = null
        viewModelScope.launch {
            val state = try {
                val response = api.getSuggestion(id)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    SuggestionDetailUiState.Success(body)
                } else {
                    SuggestionDetailUiState.Error(suggestionErrorMessage(response.code()))
                }
            } catch (e: IOException) {
                SuggestionDetailUiState.Error(NETWORK_ERROR)
            } catch (e: HttpException) {
                SuggestionDetailUiState.Error(suggestionErrorMessage(e.code()))
            }
            _uiState.value = state
            if (state is SuggestionDetailUiState.Success) startPreview(state.suggestion)
        }
    }

    fun retryPreview() {
        val suggestion = (_uiState.value as? SuggestionDetailUiState.Success)?.suggestion ?: return
        if (_preview.value !is PreviewState.Unavailable) return
        _preview.value = PreviewState.Loading
        loadPdf(suggestion.id)
    }

    // The cached PDF couldn't be opened (e.g. truncated); drop it so a retry downloads it again
    fun onPreviewCorrupted() {
        val preview = _preview.value as? PreviewState.Pdf ?: return
        preview.file.delete()
        _preview.value = PreviewState.Unavailable(UiText.Res(R.string.preview_error_corrupted))
    }

    // Permanent; the screen confirms first
    fun delete() {
        val current = _uiState.value as? SuggestionDetailUiState.Success ?: return
        if (current.busy) return
        _uiState.value = current.copy(busy = true)
        viewModelScope.launch {
            try {
                val response = api.deleteSuggestion(current.suggestion.id)
                // Already gone counts as deleted
                if (response.isSuccessful || response.code() == 404) {
                    cachedPdf(current.suggestion.id).delete()
                    _uiState.value = SuggestionDetailUiState.Deleted
                } else {
                    fail(suggestionErrorMessage(response.code()))
                }
            } catch (e: IOException) {
                fail(NETWORK_ERROR)
            } catch (e: HttpException) {
                fail(suggestionErrorMessage(e.code()))
            }
        }
    }

    fun onMessageShown() {
        _message.value = null
    }

    private fun cachedPdf(suggestionId: Long) = File(previewDir, "suggestion-$suggestionId.pdf")

    private fun startPreview(suggestion: SuggestionDetail) {
        val attachment = suggestion.attachment
        _preview.value = when {
            attachment == null -> null
            isImageFile(attachment.fileName, attachment.contentType) ->
                PreviewState.Image(ApiClient.suggestionAttachmentPreviewUrl(suggestion.id))
            attachment.previewable -> PreviewState.Loading
            else -> PreviewState.NotSupported
        }
        if (_preview.value == PreviewState.Loading) loadPdf(suggestion.id)
    }

    private fun loadPdf(suggestionId: Long) {
        viewModelScope.launch {
            val cached = cachedPdf(suggestionId)
            if (cached.exists() && cached.length() > 0) {
                _preview.value = PreviewState.Pdf(cached)
                return@launch
            }
            _preview.value = try {
                val response = api.getSuggestionAttachmentPreview(suggestionId)
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
        }
    }

    private fun fail(message: UiText) {
        _message.value = message
        _uiState.update { state ->
            if (state is SuggestionDetailUiState.Success) state.copy(busy = false) else state
        }
    }
}
