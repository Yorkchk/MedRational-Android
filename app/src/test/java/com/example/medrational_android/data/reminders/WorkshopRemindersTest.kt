package com.example.medrational_android.data.reminders

import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.PageResponse
import com.example.medrational_android.data.model.ProjectStatus
import com.example.medrational_android.data.model.ProjectType
import com.example.medrational_android.data.model.WorkshopProject
import com.example.medrational_android.data.model.parseIsoLocalDateTime
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class WorkshopRemindersTest {

    private val opensAt = "2026-10-20T09:00:00"
    private val closesAt = "2026-10-20T18:00:00"
    private val opensMillis = parseIsoLocalDateTime(opensAt)!!
    private val closesMillis = parseIsoLocalDateTime(closesAt)!!

    private fun project(status: ProjectStatus, id: Long = 4) = WorkshopProject(
        id = id, authorId = 1, authorEmail = null, title = "Suturing", description = "Hands-on",
        projectType = ProjectType.FIRST_COME_FIRST_SERVED, status = status, formLink = null,
        registrationOpensAt = opensAt, registrationClosesAt = closesAt, createdAt = null
    )

    private val tenMinutes = 10 * 60 * 1000L

    @Test
    fun `upcoming project gets reminders ten minutes before opening and closing`() {
        val reminders = reminderTimes(project(ProjectStatus.UPCOMING), nowMillis = opensMillis - 3_600_000)

        assertEquals(
            listOf(ReminderKind.OPENING to opensMillis - tenMinutes, ReminderKind.CLOSING to closesMillis - tenMinutes),
            reminders.map { it.kind to it.triggerAtMillis }
        )
    }

    @Test
    fun `open project only gets the closing reminder`() {
        val reminders = reminderTimes(project(ProjectStatus.OPEN), nowMillis = opensMillis)

        assertEquals(listOf(ReminderKind.CLOSING), reminders.map { it.kind })
    }

    @Test
    fun `closed project gets none`() {
        assertTrue(reminderTimes(project(ProjectStatus.CLOSED), nowMillis = 0).isEmpty())
    }

    @Test
    fun `reminders already due are dropped`() {
        // Five minutes before opening: the opening reminder's moment has passed
        val reminders = reminderTimes(project(ProjectStatus.UPCOMING), nowMillis = opensMillis - 5 * 60 * 1000)

        assertEquals(listOf(ReminderKind.CLOSING), reminders.map { it.kind })
    }

    @Test
    fun `each project and kind has its own alarm slot`() {
        val codes = listOf(1L, 2L).flatMap { id -> ReminderKind.entries.map { reminderRequestCode(id, it) } }

        assertEquals(codes.size, codes.toSet().size)
        assertNotEquals(reminderRequestCode(1, ReminderKind.OPENING), reminderRequestCode(1, ReminderKind.CLOSING))
    }

    @Test
    fun `notification text names the project`() {
        assertTrue(reminderText("Suturing", ReminderKind.OPENING).contains("\"Suturing\" opens in 10 minutes"))
        assertTrue(reminderText("Suturing", ReminderKind.CLOSING).contains("\"Suturing\" closes in 10 minutes"))
    }

    // --- startup sync ------------------------------------------------------------------------------

    private val api: MedRationalApi = mockk()
    private val scheduler: WorkshopReminderScheduler = mockk(relaxed = true)

    private fun page(vararg items: WorkshopProject) =
        Response.success(PageResponse(content = items.toList(), totalElements = items.size.toLong(), totalPages = 1, number = 0))

    @Test
    fun `sync replaces the reminders with every upcoming and open project`() = runTest {
        coEvery { api.getWorkshops(ProjectStatus.UPCOMING, any(), any()) } returns page(project(ProjectStatus.UPCOMING, id = 1))
        coEvery { api.getWorkshops(ProjectStatus.OPEN, any(), any()) } returns page(project(ProjectStatus.OPEN, id = 2))

        assertTrue(syncWorkshopReminders(api, scheduler))

        verify { scheduler.replaceAll(listOf(project(ProjectStatus.UPCOMING, id = 1), project(ProjectStatus.OPEN, id = 2))) }
    }

    @Test
    fun `sync keeps the existing reminders when offline`() = runTest {
        coEvery { api.getWorkshops(any(), any(), any()) } throws IOException("offline")

        assertFalse(syncWorkshopReminders(api, scheduler))

        verify(exactly = 0) { scheduler.replaceAll(any()) }
    }

    @Test
    fun `sync keeps the existing reminders when the backend fails`() = runTest {
        coEvery { api.getWorkshops(ProjectStatus.UPCOMING, any(), any()) } returns page()
        coEvery { api.getWorkshops(ProjectStatus.OPEN, any(), any()) } returns Response.error(500, "".toResponseBody(null))

        assertFalse(syncWorkshopReminders(api, scheduler))

        // An empty list here would wrongly cancel every reminder
        verify(exactly = 0) { scheduler.replaceAll(any()) }
    }
}
