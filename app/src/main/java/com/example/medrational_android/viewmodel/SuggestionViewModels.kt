package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.SuggestionDetail
import com.example.medrational_android.data.model.SuggestionStatus
import com.example.medrational_android.data.model.SuggestionSummary
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
import java.io.IOException

const val MAX_SUGGESTION_TITLE_LENGTH = 150
private const val NETWORK_ERROR = "Network error. Check your connection and try again."

// Turns the backend's suggestion error codes into something a user can act on
internal fun suggestionErrorMessage(code: Int): String = when (code) {
    409 -> "This suggestion was closed and archived, so no more messages can be added."
    413 -> "Files must be 10 MB or smaller."
    415 -> "This file type isn't supported. Use PDF, images, Office documents, TXT or CSV."
    400 -> "Please fill in every field."
    404 -> "This suggestion no longer exists."
    else -> "Something went wrong (HTTP $code). Please try again."
}

private fun String.toTextPart(): RequestBody = toRequestBody("text/plain".toMediaType())

// --- List ----------------------------------------------------------------------------------------

sealed interface SuggestionListUiState {
    data object Loading : SuggestionListUiState
    data class Success(val suggestions: List<SuggestionSummary>) : SuggestionListUiState
    data class Error(val message: String) : SuggestionListUiState
}

/** Admins see the inbox of every user's suggestions; users see their own. Tabs: PENDING and CLOSED (archived). */
class SuggestionListViewModel(
    private val api: MedRationalApi,
    private val isAdmin: Boolean
) : ViewModel() {

    private val _tab = MutableStateFlow(SuggestionStatus.PENDING)
    val tab: StateFlow<SuggestionStatus> = _tab.asStateFlow()

    private val _uiState = MutableStateFlow<SuggestionListUiState>(SuggestionListUiState.Loading)
    val uiState: StateFlow<SuggestionListUiState> = _uiState.asStateFlow()

    fun selectTab(status: SuggestionStatus) {
        if (status == _tab.value) return
        _tab.value = status
        _uiState.value = SuggestionListUiState.Loading
        load()
    }

    fun load() {
        val status = _tab.value
        viewModelScope.launch {
            val result = try {
                val response = if (isAdmin) api.getSuggestionInbox(status) else api.getMySuggestions(status)
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
            // Ignore a slow response for a tab the user already left
            if (status == _tab.value) _uiState.value = result
        }
    }
}

// --- New suggestion ------------------------------------------------------------------------------

sealed interface NewSuggestionUiState {
    data object Idle : NewSuggestionUiState
    data object Sending : NewSuggestionUiState
    data class Sent(val suggestionId: Long) : NewSuggestionUiState
    data class Error(val message: String) : NewSuggestionUiState
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
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    NewSuggestionUiState.Sent(body.id)
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

// --- Detail and conversation ---------------------------------------------------------------------

sealed interface SuggestionDetailUiState {
    data object Loading : SuggestionDetailUiState
    data class Success(
        val suggestion: SuggestionDetail,
        // A reply or the close request is in flight
        val busy: Boolean = false
    ) : SuggestionDetailUiState {
        val canPost: Boolean get() = suggestion.status == SuggestionStatus.PENDING
    }
    data class Error(val message: String) : SuggestionDetailUiState
}

class SuggestionDetailViewModel(private val api: MedRationalApi) : ViewModel() {

    private val _uiState = MutableStateFlow<SuggestionDetailUiState>(SuggestionDetailUiState.Loading)
    val uiState: StateFlow<SuggestionDetailUiState> = _uiState.asStateFlow()

    // One-shot feedback for a failed action, cleared by onMessageShown()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var suggestionId: Long? = null

    fun load(id: Long) {
        suggestionId = id
        // Keep the conversation on screen while refreshing
        if (_uiState.value !is SuggestionDetailUiState.Success) {
            _uiState.value = SuggestionDetailUiState.Loading
        }
        viewModelScope.launch {
            val result = fetch(id)
            _uiState.value = when {
                result is SuggestionDetailUiState.Error && _uiState.value is SuggestionDetailUiState.Success -> {
                    _message.value = result.message
                    (_uiState.value as SuggestionDetailUiState.Success).copy(busy = false)
                }
                else -> result
            }
        }
    }

    fun refresh() {
        suggestionId?.let(::load)
    }

    /** [onSent] runs once the reply is saved, so the screen can clear its composer. */
    fun sendMessage(body: String, attachment: MultipartBody.Part?, onSent: () -> Unit = {}) {
        val current = _uiState.value as? SuggestionDetailUiState.Success ?: return
        if (body.isBlank() || current.busy || !current.canPost) return
        _uiState.value = current.copy(busy = true)
        viewModelScope.launch {
            try {
                val response = api.postSuggestionMessage(current.suggestion.id, body.trim().toTextPart(), attachment)
                val message = response.body()
                if (response.isSuccessful && message != null) {
                    _uiState.update { state ->
                        if (state is SuggestionDetailUiState.Success) {
                            state.copy(
                                suggestion = state.suggestion.copy(
                                    messages = state.suggestion.messages + message,
                                    lastActivityAt = message.createdAt ?: state.suggestion.lastActivityAt
                                ),
                                busy = false
                            )
                        } else {
                            state
                        }
                    }
                    onSent()
                } else {
                    fail(suggestionErrorMessage(response.code()))
                    // Closed meanwhile: reload so the composer is replaced by the archived banner
                    if (response.code() == 409) refresh()
                }
            } catch (e: IOException) {
                fail(NETWORK_ERROR)
            } catch (e: HttpException) {
                fail(suggestionErrorMessage(e.code()))
            }
        }
    }

    // Admin only; permanent
    fun close() {
        val current = _uiState.value as? SuggestionDetailUiState.Success ?: return
        if (current.busy || !current.canPost) return
        _uiState.value = current.copy(busy = true)
        viewModelScope.launch {
            try {
                val response = api.closeSuggestion(current.suggestion.id)
                val closed = response.body()
                if (response.isSuccessful && closed != null) {
                    _uiState.value = SuggestionDetailUiState.Success(closed)
                } else {
                    fail(suggestionErrorMessage(response.code()))
                    if (response.code() == 409) refresh()
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

    private suspend fun fetch(id: Long): SuggestionDetailUiState = try {
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

    private fun fail(message: String) {
        _message.value = message
        _uiState.update { state ->
            if (state is SuggestionDetailUiState.Success) state.copy(busy = false) else state
        }
    }
}
