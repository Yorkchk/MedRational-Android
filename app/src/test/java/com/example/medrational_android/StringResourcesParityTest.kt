package com.example.medrational_android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * French (res/values, the default) and English (res/values-en) must offer the same strings with
 * the same placeholders: a missing one shows the other language, and a mismatched %1$s crashes.
 */
class StringResourcesParityTest {

    private val french = TestStrings.french
    private val english = TestStrings.english

    private fun placeholders(text: String): List<String> =
        Regex("""%(\d+\$)?[sdf]""").findAll(text).map { it.value }.sorted().toList()

    @Test
    fun `both languages have the same strings`() {
        assertTrue("No strings found in ${TestStrings.resDir.absolutePath}", french.strings.size > 100)
        assertEquals(emptySet<String>(), french.strings.keys - english.strings.keys - "app_name")
        assertEquals(emptySet<String>(), english.strings.keys - french.strings.keys)
        assertEquals(french.plurals.keys, english.plurals.keys)
    }

    @Test
    fun `translations keep the same placeholders`() {
        french.strings.forEach { (name, text) ->
            val translation = english.strings[name] ?: return@forEach
            assertEquals(name, placeholders(text), placeholders(translation))
        }
        french.plurals.forEach { (name, forms) ->
            assertEquals(name, placeholders(forms.getValue("other")), placeholders(english.plurals.getValue(name).getValue("other")))
        }
    }

    // Workshop pushes name these keys (WorkshopNotificationServiceImpl.pushLocalizationKey in the backend)
    @Test
    fun `every workshop push has a title and a body`() {
        listOf("opening_soon", "opened", "closing_soon", "closed").forEach { event ->
            listOf("fcfs", "motivation").forEach { type ->
                listOf("title", "body").forEach { part ->
                    val key = "push_${event}_${type}_$part"
                    assertTrue(key, french.strings[key]?.contains("%1\$s") == true)
                    assertTrue(key, english.strings[key]?.contains("%1\$s") == true)
                }
            }
        }
    }
}
