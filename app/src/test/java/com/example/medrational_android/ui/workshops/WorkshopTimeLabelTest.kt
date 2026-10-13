package com.example.medrational_android.ui.workshops

import com.example.medrational_android.data.model.ProjectStatus
import com.example.medrational_android.data.model.ProjectType
import com.example.medrational_android.data.model.WorkshopProject
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkshopTimeLabelTest {

    private val project = WorkshopProject(
        id = 1, authorId = 1, authorEmail = "admin@example.com", title = "Suturing", description = "Hands-on",
        projectType = ProjectType.MOTIVATION, status = ProjectStatus.UPCOMING, formLink = null,
        registrationOpensAt = "2026-10-20T07:05:00", registrationClosesAt = "2026-10-21T19:30:00",
        createdAt = null
    )

    // Always 24-hour, whatever the phone's setting
    @Test
    fun `workshop times are shown in 24-hour format`() {
        assertEquals("20 Oct 2026, 07:05", project.opensLabel(Locale.ENGLISH))
        assertEquals("21 Oct 2026, 19:30", project.closesLabel(Locale.ENGLISH))
    }

    @Test
    fun `workshop times use French month names in French`() {
        assertEquals("20 oct. 2026, 07:05", project.opensLabel(Locale.FRENCH))
    }
}
