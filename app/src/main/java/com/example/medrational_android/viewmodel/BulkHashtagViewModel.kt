package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.R
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.AttachHashtagsRequest
import com.example.medrational_android.data.model.Hashtag
import com.example.medrational_android.ui.text.UiText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/** A tag carried by some of the selected files; [hashtagIdByFile] maps file id to hashtag id. */
data class BulkTag(val name: String, val hashtagIdByFile: Map<Long, Long>)

sealed interface BulkHashtagUiState {
    data object Idle : BulkHashtagUiState
    data object LoadingTags : BulkHashtagUiState
    data class RemoveChoices(val fileCount: Int, val tags: List<BulkTag>) : BulkHashtagUiState
    data object Working : BulkHashtagUiState
}

/** Adds or removes hashtags on several files at once, one request per file since the API is per file. */
class BulkHashtagViewModel(private val api: MedRationalApi) : ViewModel() {

    private val _uiState = MutableStateFlow<BulkHashtagUiState>(BulkHashtagUiState.Idle)
    val uiState: StateFlow<BulkHashtagUiState> = _uiState.asStateFlow()

    // One-shot result summary, cleared by onMessageShown()
    private val _message = MutableStateFlow<UiText?>(null)
    val message: StateFlow<UiText?> = _message.asStateFlow()

    fun addTags(fileIds: Set<Long>, names: List<String>) {
        if (fileIds.isEmpty() || names.isEmpty()) return
        _uiState.value = BulkHashtagUiState.Working
        viewModelScope.launch {
            val failures = countFailures(fileIds) { fileId ->
                api.attachHashtags(fileId, AttachHashtagsRequest(names)).isSuccessful
            }
            _message.value = if (failures == 0) {
                UiText.Plural(R.plurals.bulk_tags_added, names.size, listOf(names.size, tagCount(names.size), fileCount(fileIds.size)))
            } else {
                UiText.Res(R.string.bulk_tag_failed, listOf(failures, fileCount(fileIds.size)))
            }
            _uiState.value = BulkHashtagUiState.Idle
        }
    }

    // Collects the tags on the selected files so the admin can pick which to remove
    fun loadTagsFor(fileIds: Set<Long>) {
        if (fileIds.isEmpty()) return
        _uiState.value = BulkHashtagUiState.LoadingTags
        viewModelScope.launch {
            val results = coroutineScope {
                fileIds.map { fileId -> async { fileId to fetchHashtags(fileId) } }.awaitAll()
            }
            if (results.any { it.second == null }) {
                _message.value = UiText.Res(R.string.bulk_load_failed)
                _uiState.value = BulkHashtagUiState.Idle
                return@launch
            }
            val tags = results
                .flatMap { (fileId, hashtags) -> hashtags.orEmpty().map { it.name to (fileId to it.id) } }
                .groupBy({ it.first }, { it.second })
                .map { (name, pairs) -> BulkTag(name, pairs.toMap()) }
                .sortedWith(compareByDescending<BulkTag> { it.hashtagIdByFile.size }.thenBy { it.name })
            _uiState.value = BulkHashtagUiState.RemoveChoices(fileIds.size, tags)
        }
    }

    fun removeTags(tags: List<BulkTag>) {
        val pairs = tags.flatMap { tag -> tag.hashtagIdByFile.map { (fileId, hashtagId) -> fileId to hashtagId } }
        if (pairs.isEmpty()) {
            dismiss()
            return
        }
        _uiState.value = BulkHashtagUiState.Working
        viewModelScope.launch {
            val failures = countFailures(pairs) { (fileId, hashtagId) ->
                api.detachHashtag(fileId, hashtagId).isSuccessful
            }
            _message.value = if (failures == 0) {
                val files = pairs.map { it.first }.distinct().size
                UiText.Plural(R.plurals.bulk_tags_removed, tags.size, listOf(tags.size, tagCount(tags.size), fileCount(files)))
            } else {
                UiText.Res(R.string.bulk_remove_failed, listOf(failures, tagCount(pairs.size)))
            }
            _uiState.value = BulkHashtagUiState.Idle
        }
    }

    fun dismiss() {
        _uiState.value = BulkHashtagUiState.Idle
    }

    fun onMessageShown() {
        _message.value = null
    }

    private suspend fun fetchHashtags(fileId: Long): List<Hashtag>? = try {
        val response = api.getFileHashtags(fileId)
        if (response.isSuccessful) response.body() else null
    } catch (e: IOException) {
        null
    } catch (e: HttpException) {
        null
    }

    // Sequential on purpose: parallel attaches of a brand-new tag would each try to create it on the
    // backend, and parallel detaches race on its "delete the tag once unused" check
    private suspend fun <T> countFailures(items: Collection<T>, request: suspend (T) -> Boolean): Int =
        items.count { item ->
            val succeeded = try {
                request(item)
            } catch (e: IOException) {
                false
            } catch (e: HttpException) {
                false
            }
            !succeeded
        }

    private fun tagCount(count: Int) = UiText.Plural(R.plurals.hashtag_count, count)

    private fun fileCount(count: Int) = UiText.Plural(R.plurals.file_count, count)
}
