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
import com.example.medrational_android.data.model.Reasoning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import java.io.File
import java.io.FileOutputStream
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap

sealed interface ReasoningUiState {
    object Loading : ReasoningUiState
    data class Success(val reasonings: List<Reasoning>) : ReasoningUiState
    data class Error(val message: String) : ReasoningUiState
}

// Every parameter has a default, so the no-arg constructor used by viewModel() in MainActivity still exists
class ReasoningViewModel(private val api: MedRationalApi = ApiClient.api) : ViewModel() {

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
                    _uiState.value = ReasoningUiState.Success(response.body()!!)
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
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

                // 1. Extract the actual original file name from the Uri
                var originalFileName: String? = null
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        originalFileName = cursor.getString(nameIndex)
                    }
                }

                // Fallback: If originalFileName is missing or lacks an extension, determine extension from MIME type
                if (originalFileName == null) {
                    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
                    originalFileName = "upload_${System.currentTimeMillis()}.$extension"
                }

                // 2. Copy the contents to a temporary cache file
                val tempFile = File(context.cacheDir, originalFileName!!)
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@launch

                // 3. Build multipart form with the real filename
                val requestFile = tempFile.readBytes().toRequestBody(mimeType.toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData("file", originalFileName!!, requestFile)

                try {
                    uploadAndTag(reasoningId, multipartBody, tags)
                } finally {
                    tempFile.delete()
                }
            } catch (e: IOException) {
                _message.value = "Couldn't read the selected file."
            } catch (e: SecurityException) {
                // The picker's permission on the Uri can be revoked before we read it
                _message.value = "Couldn't read the selected file."
            }
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