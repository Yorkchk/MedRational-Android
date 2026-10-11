package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.RecentDownload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface RecentDownloadsUiState {
    data object Loading : RecentDownloadsUiState
    data class Success(val downloads: List<RecentDownload>) : RecentDownloadsUiState
    data class Error(val message: String) : RecentDownloadsUiState
}

class RecentDownloadsViewModel(
    private val api: MedRationalApi,
    private val userId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecentDownloadsUiState>(RecentDownloadsUiState.Loading)
    val uiState: StateFlow<RecentDownloadsUiState> = _uiState.asStateFlow()

    fun load() {
        // Keep the current list on screen while refreshing
        if (_uiState.value !is RecentDownloadsUiState.Success) {
            _uiState.value = RecentDownloadsUiState.Loading
        }
        viewModelScope.launch {
            _uiState.value = try {
                val response = api.getRecentDownloads(userId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    // The backend already sorts newest first; entries whose file or reasoning is gone can't be opened
                    RecentDownloadsUiState.Success(body.filter { it.fileId != null && it.reasoningId != null })
                } else {
                    RecentDownloadsUiState.Error("Couldn't load your downloads (HTTP ${response.code()}).")
                }
            } catch (e: IOException) {
                RecentDownloadsUiState.Error("Network error. Check your connection and try again.")
            } catch (e: HttpException) {
                RecentDownloadsUiState.Error("Couldn't load your downloads (HTTP ${e.code()}).")
            }
        }
    }
}
