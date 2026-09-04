package com.example.medrational.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational.data.api.ApiClient
import com.example.medrational.data.model.Reasoning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReasoningUiState {
    data object Loading : ReasoningUiState
    data class Success(val reasonings: List<Reasoning>) : ReasoningUiState
    data class Error(val message: String) : ReasoningUiState
}

class ReasoningViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ReasoningUiState>(ReasoningUiState.Loading)
    val uiState: StateFlow<ReasoningUiState> = _uiState.asStateFlow()

    fun loadReasoningsForCategory(categoryId: Long) {
        viewModelScope.launch {
            _uiState.value = ReasoningUiState.Loading
            try {
                val response = ApiClient.api.getReasoningsByCategory(categoryId)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = ReasoningUiState.Success(response.body()!!)
                } else {
                    _uiState.value = ReasoningUiState.Error("Failed to load reasonings (${response.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = ReasoningUiState.Error(e.localizedMessage ?: "Failed to reach server")
            }
        }
    }
}