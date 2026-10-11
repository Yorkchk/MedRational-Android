package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.AttachHashtagsRequest
import com.example.medrational_android.data.model.Hashtag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale

sealed interface HashtagsUiState {
    data object Loading : HashtagsUiState
    data class Success(
        val hashtags: List<Hashtag>,
        // An add or rename is in flight; admin controls are disabled meanwhile
        val busy: Boolean = false,
        // One-shot feedback for a failed action, cleared by onMessageShown()
        val message: String? = null
    ) : HashtagsUiState
    data class Error(val message: String) : HashtagsUiState
}

/**
 * Hashtags of a single file. The backend has no global hashtag endpoints, so creating a tag means
 * attaching a new name, deleting means detaching (orphaned tags are removed server-side) and
 * renaming means attaching the new name then detaching the old one.
 */
class FileHashtagsViewModel(private val api: MedRationalApi) : ViewModel() {

    private val _uiState = MutableStateFlow<HashtagsUiState>(HashtagsUiState.Loading)
    val uiState: StateFlow<HashtagsUiState> = _uiState.asStateFlow()

    private var fileId: Long? = null

    fun load(fileId: Long) {
        this.fileId = fileId
        _uiState.value = HashtagsUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val response = api.getFileHashtags(fileId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    HashtagsUiState.Success(body.sortedByName())
                } else {
                    HashtagsUiState.Error("Couldn't load hashtags (HTTP ${response.code()}).")
                }
            } catch (e: IOException) {
                HashtagsUiState.Error("Network error while loading hashtags.")
            } catch (e: HttpException) {
                HashtagsUiState.Error("Couldn't load hashtags (HTTP ${e.code()}).")
            }
        }
    }

    /** Accepts one or more names separated by spaces or commas, with or without '#'. */
    fun addHashtags(input: String) {
        val fileId = fileId ?: return
        val current = _uiState.value as? HashtagsUiState.Success ?: return
        if (current.busy) return
        val names = parseHashtags(input)
        if (names.isEmpty()) return

        setBusy(true)
        viewModelScope.launch {
            val error = attach(fileId, names)
            if (error != null) finishWithMessage(error) else setBusy(false)
        }
    }

    fun renameHashtag(hashtag: Hashtag, newName: String) {
        val fileId = fileId ?: return
        val current = _uiState.value as? HashtagsUiState.Success ?: return
        if (current.busy) return
        // A rename must be a single tag; spaces or commas would turn it into several
        val name = parseHashtags(newName).singleOrNull() ?: return
        if (name == hashtag.name) return

        setBusy(true)
        viewModelScope.launch {
            // Attach first so a failed detach never leaves the file without the tag
            attach(fileId, listOf(name))?.let { error ->
                finishWithMessage(error)
                return@launch
            }
            val error = detach(fileId, hashtag)
            if (error != null) {
                finishWithMessage("Added #$name but couldn't remove #${hashtag.name}. $error")
            } else {
                _uiState.update { state ->
                    if (state is HashtagsUiState.Success) {
                        state.copy(hashtags = state.hashtags.filterNot { it.id == hashtag.id }, busy = false)
                    } else {
                        state
                    }
                }
            }
        }
    }

    // Optimistic: the chip disappears immediately and comes back if the request fails
    fun removeHashtag(hashtag: Hashtag) {
        val fileId = fileId ?: return
        val current = _uiState.value as? HashtagsUiState.Success ?: return
        if (current.busy) return
        _uiState.value = current.copy(hashtags = current.hashtags.filterNot { it.id == hashtag.id })

        viewModelScope.launch {
            val error = detach(fileId, hashtag) ?: return@launch
            _uiState.update { state ->
                if (state is HashtagsUiState.Success) {
                    state.copy(
                        hashtags = (state.hashtags + hashtag).distinctBy { it.id }.sortedByName(),
                        message = "Couldn't remove #${hashtag.name}. $error"
                    )
                } else {
                    state
                }
            }
        }
    }

    fun onMessageShown() {
        _uiState.update { state ->
            if (state is HashtagsUiState.Success) state.copy(message = null) else state
        }
    }

    // Replaces the list with the backend's view of the file on success; returns an error message otherwise.
    // Leaves busy set so a rename stays locked until its detach finishes.
    private suspend fun attach(fileId: Long, names: List<String>): String? = try {
        val response = api.attachHashtags(fileId, AttachHashtagsRequest(names))
        val body = response.body()
        if (response.isSuccessful && body != null) {
            _uiState.update { state ->
                if (state is HashtagsUiState.Success) state.copy(hashtags = body.sortedByName()) else state
            }
            null
        } else {
            "Couldn't save hashtags (HTTP ${response.code()})."
        }
    } catch (e: IOException) {
        "Network error. Check your connection and try again."
    } catch (e: HttpException) {
        "Couldn't save hashtags (HTTP ${e.code()})."
    }

    private suspend fun detach(fileId: Long, hashtag: Hashtag): String? = try {
        val response = api.detachHashtag(fileId, hashtag.id)
        if (response.isSuccessful) null else "HTTP ${response.code()}."
    } catch (e: IOException) {
        "Network error. Check your connection and try again."
    } catch (e: HttpException) {
        "HTTP ${e.code()}."
    }

    private fun setBusy(busy: Boolean) {
        _uiState.update { state ->
            if (state is HashtagsUiState.Success) state.copy(busy = busy) else state
        }
    }

    private fun finishWithMessage(message: String) {
        _uiState.update { state ->
            if (state is HashtagsUiState.Success) state.copy(busy = false, message = message) else state
        }
    }

    private fun List<Hashtag>.sortedByName() = sortedBy { it.name }
}

// Same normalisation as the backend's HashtagServiceImpl.sanitizeTag
internal fun sanitizeHashtag(raw: String): String =
    raw.trim().lowercase(Locale.ROOT).replace(Regex("^#+"), "")

internal fun parseHashtags(input: String): List<String> =
    input.split(Regex("[\\s,]+"))
        .map(::sanitizeHashtag)
        .filter { it.isNotEmpty() }
        .distinct()
