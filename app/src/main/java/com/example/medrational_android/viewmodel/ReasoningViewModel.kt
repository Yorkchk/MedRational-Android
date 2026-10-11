package com.example.medrational_android.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.VisibleForTesting
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.AttachHashtagsRequest
import com.example.medrational_android.data.model.CreateReasoningRequest
import com.example.medrational_android.data.model.FileRatingSummary
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.rating.FileRatingStore
import com.example.medrational_android.data.upload.describePickedFile
import com.example.medrational_android.data.upload.toPart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import retrofit2.HttpException
import java.io.IOException

sealed interface ReasoningUiState {
    object Loading : ReasoningUiState
    data class Success(val reasonings: List<Reasoning>) : ReasoningUiState
    data class Error(val message: String) : ReasoningUiState
}

// Every parameter has a default, so the no-arg constructor used by viewModel() in MainActivity still exists
class ReasoningViewModel(
    private val api: MedRationalApi = ApiClient.api,
    private val ratingStore: FileRatingStore = FileRatingStore.shared
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReasoningUiState>(ReasoningUiState.Loading)
    val uiState: StateFlow<ReasoningUiState> = _uiState.asStateFlow()

    // One-shot upload feedback, cleared by onMessageShown()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var currentCategoryId: Long = 0L

    fun loadReasoningsForCategory(categoryId: Long) {
        currentCategoryId = categoryId
        _uiState.value = ReasoningUiState.Loading
        viewModelScope.launch {
            try {
                val response = api.getReasoningsByCategory(categoryId)
                if (response.isSuccessful && response.body() != null) {
                    val reasonings = response.body()!!
                    ratingStore.seed(reasonings.flatMap { it.files.orEmpty() }.map { FileRatingSummary(it.id, it.avgRating, it.totalRatings) })
                    _uiState.value = ReasoningUiState.Success(reasonings)
                } else {
                    _uiState.value = ReasoningUiState.Error("Failed to fetch reasonings")
                }
            } catch (e: Exception) {
                _uiState.value = ReasoningUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun createReasoning(title: String, content: String) {
        viewModelScope.launch {
            try {
                val response = api.createReasoning(
                    CreateReasoningRequest(categoryId = currentCategoryId, title = title, content = content)
                )
                if (response.isSuccessful) {
                    loadReasoningsForCategory(currentCategoryId)
                }
            } catch (_: Exception) {}
        }
    }

    fun deleteReasoning(reasoningId: Long) {
        viewModelScope.launch {
            try {
                val response = api.deleteReasoning(reasoningId)
                if (response.isSuccessful) {
                    loadReasoningsForCategory(currentCategoryId)
                }
            } catch (_: Exception) {}
        }
    }

    fun deleteFile(fileId: Long) {
        viewModelScope.launch {
            try {
                val response = api.deleteFile(fileId)
                if (response.isSuccessful) {
                    loadReasoningsForCategory(currentCategoryId)
                }
            } catch (_: Exception) {}
        }
    }

    fun onMessageShown() {
        _message.value = null
    }

    fun uploadFile(context: Context, reasoningId: Long, uri: Uri, tags: List<String> = emptyList()) {
        viewModelScope.launch {
            val part = try {
                describePickedFile(context, uri).toPart(context, "file")
            } catch (e: IOException) {
                _message.value = "Couldn't read the selected file."
                return@launch
            } catch (e: SecurityException) {
                // The picker's permission on the Uri can be revoked before we read it
                _message.value = "Couldn't read the selected file."
                return@launch
            }
            uploadAndTag(reasoningId, part, tags)
        }
    }

    // Uploads, then attaches the tags to the new file; the upload stands even if tagging fails
    @VisibleForTesting
    internal suspend fun uploadAndTag(reasoningId: Long, part: MultipartBody.Part, tags: List<String>) {
        val uploaded = try {
            val response = api.uploadFile(reasoningId, part)
            if (response.isSuccessful) response.body() else null
        } catch (e: IOException) {
            null
        } catch (e: HttpException) {
            null
        }
        if (uploaded == null) {
            _message.value = "Upload failed. Check your connection and try again."
            return
        }

        _message.value = if (tags.isEmpty()) {
            "Uploaded ${uploaded.fileName}"
        } else {
            val tagged = try {
                api.attachHashtags(uploaded.id, AttachHashtagsRequest(tags)).isSuccessful
            } catch (e: IOException) {
                false
            } catch (e: HttpException) {
                false
            }
            if (tagged) {
                "Uploaded and tagged ${uploaded.fileName}"
            } else {
                "Uploaded ${uploaded.fileName}, but its hashtags couldn't be saved."
            }
        }
        loadReasoningsForCategory(currentCategoryId)
    }
}