package com.example.medrational_android.ui.workshops

import com.example.medrational_android.data.model.ProjectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectTypeLabelTest {

    @Test
    fun `each type has its own name`() {
        assertEquals("First come, first served", ProjectType.FIRST_COME_FIRST_SERVED.label())
        assertEquals("Motivation letter", ProjectType.MOTIVATION.label())
    }

    // The project page used to say "first come, first served" whatever the type
    @Test
    fun `a motivation project explains the committee's selection`() {
        assertTrue(ProjectType.MOTIVATION.description().contains("motivation letter"))
        assertTrue(ProjectType.MOTIVATION.description().contains("committee"))
        assertTrue(ProjectType.FIRST_COME_FIRST_SERVED.description().contains("first to sign up"))
    }

    // The "Project type" line already names the type, so the sentence under it doesn't repeat it
    @Test
    fun `descriptions don't repeat the type name`() {
        ProjectType.entries.forEach { type ->
            assertTrue(type.description(), !type.description().startsWith(type.label()))
        }
    }
}
