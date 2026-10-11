package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.FileRatingSummary
import com.example.medrational_android.data.model.RateFileRequest
import com.example.medrational_android.data.rating.FileRatingStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode

sealed interface FileRatingUiState {
    data object Loading : FileRatingUiState
    data class Success(
        val summary: FileRatingSummary,
        // A rate/remove request is in flight; the stars ignore taps until it settles
        val isSubmitting: Boolean = false,
        // One-shot error to show, cleared by onMessageShown()
        val message: String? = null
    ) : FileRatingUiState
    data class Error(val message: String) : FileRatingUiState
}

/**
 * The signed-in user's rating of one file. Changes go to the shared [FileRatingStore] first
 * (optimistically) so every screen showing the file updates at once, then to the backend; a failed
 * request puts the previous rating back.
 */
class FileRatingViewModel(
    private val api: MedRationalApi,
    private val store: FileRatingStore = FileRatingStore.shared
) : ViewModel() {

    private sealed interface Load {
        data object Loading : Load
        data object Loaded : Load
        data class Failed(val message: String) : Load
    }

    private var fileId: Long? = null
    private val load = MutableStateFlow<Load>(Load.Loading)
    private val submitting = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    // Stays Loading until this user's own score arrives, so a score cached for someone else never shows
    val uiState: StateFlow<FileRatingUiState> =
        combine(load, submitting, message, store.ratings) { load, submitting, message, ratings ->
            when (load) {
                Load.Loading -> FileRatingUiState.Loading
                is Load.Failed -> FileRatingUiState.Error(load.message)
                Load.Loaded -> fileId?.let { ratings[it] }
                    ?.let { FileRatingUiState.Success(it, submitting, message) }
                    ?: FileRatingUiState.Loading
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, FileRatingUiState.Loading)

    fun load(fileId: Long) {
        this.fileId = fileId
        load.value = Load.Loading
        viewModelScope.launch {
            load.value = try {
                val response = api.getFileRating(fileId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    store.put(body)
                    Load.Loaded
                } else {
                    Load.Failed("Couldn't load ratings (HTTP ${response.code()}).")
                }
            } catch (e: IOException) {
                Load.Failed("Network error while loading ratings.")
            } catch (e: HttpException) {
                Load.Failed("Couldn't load ratings (HTTP ${e.code()}).")
            }
        }
    }

    fun rate(score: Int) {
        require(score in 1..5) { "score must be 1-5, was $score" }
        val current = currentSummary() ?: return
        if (current.userRating == score) return
        submit(withUserRating(current, score)) { api.rateFile(current.fileId, RateFileRequest(score)) }
    }

    fun removeRating() {
        val current = currentSummary() ?: return
        if (current.userRating == null) return
        submit(withUserRating(current, null)) { api.removeFileRating(current.fileId) }
    }

    fun onMessageShown() {
        message.value = null
    }

    // Null while loading or while another change is in flight, so requests can't race
    private fun currentSummary(): FileRatingSummary? {
        if (load.value != Load.Loaded || submitting.value) return null
        return fileId?.let { store[it] }
    }

    private fun submit(optimistic: FileRatingSummary, request: suspend () -> Response<FileRatingSummary>) {
        val previous = store[optimistic.fileId] ?: return
        submitting.value = true
        store.put(optimistic)
        viewModelScope.launch {
            val error = try {
                val response = request()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    store.put(body)
                    null
                } else {
                    "Couldn't save your rating (HTTP ${response.code()})."
                }
            } catch (e: IOException) {
                "Network error. Your rating wasn't saved."
            } catch (e: HttpException) {
                "Couldn't save your rating (HTTP ${e.code()})."
            }
            if (error != null) {
                store.put(previous)
                message.value = error
            }
            submitting.value = false
        }
    }
}

/**
 * What the summary becomes once the user's score is [newScore] (null removes it), worked out
 * locally from the rounded average. The server's exact figures replace it when the request returns.
 */
internal fun withUserRating(summary: FileRatingSummary, newScore: Int?): FileRatingSummary {
    val oldScore = summary.userRating
    var total = summary.totalRatings
    var sum = summary.avgRating * summary.totalRatings
    if (oldScore != null && total > 0) {
        total -= 1
        sum -= oldScore
    }
    if (newScore != null) {
        total += 1
        sum += newScore
    }
    // The clamp absorbs the rounding error carried by the average
    val average = if (total > 0) roundToOneDecimal(sum / total).coerceIn(1.0, 5.0) else 0.0
    return summary.copy(avgRating = average, totalRatings = total, userRating = newScore)
}

private fun roundToOneDecimal(value: Double): Double =
    BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toDouble()
