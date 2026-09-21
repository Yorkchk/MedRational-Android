package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.Category
import com.example.medrational_android.data.model.FileSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    data object Idle : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(val files: List<FileSearchResult>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(
    private val api: MedRationalApi = ApiClient.api
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    var searchQuery = MutableStateFlow("")
    var selectedCategory = MutableStateFlow<Category?>(null)

    private var searchJob: Job? = null

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val res = api.getCategories()
                if (res.isSuccessful && res.body() != null) {
                    _categories.value = res.body()!!
                }
            } catch (_: Exception) {}
        }
    }

    fun onQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
        triggerDebouncedSearch()
    }

    fun onCategorySelected(category: Category?) {
        selectedCategory.value = category
        triggerDebouncedSearch()
    }

    private fun triggerDebouncedSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350) // Debounce typing
            executeSearch()
        }
    }

    private suspend fun executeSearch() {
        val query = searchQuery.value.trim().ifEmpty { null }
        val categoryId = selectedCategory.value?.id

        if (query == null && categoryId == null) {
            _uiState.value = SearchUiState.Idle
            return
        }

        _uiState.value = SearchUiState.Loading
        try {
            val response = api.searchFiles(query, categoryId)
            if (response.isSuccessful && response.body() != null) {
                _uiState.value = SearchUiState.Success(response.body()!!.content)
            } else {
                _uiState.value = SearchUiState.Error("No results found.")
            }
        } catch (e: Exception) {
            _uiState.value = SearchUiState.Error(e.localizedMessage ?: "Search failed.")
        }
    }
}