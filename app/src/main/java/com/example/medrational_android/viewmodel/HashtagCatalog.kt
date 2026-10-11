package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.HashtagUsage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * Every hashtag in use, for autocomplete. Backed by an admin-only endpoint, so only create this
 * for admins. Suggestions are a convenience: failures keep whatever list was already loaded.
 */
class HashtagCatalogViewModel(private val api: MedRationalApi) : ViewModel() {

    private val _catalog = MutableStateFlow<List<HashtagUsage>>(emptyList())
    val catalog: StateFlow<List<HashtagUsage>> = _catalog.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            try {
                val response = api.getHashtagUsage()
                val body = response.body()
                if (response.isSuccessful && body != null) _catalog.value = body
            } catch (_: IOException) {
            } catch (_: HttpException) {
            }
        }
    }
}

/**
 * Tags starting with the query come before tags merely containing it; within each group the most
 * used come first. An empty query returns the most used tags.
 */
fun suggestHashtags(
    catalog: List<HashtagUsage>,
    query: String,
    exclude: Set<String> = emptySet(),
    max: Int = 8
): List<HashtagUsage> {
    val term = sanitizeHashtag(query)
    return catalog
        .filter { it.hashtag !in exclude && term in it.hashtag }
        .sortedWith(
            compareBy<HashtagUsage> { !it.hashtag.startsWith(term) }
                .thenByDescending { it.attachedFilesCount }
                .thenBy { it.hashtag }
        )
        .take(max)
}
