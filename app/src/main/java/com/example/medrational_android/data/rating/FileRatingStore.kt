package com.example.medrational_android.data.rating

import com.example.medrational_android.data.model.FileRatingSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Latest known rating of every file the app has seen, shared by all screens so rating a file on
 * its detail screen updates every list showing it without a refetch. Lists [seed] it with what
 * they loaded and read `ratings[file.id]`; the rating screen [put]s each server answer.
 */
class FileRatingStore {

    private val _ratings = MutableStateFlow<Map<Long, FileRatingSummary>>(emptyMap())
    val ratings: StateFlow<Map<Long, FileRatingSummary>> = _ratings.asStateFlow()

    operator fun get(fileId: Long): FileRatingSummary? = _ratings.value[fileId]

    fun put(summary: FileRatingSummary) {
        _ratings.update { it + (summary.fileId to summary) }
    }

    // Fresh averages and counts from a list response. Lists don't carry the user's own score, so
    // the known one is kept
    fun seed(summaries: Collection<FileRatingSummary>) {
        if (summaries.isEmpty()) return
        _ratings.update { current ->
            current + summaries.map { it.fileId to it.copy(userRating = current[it.fileId]?.userRating) }
        }
    }

    // Holds the signed-in user's scores, so it is emptied on sign-out
    fun clear() {
        _ratings.value = emptyMap()
    }

    companion object {
        val shared = FileRatingStore()
    }
}
