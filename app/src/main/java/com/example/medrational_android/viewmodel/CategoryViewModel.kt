package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.R
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.model.Category
import com.example.medrational_android.data.model.CreateCategoryRequest
import com.example.medrational_android.ui.text.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CategoryUiState {
    object Loading : CategoryUiState
    data class Success(val categories: List<Category>) : CategoryUiState
    data class Error(val message: UiText) : CategoryUiState
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
                    _uiState.value = CategoryUiState.Error(UiText.Res(R.string.categories_error_load, listOf(response.code())))
                }
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error(UiText.Res(R.string.error_network))
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