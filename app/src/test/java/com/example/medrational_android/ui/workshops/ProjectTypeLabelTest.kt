package com.example.medrational_android.ui.workshops

import com.example.medrational_android.TestStrings
import com.example.medrational_android.data.model.ProjectType
import com.example.medrational_android.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectTypeLabelTest {

    private fun en(id: Int) = TestStrings.en(UiText.Res(id))!!

    private fun fr(id: Int) = TestStrings.fr(UiText.Res(id))!!

    @Test
    fun `each type has its own name`() {
        assertEquals("First come, first served", en(ProjectType.FIRST_COME_FIRST_SERVED.label()))
        assertEquals("Motivation letter", en(ProjectType.MOTIVATION.label()))
        assertEquals("Premier arrivé, premier servi", fr(ProjectType.FIRST_COME_FIRST_SERVED.label()))
        assertEquals("Lettre de motivation", fr(ProjectType.MOTIVATION.label()))
    }

    // The project page used to say "first come, first served" whatever the type
    @Test
    fun `a motivation project explains the committee's selection`() {
        assertTrue(en(ProjectType.MOTIVATION.description()).contains("motivation letter"))
        assertTrue(en(ProjectType.MOTIVATION.description()).contains("committee"))
        assertTrue(en(ProjectType.FIRST_COME_FIRST_SERVED.description()).contains("first to sign up"))
        assertTrue(fr(ProjectType.MOTIVATION.description()).contains("comité"))
    }

    // The "Project type" line already names the type, so the sentence under it doesn't repeat it
    @Test
    fun `descriptions don't repeat the type name`() {
        ProjectType.entries.forEach { type ->
            assertTrue(en(type.description()), !en(type.description()).startsWith(en(type.label())))
            assertTrue(fr(type.description()), !fr(type.description()).startsWith(fr(type.label())))
        }
    }
}
