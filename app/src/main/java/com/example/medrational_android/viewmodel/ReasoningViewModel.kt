package com.example.medrational_android.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.model.CreateReasoningRequest
import com.example.medrational_android.data.model.Reasoning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap

sealed interface ReasoningUiState {
    object Loading : ReasoningUiState
    data class Success(val reasonings: List<Reasoning>) : ReasoningUiState
    data class Error(val message: String) : ReasoningUiState
}

class ReasoningViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ReasoningUiState>(ReasoningUiState.Loading)
    val uiState: StateFlow<ReasoningUiState> = _uiState.asStateFlow()

    private var currentCategoryId: Long = 0L

    fun loadReasoningsForCategory(categoryId: Long) {
        currentCategoryId = categoryId
        _uiState.value = ReasoningUiState.Loading
        viewModelScope.launch {
            try {
                val response = ApiClient.api.getReasoningsByCategory(categoryId)
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
                val response = ApiClient.api.createReasoning(
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
                val response = ApiClient.api.deleteReasoning(reasoningId)
                if (response.isSuccessful) {
                    loadReasoningsForCategory(currentCategoryId)
                }
            } catch (_: Exception) {}
        }
    }

    fun deleteFile(fileId: Long) {
        viewModelScope.launch {
            try {
                val response = ApiClient.api.deleteFile(fileId)
                if (response.isSuccessful) {
                    loadReasoningsForCategory(currentCategoryId)
                }
            } catch (_: Exception) {}
        }
    }

    fun uploadFile(context: Context, reasoningId: Long, uri: Uri) {
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

                val response = ApiClient.api.uploadFile(reasoningId, multipartBody)
                tempFile.delete()

                if (response.isSuccessful) {
                    loadReasoningsForCategory(currentCategoryId)
                }
            } catch (_: Exception) {}
        }
    }
}