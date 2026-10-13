package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.R
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.FileRatingSummary
import com.example.medrational_android.data.model.RecentDownload
import com.example.medrational_android.data.rating.FileRatingStore
import com.example.medrational_android.ui.text.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface RecentDownloadsUiState {
    data object Loading : RecentDownloadsUiState
    data class Success(val downloads: List<RecentDownload>) : RecentDownloadsUiState
    data class Error(val message: UiText) : RecentDownloadsUiState
}

class RecentDownloadsViewModel(
    private val api: MedRationalApi,
    private val userId: Long,
    private val ratingStore: FileRatingStore = FileRatingStore.shared
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
                    val openable = body.filter { it.fileId != null && it.reasoningId != null }
                    ratingStore.seed(openable.map { FileRatingSummary(it.fileId!!, it.avgRating, it.totalRatings) })
                    RecentDownloadsUiState.Success(openable)
                } else {
                    RecentDownloadsUiState.Error(UiText.Res(R.string.downloads_error_load, listOf(response.code())))
                }
            } catch (e: IOException) {
                RecentDownloadsUiState.Error(UiText.Res(R.string.error_network))
            } catch (e: HttpException) {
                RecentDownloadsUiState.Error(UiText.Res(R.string.downloads_error_load, listOf(e.code())))
            }
        }
    }
}
