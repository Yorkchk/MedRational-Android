package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.CreateWorkshopProjectRequest
import com.example.medrational_android.data.model.PageResponse
import com.example.medrational_android.data.model.ProjectStatus
import com.example.medrational_android.data.model.ProjectType
import com.example.medrational_android.data.model.WorkshopAttachment
import com.example.medrational_android.data.model.WorkshopProject
import com.example.medrational_android.data.model.parseIsoLocalDateTime
import com.example.medrational_android.data.reminders.WorkshopReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class WorkshopViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()
    private val reminders: WorkshopReminderScheduler = mockk(relaxed = true)

    private fun <T> error(code: Int): Response<T> = Response.error(code, "".toResponseBody(null))

    private fun project(
        id: Long = ID,
        status: ProjectStatus = ProjectStatus.UPCOMING,
        formLink: String? = null,
        attachments: List<WorkshopAttachment> = emptyList()
    ) = WorkshopProject(
        id = id, authorId = 1, authorEmail = "admin@example.com", title = "Suturing $id", description = "Hands-on",
        projectType = ProjectType.FIRST_COME_FIRST_SERVED, status = status, formLink = formLink, capacity = 30,
        registrationOpensAt = "2026-10-20T09:00:00", registrationClosesAt = "2026-10-21T18:00:00",
        createdAt = null, attachments = attachments
    )

    private fun page(vararg items: WorkshopProject) =
        Response.success(PageResponse(content = items.toList(), totalElements = items.size.toLong(), totalPages = 1, number = 0))

    // --- list --------------------------------------------------------------------------------------

    @Test
    fun `list loads every project and schedules their reminders`() {
        coEvery { api.getWorkshops(null, any(), any()) } returns page(project(1), project(2))

        val viewModel = WorkshopListViewModel(api, reminders)
        viewModel.load()

        assertEquals(WorkshopListUiState.Success(listOf(project(1), project(2))), viewModel.uiState.value)
        verify { reminders.schedule(listOf(project(1), project(2))) }
    }

    @Test
    fun `a tab filters by status`() {
        coEvery { api.getWorkshops(null, any(), any()) } returns page(project(1))
        coEvery { api.getWorkshops(ProjectStatus.OPEN, any(), any()) } returns page(project(2, ProjectStatus.OPEN))

        val viewModel = WorkshopListViewModel(api, reminders)
        viewModel.load()
        viewModel.selectTab(ProjectStatus.OPEN)

        assertEquals(ProjectStatus.OPEN, viewModel.tab.value)
        assertEquals(WorkshopListUiState.Success(listOf(project(2, ProjectStatus.OPEN))), viewModel.uiState.value)
    }

    @Test
    fun `list network failure is an Error and leaves the reminders alone`() {
        coEvery { api.getWorkshops(any(), any(), any()) } throws IOException("offline")

        val viewModel = WorkshopListViewModel(api, reminders)
        viewModel.load()

        assertTrue(viewModel.uiState.value is WorkshopListUiState.Error)
        verify(exactly = 0) { reminders.schedule(any()) }
    }

    // --- detail ------------------------------------------------------------------------------------

    private fun loadedDetail(project: WorkshopProject = project()): WorkshopDetailViewModel {
        coEvery { api.getWorkshop(project.id) } returns Response.success(project)
        return WorkshopDetailViewModel(api, reminders).also { it.load(project.id) }
    }

    @Test
    fun `registration link only shows while open`() {
        val link = "https://forms.example.com/suturing"

        val open = loadedDetail(project(status = ProjectStatus.OPEN, formLink = link)).uiState.value
        // Admins receive the link in every status; it still mustn't be offered as a sign-up button
        val upcoming = loadedDetail(project(status = ProjectStatus.UPCOMING, formLink = link)).uiState.value
        val closed = loadedDetail(project(status = ProjectStatus.CLOSED, formLink = link)).uiState.value

        assertEquals(link, (open as WorkshopDetailUiState.Success).registrationLink)
        assertNull((upcoming as WorkshopDetailUiState.Success).registrationLink)
        assertNull((closed as WorkshopDetailUiState.Success).registrationLink)
    }

    @Test
    fun `open project without a link offers no registration`() {
        val state = loadedDetail(project(status = ProjectStatus.OPEN, formLink = null)).uiState.value

        assertNull((state as WorkshopDetailUiState.Success).registrationLink)
    }

    @Test
    fun `loading a project refreshes its reminders`() {
        loadedDetail()

        verify { reminders.schedule(listOf(project())) }
    }

    @Test
    fun `a project deleted meanwhile loses its reminders`() {
        coEvery { api.getWorkshop(ID) } returns error(404)

        val viewModel = WorkshopDetailViewModel(api, reminders)
        viewModel.load(ID)

        assertEquals(WorkshopDetailUiState.Error("This project no longer exists."), viewModel.uiState.value)
        verify { reminders.cancel(ID) }
    }

    @Test
    fun `status override shows the new status and reschedules`() {
        val viewModel = loadedDetail()
        val closed = project(status = ProjectStatus.CLOSED)
        coEvery { api.updateWorkshopStatus(ID, ProjectStatus.CLOSED) } returns Response.success(closed)

        viewModel.changeStatus(ProjectStatus.CLOSED)

        assertEquals(WorkshopDetailUiState.Success(closed), viewModel.uiState.value)
        verify { reminders.schedule(listOf(closed)) }
    }

    @Test
    fun `failed status override keeps the project and explains`() {
        val viewModel = loadedDetail()
        coEvery { api.updateWorkshopStatus(ID, any()) } throws IOException("offline")

        viewModel.changeStatus(ProjectStatus.OPEN)

        assertEquals(WorkshopDetailUiState.Success(project()), viewModel.uiState.value)
        assertTrue(viewModel.message.value!!.startsWith("Network error"))
    }

    @Test
    fun `deleting goes back and cancels the reminders`() {
        val viewModel = loadedDetail()
        coEvery { api.deleteWorkshop(ID) } returns Response.success(Unit)

        viewModel.delete()

        assertEquals(WorkshopDetailUiState.Deleted, viewModel.uiState.value)
        verify { reminders.cancel(ID) }
    }

    @Test
    fun `failed delete keeps the project`() {
        val viewModel = loadedDetail()
        coEvery { api.deleteWorkshop(ID) } returns error(500)

        viewModel.delete()

        assertEquals(WorkshopDetailUiState.Success(project()), viewModel.uiState.value)
        assertEquals("Something went wrong (HTTP 500). Please try again.", viewModel.message.value)
        verify(exactly = 0) { reminders.cancel(any()) }
    }

    @Test
    fun `uploaded attachment is added to the project`() {
        val viewModel = loadedDetail()
        val part = MultipartBody.Part.createFormData("file", "program.pdf", "pdf".toRequestBody())
        val attachment = WorkshopAttachment(8, "program.pdf", "https://cdn.example.com/program.pdf")
        coEvery { api.uploadWorkshopAttachment(ID, part) } returns Response.success(attachment)

        viewModel.uploadAttachment(part)

        assertEquals(WorkshopDetailUiState.Success(project(attachments = listOf(attachment))), viewModel.uiState.value)
    }

    @Test
    fun `too large attachment is explained`() {
        val viewModel = loadedDetail()
        coEvery { api.uploadWorkshopAttachment(ID, any()) } returns error(413)

        viewModel.uploadAttachment(MultipartBody.Part.createFormData("file", "big.pdf", "x".toRequestBody()))

        assertEquals("Files must be 10 MB or smaller.", viewModel.message.value)
        assertEquals(WorkshopDetailUiState.Success(project()), viewModel.uiState.value)
    }

    @Test
    fun `deleted attachment is removed from the project`() {
        val keep = WorkshopAttachment(1, "keep.pdf", "https://cdn.example.com/keep.pdf")
        val drop = WorkshopAttachment(2, "drop.pdf", "https://cdn.example.com/drop.pdf")
        val viewModel = loadedDetail(project(attachments = listOf(keep, drop)))
        coEvery { api.deleteWorkshopAttachment(2) } returns Response.success(Unit)

        viewModel.deleteAttachment(2)

        assertEquals(WorkshopDetailUiState.Success(project(attachments = listOf(keep))), viewModel.uiState.value)
    }

    // --- new project -------------------------------------------------------------------------------

    private val opens = parseIsoLocalDateTime("2026-10-20T09:00:00")!!
    private val closes = parseIsoLocalDateTime("2026-10-21T18:00:00")!!

    private fun form(
        formLink: String = "https://forms.example.com/suturing",
        capacity: String = "30",
        opensAt: Long? = opens,
        closesAt: Long? = closes
    ) = WorkshopForm(" Suturing ", " Hands-on ", formLink, capacity, opensAt, closesAt)

    @Test
    fun `valid form is accepted`() {
        assertNull(validateWorkshopForm(form()))
        assertNull(validateWorkshopForm(form(capacity = "")))
    }

    @Test
    fun `invalid forms are rejected with a reason`() {
        assertEquals("Enter a title.", validateWorkshopForm(form().copy(title = " ")))
        assertEquals("Enter the registration link.", validateWorkshopForm(form(formLink = "")))
        assertEquals(
            "The registration link must start with http:// or https://.",
            validateWorkshopForm(form(formLink = "forms.example.com"))
        )
        assertEquals(
            "Capacity must be a positive number, or left empty for no limit.",
            validateWorkshopForm(form(capacity = "0"))
        )
        assertEquals("Pick when registration opens.", validateWorkshopForm(form(opensAt = null)))
        assertEquals("Registration must close after it opens.", validateWorkshopForm(form(closesAt = opens)))
    }

    @Test
    fun `invalid form makes no request`() {
        val viewModel = NewWorkshopViewModel(api, reminders, authorId = 9)

        viewModel.submit(form(closesAt = opens - 1))

        assertEquals(NewWorkshopUiState.Error("Registration must close after it opens."), viewModel.uiState.value)
        coVerify(exactly = 0) { api.createWorkshop(any(), any()) }
    }

    @Test
    fun `creating sends a trimmed first-come project with ISO dates and the author`() {
        val request = slot<CreateWorkshopProjectRequest>()
        coEvery { api.createWorkshop(9, capture(request)) } returns Response.success(project(id = 12))

        val viewModel = NewWorkshopViewModel(api, reminders, authorId = 9)
        viewModel.submit(form())

        assertEquals(
            CreateWorkshopProjectRequest(
                title = "Suturing", description = "Hands-on", projectType = ProjectType.FIRST_COME_FIRST_SERVED,
                formLink = "https://forms.example.com/suturing", capacity = 30,
                registrationOpensAt = "2026-10-20T09:00:00", registrationClosesAt = "2026-10-21T18:00:00"
            ),
            request.captured
        )
        assertEquals(NewWorkshopUiState.Created(12), viewModel.uiState.value)
        verify { reminders.schedule(listOf(project(id = 12))) }
    }

    @Test
    fun `blank capacity is sent as no limit`() {
        val request = slot<CreateWorkshopProjectRequest>()
        coEvery { api.createWorkshop(any(), capture(request)) } returns Response.success(project())

        NewWorkshopViewModel(api, reminders, authorId = 9).submit(form(capacity = ""))

        assertNull(request.captured.capacity)
    }

    @Test
    fun `rejected create is an Error that can be retried`() {
        coEvery { api.createWorkshop(any(), any()) } returns error(400)

        val viewModel = NewWorkshopViewModel(api, reminders, authorId = 9)
        viewModel.submit(form())

        assertTrue(viewModel.uiState.value is NewWorkshopUiState.Error)
        viewModel.onErrorShown()
        assertEquals(NewWorkshopUiState.Idle, viewModel.uiState.value)
    }

    private companion object {
        const val ID = 5L
    }
}
