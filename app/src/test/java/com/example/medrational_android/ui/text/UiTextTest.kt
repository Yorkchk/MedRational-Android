package com.example.medrational_android.ui.text

import android.content.Context
import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class UiTextTest {

    private val resources: Resources = mockk {
        every { getQuantityString(PLURAL_FILES, 3, 3) } returns "3 files"
    }
    private val context: Context = mockk {
        every { getString(STRING_PLAIN) } returns "Saved"
        every { getString(STRING_ADDED, "#ecg", "3 files") } returns "Added #ecg to 3 files"
        every { this@mockk.resources } returns this@UiTextTest.resources
    }

    @Test
    fun `a string is read when shown`() {
        assertEquals("Saved", UiText.Res(STRING_PLAIN).asString(context))
    }

    // e.g. "Added %1$s to %2$s" with "3 files", itself a plural
    @Test
    fun `arguments can be text themselves`() {
        val text = UiText.Res(STRING_ADDED, listOf("#ecg", UiText.Plural(PLURAL_FILES, 3)))

        assertEquals("Added #ecg to 3 files", text.asString(context))
    }

    private companion object {
        const val STRING_PLAIN = 1
        const val STRING_ADDED = 2
        const val PLURAL_FILES = 3
    }
}
