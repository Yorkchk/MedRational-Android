package com.example.medrational_android.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkshopProjectGsonTest {

    private val gson = Gson()

    // As WorkshopProjectResponseDTO is serialized by the backend for a student before opening
    private val upcomingJson = """
        {"id":3,"authorId":1,"authorEmail":"admin@example.com","title":"Suturing workshop",
         "description":"Hands-on","projectType":"FIRST_COME_FIRST_SERVED","status":"UPCOMING",
         "formLink":null,"capacity":null,"registrationOpensAt":"2026-10-20T09:00:00",
         "registrationClosesAt":"2026-10-21T18:00:00","createdAt":"2026-10-11T10:00:00.123456",
         "attachments":[{"id":8,"fileName":"program.pdf","fileUrl":"https://cdn.example.com/WorkshopAttachments/Project_3/program.pdf"}]}
    """.trimIndent()

    @Test
    fun `backend project is parsed with its enums, dates and attachments`() {
        val project = gson.fromJson(upcomingJson, WorkshopProject::class.java)

        assertEquals(ProjectType.FIRST_COME_FIRST_SERVED, project.projectType)
        assertEquals(ProjectStatus.UPCOMING, project.status)
        assertEquals("2026-10-20T09:00:00", project.registrationOpensAt)
        assertEquals(WorkshopAttachment(8, "program.pdf", "https://cdn.example.com/WorkshopAttachments/Project_3/program.pdf"), project.attachments.single())
    }

    @Test
    fun `hidden link and unlimited capacity are null`() {
        val project = gson.fromJson(upcomingJson, WorkshopProject::class.java)

        assertNull(project.formLink)
        assertNull(project.capacity)
    }

    @Test
    fun `create request carries the fields the backend validates`() {
        val json = gson.toJson(
            CreateWorkshopProjectRequest(
                title = "Suturing", description = "Hands-on", projectType = ProjectType.FIRST_COME_FIRST_SERVED,
                formLink = "https://forms.example.com", capacity = 30,
                registrationOpensAt = "2026-10-20T09:00:00", registrationClosesAt = "2026-10-21T18:00:00"
            )
        )

        assertEquals(
            """{"title":"Suturing","description":"Hands-on","projectType":"FIRST_COME_FIRST_SERVED",""" +
                """"formLink":"https://forms.example.com","capacity":30,""" +
                """"registrationOpensAt":"2026-10-20T09:00:00","registrationClosesAt":"2026-10-21T18:00:00"}""",
            json
        )
    }
}
