package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.R
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.AdminStatistics
import com.example.medrational_android.data.model.ContentStats
import com.example.medrational_android.ui.text.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/** Period of the content leaderboards: the last 7 days or since the start. */
enum class StatRange { WEEK, ALL_TIME }

sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState
    data class Success(
        val stats: AdminStatistics,
        val range: StatRange = StatRange.WEEK,
        val refreshing: Boolean = false,
        // Set when a refresh failed; the previous figures stay on screen
        val refreshError: UiText? = null
    ) : StatisticsUiState {
        val content: ContentStats get() = if (range == StatRange.WEEK) stats.lastWeek else stats.allTime
    }
    data class Error(val message: UiText) : StatisticsUiState
}

/** The admin statistics screen. Only create it for admins: the endpoint answers 403 to others, which logs out. */
class StatisticsViewModel(private val api: MedRationalApi) : ViewModel() {

    private val _uiState = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Loading)
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = StatisticsUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                StatisticsUiState.Success(fetch())
            } catch (e: IOException) {
                StatisticsUiState.Error(UiText.Res(R.string.error_network))
            } catch (e: HttpException) {
                StatisticsUiState.Error(statisticsErrorMessage(e.code()))
            }
        }
    }

    /** Reloads while keeping the current figures and range on screen. */
    fun refresh() {
        val current = _uiState.value
        if (current !is StatisticsUiState.Success) {
            load()
            return
        }
        if (current.refreshing) return
        _uiState.value = current.copy(refreshing = true, refreshError = null)
        viewModelScope.launch {
            val error = try {
                val stats = fetch()
                _uiState.update { state ->
                    if (state is StatisticsUiState.Success) state.copy(stats = stats, refreshing = false) else state
                }
                return@launch
            } catch (e: IOException) {
                UiText.Res(R.string.error_network)
            } catch (e: HttpException) {
                statisticsErrorMessage(e.code())
            }
            _uiState.update { state ->
                if (state is StatisticsUiState.Success) state.copy(refreshing = false, refreshError = error) else state
            }
        }
    }

    fun refreshErrorShown() {
        _uiState.update { state ->
            if (state is StatisticsUiState.Success) state.copy(refreshError = null) else state
        }
    }

    fun selectRange(range: StatRange) {
        _uiState.update { state ->
            if (state is StatisticsUiState.Success) state.copy(range = range) else state
        }
    }

    private suspend fun fetch(): AdminStatistics {
        val response = api.getAdminStatistics()
        val body = response.body()
        if (!response.isSuccessful || body == null) throw HttpException(response)
        return body
    }
}

internal fun statisticsErrorMessage(code: Int): UiText = UiText.Res(R.string.stats_error_load, listOf(code))
