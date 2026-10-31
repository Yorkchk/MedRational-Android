package com.example.medrational_android.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.VisibleForTesting
import com.example.medrational_android.R
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.AttachHashtagsRequest
import com.example.medrational_android.data.model.CreateReasoningRequest
import com.example.medrational_android.data.model.FileRatingSummary
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.data.rating.FileRatingStore
import com.example.medrational_android.data.upload.MAX_UPLOAD_FILE_BYTES
import com.example.medrational_android.data.upload.describePickedFile
import com.example.medrational_android.data.upload.exceedsLimit
import com.example.medrational_android.data.upload.toPart
import com.example.medrational_android.data.upload.uploadBatches
import com.example.medrational_android.ui.text.UiText
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
    data class Error(val message: UiText) : ReasoningUiState
}

// Every parameter has a default, so the no-arg constructor used by viewModel() in MainActivity still exists
class ReasoningViewModel(
    private val api: MedRationalApi = ApiClient.api,
    private val ratingStore: FileRatingStore = FileRatingStore.shared
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReasoningUiState>(ReasoningUiState.Loading)
    val uiState: StateFlow<ReasoningUiState> = _uiState.asStateFlow()

    // One-shot upload feedback, cleared by onMessageShown()
    private val _message = MutableStateFlow<UiText?>(null)
    val message: StateFlow<UiText?> = _message.asStateFlow()

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
                    _uiState.value = ReasoningUiState.Error(UiText.Res(R.string.reasonings_error_load, listOf(response.code())))
                }
            } catch (e: Exception) {
                _uiState.value = ReasoningUiState.Error(UiText.Res(R.string.error_network))
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
                _message.value = UiText.Res(R.string.upload_error_read_file)
                return@launch
            } catch (e: SecurityException) {
                // The picker's permission on the Uri can be revoked before we read it
                _message.value = UiText.Res(R.string.upload_error_read_file)
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
            _message.value = UiText.Res(R.string.upload_error_failed)
            return
        }

        _message.value = if (tags.isEmpty()) {
            UiText.Res(R.string.upload_done, listOf(uploaded.fileName))
        } else {
            val tagged = try {
                api.attachHashtags(uploaded.id, AttachHashtagsRequest(tags)).isSuccessful
            } catch (e: IOException) {
                false
            } catch (e: HttpException) {
                false
            }
            if (tagged) {
                UiText.Res(R.string.upload_done_tagged, listOf(uploaded.fileName))
            } else {
                UiText.Res(R.string.upload_done_tags_failed, listOf(uploaded.fileName))
            }
        }
        loadReasoningsForCategory(currentCategoryId)
    }

    /**
     * Uploads files picked together through the batch endpoint, so students get one push for them
     * rather than one per file. A single file goes through [uploadFile].
     */
    fun uploadFiles(context: Context, reasoningId: Long, uris: List<Uri>, tags: List<String> = emptyList()) {
        if (uris.size == 1) return uploadFile(context, reasoningId, uris.single(), tags)
        if (uris.isEmpty()) return
        viewModelScope.launch {
            // Unreadable files and files the backend would refuse are left out and counted as not sent
            val picked = uris.mapNotNull { uri ->
                try {
                    describePickedFile(context, uri)
                } catch (e: IOException) {
                    null
                } catch (e: SecurityException) {
                    null
                }
            }.filterNot { it.exceedsLimit(MAX_UPLOAD_FILE_BYTES) }
            // Read one batch at a time, so only one request's files are in memory
            val batches = uploadBatches(picked).map { batch ->
                suspend { batch.map { it.toPart(context, "files") } }
            }
            uploadBatchesAndTag(reasoningId, batches, tags, total = uris.size)
        }
    }

    // Sends each batch, then attaches the tags to its files; uploads stand even if tagging fails.
    // [total] is how many files were picked, so files left out count as not sent
    @VisibleForTesting
    internal suspend fun uploadBatchesAndTag(
        reasoningId: Long,
        batches: List<suspend () -> List<MultipartBody.Part>>,
        tags: List<String>,
        total: Int
    ) {
        val uploaded = mutableListOf<StudyFile>()
        for (readBatch in batches) {
            try {
                val response = api.uploadFiles(reasoningId, readBatch())
                if (response.isSuccessful) uploaded += response.body().orEmpty()
            } catch (e: IOException) {
                // Offline, or a file that couldn't be read: the other batches still go
            } catch (e: SecurityException) {
            } catch (e: HttpException) {
            }
        }
        if (uploaded.isEmpty()) {
            _message.value = UiText.Res(R.string.upload_error_failed)
            return
        }

        val tagFailures = if (tags.isEmpty()) 0 else uploaded.count { file ->
            try {
                !api.attachHashtags(file.id, AttachHashtagsRequest(tags)).isSuccessful
            } catch (e: IOException) {
                true
            } catch (e: HttpException) {
                true
            }
        }
        val sent = uploaded.size
        _message.value = when {
            sent < total -> UiText.Res(R.string.upload_many_partial, listOf(sent, fileCount(total)))
            tagFailures > 0 -> UiText.Res(R.string.upload_many_tags_failed, listOf(fileCount(sent), fileCount(tagFailures)))
            tags.isEmpty() -> UiText.Plural(R.plurals.upload_many_done, sent)
            else -> UiText.Plural(R.plurals.upload_many_done_tagged, sent)
        }
        loadReasoningsForCategory(currentCategoryId)
    }

    private fun fileCount(count: Int) = UiText.Plural(R.plurals.file_count, count)
}