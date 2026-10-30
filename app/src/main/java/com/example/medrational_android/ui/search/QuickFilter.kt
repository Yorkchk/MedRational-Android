package com.example.medrational_android.ui.search

import com.example.medrational_android.data.model.Category
import com.example.medrational_android.data.model.Reasoning
import java.text.Normalizer

// Quick search of a list that is already loaded (home categories, a category's reasonings),
// as opposed to the Search screen, which asks the backend for files

private val combiningMarks = Regex("\\p{Mn}+")

/** Lowercase, without accents, so "dyspnee" finds "Dyspnée". */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD).replace(combiningMarks, "")

private fun String?.matches(normalizedQuery: String): Boolean =
    this != null && normalizeForSearch(this).contains(normalizedQuery)

/** Categories whose name or description contains [query]; all of them when it is blank. */
fun filterCategories(categories: List<Category>, query: String): List<Category> {
    val q = normalizeForSearch(query)
    if (q.isEmpty()) return categories
    return categories.filter { it.name.matches(q) || it.description.matches(q) }
}

/** Reasonings whose title, text or one of whose files contains [query]; all of them when it is blank. */
fun filterReasonings(reasonings: List<Reasoning>, query: String): List<Reasoning> {
    val q = normalizeForSearch(query)
    if (q.isEmpty()) return reasonings
    return reasonings.filter { reasoning ->
        reasoning.title.matches(q) ||
            reasoning.content.matches(q) ||
            reasoning.files.orEmpty().any { it.fileName.matches(q) }
    }
}
