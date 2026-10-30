package com.example.medrational_android.ui.search

import com.example.medrational_android.data.model.Category
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.model.StudyFile
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickFilterTest {

    private val cardio = Category(id = 1, name = "Cardiologie", description = "Douleur thoracique, dyspnée")
    private val pneumo = Category(id = 2, name = "Pneumologie", description = null)
    private val categories = listOf(cardio, pneumo)

    private fun file(name: String) =
        StudyFile(id = 9, reasoningId = 1, fileName = name, fileType = null, publicUrl = "", fileSizeBytes = null)

    private val chestPain = Reasoning(id = 1, categoryId = 1, title = "Douleur thoracique", content = "Éliminer un SCA", files = listOf(file("ECG-lecture.pdf")))
    private val syncope = Reasoning(id = 2, categoryId = 1, title = "Syncope", content = null, files = null)
    private val reasonings = listOf(chestPain, syncope)

    @Test
    fun `accents and case are ignored`() {
        assertEquals("dyspnee", normalizeForSearch("  Dyspnée "))
        assertEquals(listOf(cardio), filterCategories(categories, "DYSPNEE"))
    }

    @Test
    fun `categories match on their name`() {
        assertEquals(listOf(pneumo), filterCategories(categories, "pneumo"))
    }

    @Test
    fun `categories match on their description`() {
        assertEquals(listOf(cardio), filterCategories(categories, "thoracique"))
    }

    @Test
    fun `a blank query keeps every category`() {
        assertEquals(categories, filterCategories(categories, "  "))
    }

    @Test
    fun `nothing matching gives an empty list`() {
        assertEquals(emptyList<Category>(), filterCategories(categories, "dermato"))
        assertEquals(emptyList<Reasoning>(), filterReasonings(reasonings, "dermato"))
    }

    @Test
    fun `reasonings match on their title and text`() {
        assertEquals(listOf(syncope), filterReasonings(reasonings, "syncope"))
        assertEquals(listOf(chestPain), filterReasonings(reasonings, "eliminer"))
    }

    @Test
    fun `reasonings match on the name of one of their files`() {
        assertEquals(listOf(chestPain), filterReasonings(reasonings, "ecg"))
    }

    @Test
    fun `a blank query keeps every reasoning`() {
        assertEquals(reasonings, filterReasonings(reasonings, ""))
    }
}
