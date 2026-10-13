package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.R
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.FileRatingSummary
import com.example.medrational_android.data.model.NoveltiesFeed
import com.example.medrational_android.data.model.NoveltyItem
import com.example.medrational_android.data.rating.FileRatingStore
import com.example.medrational_android.ui.text.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface NoveltiesUiState {
    data object Loading : NoveltiesUiState
    data class Success(
        val items: List<NoveltyItem>,
        // Every file uploaded since the last visit, including those on pages not loaded yet
        val newCount: Long,
        val canLoadMore: Boolean,
        val loadingMore: Boolean = false,
        val loadMoreFailed: Boolean = false
    ) : NoveltiesUiState
    data class Error(val message: UiText) : NoveltiesUiState
}

/**
 * The "What's new" feed. It loads once per screen visit and then tells the backend the feed was
 * seen, so the files flagged new keep their highlight while the user browses into them and back;
 * they show as seen on the next visit.
 */
class NoveltiesViewModel(
    private val api: MedRationalApi,
    private val pageSize: Int = PAGE_SIZE,
    private val ratingStore: FileRatingStore = FileRatingStore.shared
) : ViewModel() {

    private val _uiState = MutableStateFlow<NoveltiesUiState>(NoveltiesUiState.Loading)
    val uiState: StateFlow<NoveltiesUiState> = _uiState.asStateFlow()

    private var nextPage = 0
    private var markedSeen = false

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = NoveltiesUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val feed = fetchPage(0)
                nextPage = 1
                markSeenOnce()
                NoveltiesUiState.Success(
                    items = feed.feed.content.openable(),
                    newCount = feed.newFilesCount,
                    canLoadMore = feed.hasMorePages()
                )
            } catch (e: IOException) {
                NoveltiesUiState.Error(UiText.Res(R.string.error_network))
            } catch (e: HttpException) {
                NoveltiesUiState.Error(UiText.Res(R.string.novelties_error_load, listOf(e.code())))
            }
        }
    }

    fun loadMore() {
        val current = _uiState.value as? NoveltiesUiState.Success ?: return
        if (!current.canLoadMore || current.loadingMore) return
        _uiState.value = current.copy(loadingMore = true, loadMoreFailed = false)
        viewModelScope.launch {
            try {
                val feed = fetchPage(nextPage)
                nextPage++
                _uiState.update { state ->
                    if (state !is NoveltiesUiState.Success) return@update state
                    // Uploads made while scrolling shift the pages; skip files already shown
                    val shown = state.items.mapTo(HashSet()) { it.fileId }
                    state.copy(
                        items = state.items + feed.feed.content.openable().filterNot { it.fileId in shown },
                        canLoadMore = feed.hasMorePages(),
                        loadingMore = false
                    )
                }
            } catch (e: IOException) {
                failLoadMore()
            } catch (e: HttpException) {
                failLoadMore()
            }
        }
    }

    private fun failLoadMore() {
        _uiState.update { state ->
            if (state is NoveltiesUiState.Success) state.copy(loadingMore = false, loadMoreFailed = true) else state
        }
    }

    private suspend fun fetchPage(page: Int): NoveltiesFeed {
        val response = api.getNovelties(page, pageSize)
        val body = response.body()
        if (!response.isSuccessful || body == null) throw HttpException(response)
        ratingStore.seed(body.feed.content.map { FileRatingSummary(it.fileId, it.avgRating, it.totalRatings) })
        return body
    }

    // Only after the feed is on screen, so nothing is marked seen that the user never got to see.
    // A failure just leaves the badge up until the next visit
    private suspend fun markSeenOnce() {
        if (markedSeen) return
        try {
            if (api.markNoveltiesSeen().isSuccessful) markedSeen = true
        } catch (e: IOException) {
            // Retried on the next visit
        } catch (e: HttpException) {
            // Retried on the next visit
        }
    }

    private fun NoveltiesFeed.hasMorePages() = feed.number + 1 < feed.totalPages

    // Files whose reasoning is gone can't be opened
    private fun List<NoveltyItem>.openable() = filter { it.reasoningId != null }

    companion object {
        const val PAGE_SIZE = 20
    }
}
