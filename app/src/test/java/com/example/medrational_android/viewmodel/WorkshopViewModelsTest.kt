package com.example.medrational_android.viewmodel

import com.example.medrational_android.MainDispatcherRule
import com.example.medrational_android.TestStrings.en
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.CreateWorkshopProjectRequest
import com.example.medrational_android.data.model.PageResponse
import com.example.medrational_android.data.model.ProjectStatus
import com.example.medrational_android.data.model.ProjectType
import com.example.medrational_android.data.model.WorkshopAttachment
import com.example.medrational_android.data.model.WorkshopProject
import com.example.medrational_android.data.model.parseIsoLocalDateTime
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class WorkshopViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api: MedRationalApi = mockk()

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
    fun `list loads every project`() {
        coEvery { api.getWorkshops(null, any(), any()) } returns page(project(1), project(2))

        val viewModel = WorkshopListViewModel(api)
        viewModel.load()

        assertEquals(WorkshopListUiState.Success(listOf(project(1), project(2))), viewModel.uiState.value)
    }

    @Test
    fun `a tab filters by status`() {
        coEvery { api.getWorkshops(null, any(), any()) } returns page(project(1))
        coEvery { api.getWorkshops(ProjectStatus.OPEN, any(), any()) } returns page(project(2, ProjectStatus.OPEN))

        val viewModel = WorkshopListViewModel(api)
        viewModel.load()
        viewModel.selectTab(ProjectStatus.OPEN)

        assertEquals(ProjectStatus.OPEN, viewModel.tab.value)
        assertEquals(WorkshopListUiState.Success(listOf(project(2, ProjectStatus.OPEN))), viewModel.uiState.value)
    }

    @Test
    fun `list network failure is an Error`() {
        coEvery { api.getWorkshops(any(), any(), any()) } throws IOException("offline")

        val viewModel = WorkshopListViewModel(api)
        viewModel.load()

        assertTrue(viewModel.uiState.value is WorkshopListUiState.Error)
    }

    // --- detail ------------------------------------------------------------------------------------

    private fun loadedDetail(project: WorkshopProject = project()): WorkshopDetailViewModel {
        coEvery { api.getWorkshop(project.id) } returns Response.success(project)
        return WorkshopDetailViewModel(api).also { it.load(project.id) }
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
    fun `a project deleted meanwhile is an Error`() {
        coEvery { api.getWorkshop(ID) } returns error(404)

        val viewModel = WorkshopDetailViewModel(api)
        viewModel.load(ID)

        assertEquals("This project no longer exists.", en((viewModel.uiState.value as WorkshopDetailUiState.Error).message))
    }

    @Test
    fun `status override shows the new status`() {
        val viewModel = loadedDetail()
        val closed = project(status = ProjectStatus.CLOSED)
        coEvery { api.updateWorkshopStatus(ID, ProjectStatus.CLOSED) } returns Response.success(closed)

        viewModel.changeStatus(ProjectStatus.CLOSED)

        assertEquals(WorkshopDetailUiState.Success(closed), viewModel.uiState.value)
    }

    @Test
    fun `failed status override keeps the project and explains`() {
        val viewModel = loadedDetail()
        coEvery { api.updateWorkshopStatus(ID, any()) } throws IOException("offline")

        viewModel.changeStatus(ProjectStatus.OPEN)

        assertEquals(WorkshopDetailUiState.Success(project()), viewModel.uiState.value)
        assertTrue(en(viewModel.message.value)!!.startsWith("Network error"))
    }

    @Test
    fun `deleting goes back`() {
        val viewModel = loadedDetail()
        coEvery { api.deleteWorkshop(ID) } returns Response.success(Unit)

        viewModel.delete()

        assertEquals(WorkshopDetailUiState.Deleted, viewModel.uiState.value)
    }

    @Test
    fun `failed delete keeps the project`() {
        val viewModel = loadedDetail()
        coEvery { api.deleteWorkshop(ID) } returns error(500)

        viewModel.delete()

        assertEquals(WorkshopDetailUiState.Success(project()), viewModel.uiState.value)
        assertEquals("Something went wrong (HTTP 500). Please try again.", en(viewModel.message.value))
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

        assertEquals("Files must be 10 MB or smaller.", en(viewModel.message.value))
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

    // Fixed "now" for new projects, a few days before the form's opening time
    private val now = parseIsoLocalDateTime("2026-10-11T12:00:00")!!
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
        assertEquals("Enter a title.", en(validateWorkshopForm(form().copy(title = " "))))
        assertEquals("Enter the registration link.", en(validateWorkshopForm(form(formLink = ""))))
        assertEquals(
            "The registration link must start with http:// or https://.",
            en(validateWorkshopForm(form(formLink = "forms.example.com")))
        )
        assertEquals(
            "Capacity must be a positive number, or left empty for no limit.",
            en(validateWorkshopForm(form(capacity = "0")))
        )
        assertEquals("Pick when registration opens.", en(validateWorkshopForm(form(opensAt = null))))
        assertEquals("Registration must close after it opens.", en(validateWorkshopForm(form(closesAt = opens))))
    }

    // A 7:53 picked on a 24-hour dial meant 07:53, hours in the past: the bug behind projects created CLOSED
    @Test
    fun `new project opening in the past is rejected`() {
        val viewModel = NewWorkshopViewModel(api, authorId = 9, clock = { now })

        viewModel.submit(form(opensAt = now - 12 * 60 * 60 * 1000, closesAt = now - 11 * 60 * 60 * 1000))

        assertEquals(
            "Registration must open in the future. Check the date and time.",
            en((viewModel.uiState.value as NewWorkshopUiState.Error).message)
        )
        coVerify(exactly = 0) { api.createWorkshop(any(), any()) }
    }

    @Test
    fun `editing may keep an opening time that already passed`() {
        assertNull(validateWorkshopForm(form(opensAt = now - 1000, closesAt = now + 1000)))
    }

    @Test
    fun `invalid form makes no request`() {
        val viewModel = NewWorkshopViewModel(api, authorId = 9, clock = { now })

        viewModel.submit(form(closesAt = opens - 1))

        assertEquals("Registration must close after it opens.", en((viewModel.uiState.value as NewWorkshopUiState.Error).message))
        coVerify(exactly = 0) { api.createWorkshop(any(), any()) }
    }

    @Test
    fun `creating sends a trimmed first-come project with ISO dates and the author`() {
        val request = slot<CreateWorkshopProjectRequest>()
        coEvery { api.createWorkshop(9, capture(request)) } returns Response.success(project(id = 12))

        val viewModel = NewWorkshopViewModel(api, authorId = 9, clock = { now })
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
    }

    @Test
    fun `blank capacity is sent as no limit`() {
        val request = slot<CreateWorkshopProjectRequest>()
        coEvery { api.createWorkshop(any(), capture(request)) } returns Response.success(project())

        NewWorkshopViewModel(api, authorId = 9, clock = { now }).submit(form(capacity = ""))

        assertNull(request.captured.capacity)
    }

    @Test
    fun `creating sends the type the admin picked`() {
        val request = slot<CreateWorkshopProjectRequest>()
        coEvery { api.createWorkshop(any(), capture(request)) } returns Response.success(project())

        NewWorkshopViewModel(api, authorId = 9, clock = { now })
            .submit(form().copy(projectType = ProjectType.MOTIVATION))

        assertEquals(ProjectType.MOTIVATION, request.captured.projectType)
    }

    @Test
    fun `a new project is first come, first served unless the admin picks otherwise`() {
        assertEquals(ProjectType.FIRST_COME_FIRST_SERVED, WorkshopForm().projectType)
    }

    @Test
    fun `rejected create is an Error that can be retried`() {
        coEvery { api.createWorkshop(any(), any()) } returns error(400)

        val viewModel = NewWorkshopViewModel(api, authorId = 9, clock = { now })
        viewModel.submit(form())

        assertTrue(viewModel.uiState.value is NewWorkshopUiState.Error)
        viewModel.onErrorShown()
        assertEquals(NewWorkshopUiState.Idle, viewModel.uiState.value)
    }

    // --- attachments on create ---------------------------------------------------------------------

    private fun pending(name: String) =
        PendingAttachment(name, MultipartBody.Part.createFormData("file", name, name.toRequestBody()))

    @Test
    fun `creating attaches every picked file to the new project`() {
        val files = listOf(pending("program.pdf"), pending("map.png"))
        coEvery { api.createWorkshop(any(), any()) } returns Response.success(project(id = 12))
        coEvery { api.uploadWorkshopAttachment(12, any()) } returns
            Response.success(WorkshopAttachment(1, "x", "https://cdn.example.com/x"))

        val viewModel = NewWorkshopViewModel(api, authorId = 9, clock = { now })
        viewModel.submit(form(), files)

        assertEquals(NewWorkshopUiState.Created(12, failedFiles = emptyList()), viewModel.uiState.value)
        coVerify(ordering = io.mockk.Ordering.SEQUENCE) {
            api.createWorkshop(9, any())
            api.uploadWorkshopAttachment(12, files[0].part)
            api.uploadWorkshopAttachment(12, files[1].part)
        }
    }

    @Test
    fun `files that fail to attach are reported but the project is still created`() {
        val files = listOf(pending("program.pdf"), pending("huge.zip"), pending("map.png"))
        coEvery { api.createWorkshop(any(), any()) } returns Response.success(project(id = 12))
        coEvery { api.uploadWorkshopAttachment(12, files[0].part) } returns
            Response.success(WorkshopAttachment(1, "program.pdf", "https://cdn.example.com/program.pdf"))
        coEvery { api.uploadWorkshopAttachment(12, files[1].part) } returns error(413)
        coEvery { api.uploadWorkshopAttachment(12, files[2].part) } throws IOException("offline")

        val viewModel = NewWorkshopViewModel(api, authorId = 9, clock = { now })
        viewModel.submit(form(), files)

        assertEquals(NewWorkshopUiState.Created(12, failedFiles = listOf("huge.zip", "map.png")), viewModel.uiState.value)
    }

    @Test
    fun `no file is uploaded when the project can't be created`() {
        coEvery { api.createWorkshop(any(), any()) } returns error(400)

        NewWorkshopViewModel(api, authorId = 9, clock = { now }).submit(form(), listOf(pending("program.pdf")))

        coVerify(exactly = 0) { api.uploadWorkshopAttachment(any(), any()) }
    }

    // --- edit --------------------------------------------------------------------------------------

    private fun loadedEdit(project: WorkshopProject = project(status = ProjectStatus.OPEN, formLink = LINK)): EditWorkshopViewModel {
        coEvery { api.getWorkshop(project.id) } returns Response.success(project)
        return EditWorkshopViewModel(api).also { it.load(project.id) }
    }

    @Test
    fun `edit form is prefilled with the project`() {
        val viewModel = loadedEdit()

        assertEquals(
            EditWorkshopUiState.Editing(
                WorkshopForm(
                    title = "Suturing $ID", description = "Hands-on", formLink = LINK, capacity = "30",
                    opensAtMillis = opens, closesAtMillis = closes
                )
            ),
            viewModel.uiState.value
        )
    }

    @Test
    fun `editing a motivation project keeps its type`() {
        val viewModel = loadedEdit(project(formLink = LINK).copy(projectType = ProjectType.MOTIVATION))
        val form = (viewModel.uiState.value as EditWorkshopUiState.Editing).initial
        val request = slot<CreateWorkshopProjectRequest>()
        coEvery { api.updateWorkshop(ID, capture(request)) } returns Response.success(project())

        viewModel.save(form.copy(title = "Renamed"))

        assertEquals(ProjectType.MOTIVATION, form.projectType)
        assertEquals(ProjectType.MOTIVATION, request.captured.projectType)
    }

    // The edit screen has no type picker, so even a form saying otherwise can't change it
    @Test
    fun `saving an edit never changes the type`() {
        val viewModel = loadedEdit(project(formLink = LINK).copy(projectType = ProjectType.MOTIVATION))
        val form = (viewModel.uiState.value as EditWorkshopUiState.Editing).initial
        val request = slot<CreateWorkshopProjectRequest>()
        coEvery { api.updateWorkshop(ID, capture(request)) } returns Response.success(project())

        viewModel.save(form.copy(projectType = ProjectType.FIRST_COME_FIRST_SERVED))

        assertEquals(ProjectType.MOTIVATION, request.captured.projectType)
    }

    @Test
    fun `unlimited capacity is prefilled as empty`() {
        val viewModel = loadedEdit(project().copy(capacity = null))

        assertEquals("", (viewModel.uiState.value as EditWorkshopUiState.Editing).initial.capacity)
    }

    @Test
    fun `saving sends the trimmed form with ISO dates`() {
        val viewModel = loadedEdit()
        val newOpens = parseIsoLocalDateTime("2026-11-02T08:30:00")!!
        val newCloses = parseIsoLocalDateTime("2026-11-03T12:00:00")!!
        val updated = project(status = ProjectStatus.UPCOMING, formLink = LINK)
        val request = slot<CreateWorkshopProjectRequest>()
        coEvery { api.updateWorkshop(ID, capture(request)) } returns Response.success(updated)

        viewModel.save(WorkshopForm(" New title ", " New text ", " $LINK ", "", newOpens, newCloses))

        assertEquals(
            CreateWorkshopProjectRequest(
                title = "New title", description = "New text", projectType = ProjectType.FIRST_COME_FIRST_SERVED,
                formLink = LINK, capacity = null,
                registrationOpensAt = "2026-11-02T08:30:00", registrationClosesAt = "2026-11-03T12:00:00"
            ),
            request.captured
        )
        assertEquals(EditWorkshopUiState.Saved, viewModel.uiState.value)
    }

    @Test
    fun `invalid edit makes no request and explains`() {
        val viewModel = loadedEdit()

        viewModel.save(WorkshopForm("Title", "Text", LINK, "", opensAtMillis = closes, closesAtMillis = opens))

        assertEquals("Registration must close after it opens.", en(viewModel.message.value))
        assertTrue(viewModel.uiState.value is EditWorkshopUiState.Editing)
        coVerify(exactly = 0) { api.updateWorkshop(any(), any()) }
    }

    @Test
    fun `failed save keeps the form open and explains`() {
        val viewModel = loadedEdit()
        coEvery { api.updateWorkshop(ID, any()) } throws IOException("offline")

        viewModel.save(WorkshopForm("Title", "Text", LINK, "", opens, closes))

        assertEquals(EditWorkshopUiState.Editing(project(status = ProjectStatus.OPEN, formLink = LINK).toForm()), viewModel.uiState.value)
        assertTrue(en(viewModel.message.value)!!.startsWith("Network error"))
    }

    @Test
    fun `editing a project that was deleted is a load error`() {
        coEvery { api.getWorkshop(ID) } returns error(404)

        val viewModel = EditWorkshopViewModel(api)
        viewModel.load(ID)

        assertEquals("This project no longer exists.", en((viewModel.uiState.value as EditWorkshopUiState.LoadError).message))
    }

    @Test
    fun `edit load network failure is a load error`() {
        coEvery { api.getWorkshop(ID) } throws IOException("offline")

        val viewModel = EditWorkshopViewModel(api)
        viewModel.load(ID)

        assertTrue(viewModel.uiState.value is EditWorkshopUiState.LoadError)
    }

    // --- automatic status changes ------------------------------------------------------------------

    private val opensAt = parseIsoLocalDateTime("2026-10-20T09:00:00")!!
    private val closesAt = parseIsoLocalDateTime("2026-10-21T18:00:00")!!
    private val scheduler get() = mainDispatcherRule.dispatcher.scheduler

    // The device clock follows the test's virtual time, starting [before] the opening
    private fun clockStarting(before: Long): () -> Long {
        val start = opensAt - before
        return { start + scheduler.currentTime }
    }

    @Test
    fun `the next change is the opening for upcoming and the closing for open projects`() {
        val now = opensAt - 60_000

        assertEquals(opensAt, nextStatusChangeMillis(project(status = ProjectStatus.UPCOMING), now))
        assertEquals(closesAt, nextStatusChangeMillis(project(status = ProjectStatus.OPEN), now))
        assertNull(nextStatusChangeMillis(project(status = ProjectStatus.CLOSED), now))
    }

    @Test
    fun `a change that passed long ago is not expected anymore`() {
        // E.g. an admin set the project back to upcoming after it opened
        assertEquals(opensAt, nextStatusChangeMillis(project(status = ProjectStatus.UPCOMING), opensAt + 30_000))
        assertNull(nextStatusChangeMillis(project(status = ProjectStatus.UPCOMING), opensAt + 120_000))
    }

    @Test
    fun `an open detail page switches to open with the link at the opening time`() {
        val opened = project(status = ProjectStatus.OPEN, formLink = LINK)
        coEvery { api.getWorkshop(ID) } returns Response.success(project()) andThen Response.success(opened)
        val viewModel = WorkshopDetailViewModel(api, clockStarting(before = 60_000))
        viewModel.load(ID)

        scheduler.advanceTimeBy(60_000)
        scheduler.runCurrent()
        // Not yet: the backend switches on the minute, the page asks a few seconds later
        coVerify(exactly = 1) { api.getWorkshop(ID) }

        scheduler.advanceTimeBy(6_000)
        scheduler.runCurrent()

        val state = viewModel.uiState.value as WorkshopDetailUiState.Success
        assertEquals(ProjectStatus.OPEN, state.project.status)
        assertEquals(LINK, state.registrationLink)
    }

    @Test
    fun `the detail page asks again when the backend switches late`() {
        coEvery { api.getWorkshop(ID) } returnsMany listOf(
            Response.success(project()),
            Response.success(project()),
            Response.success(project(status = ProjectStatus.OPEN))
        )
        val viewModel = WorkshopDetailViewModel(api, clockStarting(before = 0))
        viewModel.load(ID)

        scheduler.advanceTimeBy(6_000)
        scheduler.runCurrent()
        assertEquals(ProjectStatus.UPCOMING, (viewModel.uiState.value as WorkshopDetailUiState.Success).project.status)

        scheduler.advanceTimeBy(16_000)
        scheduler.runCurrent()
        assertEquals(ProjectStatus.OPEN, (viewModel.uiState.value as WorkshopDetailUiState.Success).project.status)
    }

    @Test
    fun `a closed project is not refetched`() {
        coEvery { api.getWorkshop(ID) } returns Response.success(project(status = ProjectStatus.CLOSED))
        val viewModel = WorkshopDetailViewModel(api, clockStarting(before = 0))
        viewModel.load(ID)

        scheduler.advanceTimeBy(10 * 60_000L)
        scheduler.runCurrent()

        coVerify(exactly = 1) { api.getWorkshop(ID) }
    }

    @Test
    fun `the list refreshes when a listed project opens`() {
        coEvery { api.getWorkshops(null, any(), any()) } returns page(project(1), project(2, ProjectStatus.CLOSED)) andThen
            page(project(1, ProjectStatus.OPEN), project(2, ProjectStatus.CLOSED))
        val viewModel = WorkshopListViewModel(api, clockStarting(before = 60_000))
        viewModel.load()

        scheduler.advanceTimeBy(66_000)
        scheduler.runCurrent()

        assertEquals(
            WorkshopListUiState.Success(listOf(project(1, ProjectStatus.OPEN), project(2, ProjectStatus.CLOSED))),
            viewModel.uiState.value
        )
    }

    private companion object {
        const val LINK = "https://forms.example.com/suturing"
        const val ID = 5L
    }
}
