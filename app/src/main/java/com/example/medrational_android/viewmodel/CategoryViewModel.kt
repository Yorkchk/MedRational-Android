package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.model.Category
import com.example.medrational_android.data.model.CreateCategoryRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CategoryUiState {
    object Loading : CategoryUiState
    data class Success(val categories: List<Category>) : CategoryUiState
    data class Error(val message: String) : CategoryUiState
}

class CategoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        _uiState.value = CategoryUiState.Loading
        viewModelScope.launch {
            try {
                val response = ApiClient.api.getCategories()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = CategoryUiState.Success(response.body()!!)
                } else {
                    _uiState.value = CategoryUiState.Error("Failed to fetch categories")
                }
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun createCategory(name: String, description: String?) {
        viewModelScope.launch {
            try {
                val response = ApiClient.api.createCategory(CreateCategoryRequest(name, description))
                if (response.isSuccessful) {
                    loadCategories()
                }
            } catch (_: Exception) {}
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            try {
                val response = ApiClient.api.deleteCategory(id)
                if (response.isSuccessful) {
                    loadCategories()
                }
            } catch (_: Exception) {}
        }
    }
}