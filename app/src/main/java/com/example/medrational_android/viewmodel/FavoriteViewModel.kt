package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.model.FavoriteResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FavoriteUiState {
    data object Loading : FavoriteUiState()
    data class Success(val favorites: List<FavoriteResponse>) : FavoriteUiState()
    data class Error(val message: String) : FavoriteUiState()
}

class FavoriteViewModel(
    private val tokenManager: TokenManager,
    private val api: MedRationalApi = ApiClient.api
) : ViewModel() {

    private val _uiState = MutableStateFlow<FavoriteUiState>(FavoriteUiState.Loading)
    val uiState: StateFlow<FavoriteUiState> = _uiState.asStateFlow()

    private val _favoritedFileIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoritedFileIds: StateFlow<Set<Long>> = _favoritedFileIds.asStateFlow()

    fun loadFavorites() {
        val userId = tokenManager.getUserId()
        if (userId <= 0L) {
            _uiState.value = FavoriteUiState.Error("User session not found. Please log in again.")
            return
        }

        viewModelScope.launch {
            try {
                val response = api.getUserFavorites(userId)
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!.content
                    _uiState.value = FavoriteUiState.Success(list)
                    // Persist the set of file IDs so all hearts know they are liked
                    _favoritedFileIds.value = list.map { it.file.id }.toSet()
                } else {
                    _uiState.value = FavoriteUiState.Error("Failed to fetch favorites.")
                }
            } catch (e: Exception) {
                _uiState.value = FavoriteUiState.Error(e.localizedMessage ?: "Network error.")
            }
        }
    }

    // Files with a toggle request in flight; extra taps are ignored so requests can't race
    private val pendingToggles = mutableSetOf<Long>()

    fun toggleFavorite(fileId: Long) {
        val userId = tokenManager.getUserId()
        if (userId <= 0L) return
        if (!pendingToggles.add(fileId)) return

        // Optimistic UI update: flip the heart immediately
        val currentSet = _favoritedFileIds.value.toMutableSet()
        val willBeFavorited = !currentSet.contains(fileId)
        if (willBeFavorited) {
            currentSet.add(fileId)
        } else {
            currentSet.remove(fileId)
            // Drop it from the Favorites list right away
            removeFromList(fileId)
        }
        _favoritedFileIds.value = currentSet

        viewModelScope.launch {
            try {
                val response = api.toggleFavorite(userId, fileId)
                if (response.isSuccessful && response.body() != null) {
                    val isFav = response.body()!!.isFavorited
                    // Sync with actual backend confirmation
                    val confirmedSet = _favoritedFileIds.value.toMutableSet()
                    if (isFav) confirmedSet.add(fileId) else confirmedSet.remove(fileId)
                    _favoritedFileIds.value = confirmedSet
                    if (!isFav) removeFromList(fileId)
                } else {
                    // Revert optimistic update on failure
                    loadFavorites()
                }
            } catch (e: Exception) {
                loadFavorites()
            } finally {
                pendingToggles.remove(fileId)
            }
        }
    }

    private fun removeFromList(fileId: Long) {
        val state = _uiState.value
        if (state is FavoriteUiState.Success) {
            _uiState.value = FavoriteUiState.Success(state.favorites.filter { it.file.id != fileId })
        }
    }
}