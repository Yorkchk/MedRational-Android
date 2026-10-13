package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.R
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.CreateWorkshopProjectRequest
import com.example.medrational_android.data.model.ProjectStatus
import com.example.medrational_android.data.model.ProjectType
import com.example.medrational_android.data.model.WorkshopProject
import com.example.medrational_android.data.model.parseIsoLocalDateTime
import com.example.medrational_android.data.model.toIsoLocalDateTime
import com.example.medrational_android.ui.text.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import retrofit2.HttpException
import java.io.IOException

private val NETWORK_ERROR = UiText.Res(R.string.error_network)

// Turns the backend's workshop error codes into something a user can act on
internal fun workshopErrorMessage(code: Int): UiText = when (code) {
    400 -> UiText.Res(R.string.workshop_error_form)
    404 -> UiText.Res(R.string.workshop_error_gone)
    413 -> UiText.Res(R.string.error_file_too_large)
    else -> UiText.Res(R.string.error_generic_http, listOf(code))
}

// The backend switches statuses on the minute; refetch a little after, and again a few times if it's late
private const val STATUS_CHANGE_GRACE_MILLIS = 5_000L
private const val STATUS_CHANGE_RETRY_MILLIS = 15_000L
private const val STATUS_CHANGE_CHECKS = 4
// A change that just passed may not be in the backend's response yet
private const val STATUS_CHANGE_WINDOW_MILLIS = 60_000L

/**
 * When the backend will next move [project] on its own (opening for UPCOMING, closing for OPEN), or
 * null when no change is due. A time that passed less than a minute ago still counts, since the
 * backend may not have switched yet; an older one means an admin kept the status by hand.
 */
fun nextStatusChangeMillis(project: WorkshopProject, nowMillis: Long): Long? {
    val changeAt = when (project.status) {
        ProjectStatus.UPCOMING -> parseIsoLocalDateTime(project.registrationOpensAt)
        ProjectStatus.OPEN -> parseIsoLocalDateTime(project.registrationClosesAt)
        ProjectStatus.CLOSED -> null
    } ?: return null
    return changeAt.takeIf { it > nowMillis - STATUS_CHANGE_WINDOW_MILLIS }
}

// --- List ----------------------------------------------------------------------------------------

sealed interface WorkshopListUiState {
    data object Loading : WorkshopListUiState
    data class Success(val projects: List<WorkshopProject>) : WorkshopListUiState
    data class Error(val message: UiText) : WorkshopListUiState
}

/** Tabs filter by status; a null tab shows every project. */
class WorkshopListViewModel(
    private val api: MedRationalApi,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _tab = MutableStateFlow<ProjectStatus?>(null)
    val tab: StateFlow<ProjectStatus?> = _tab.asStateFlow()

    private val _uiState = MutableStateFlow<WorkshopListUiState>(WorkshopListUiState.Loading)
    val uiState: StateFlow<WorkshopListUiState> = _uiState.asStateFlow()

    fun selectTab(status: ProjectStatus?) {
        if (status == _tab.value) return
        _tab.value = status
        _uiState.value = WorkshopListUiState.Loading
        load()
    }

    // Refetches when a listed project is due to open or close
    private var statusWatch: Job? = null

    fun load() {
        val status = _tab.value
        viewModelScope.launch {
            val result = fetch(status)
            // Ignore a slow response for a tab the user already left
            if (status == _tab.value) show(result)
        }
    }

    private suspend fun fetch(status: ProjectStatus?): WorkshopListUiState = try {
        val response = api.getWorkshops(status)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            WorkshopListUiState.Success(body.content)
        } else {
            WorkshopListUiState.Error(workshopErrorMessage(response.code()))
        }
    } catch (e: IOException) {
        WorkshopListUiState.Error(NETWORK_ERROR)
    } catch (e: HttpException) {
        WorkshopListUiState.Error(workshopErrorMessage(e.code()))
    }

    private fun show(state: WorkshopListUiState) {
        _uiState.value = state
        statusWatch?.cancel()
        if (state is WorkshopListUiState.Success) watchStatuses(state.projects)
    }

    private fun watchStatuses(projects: List<WorkshopProject>) {
        val now = clock()
        val changeAt = projects.mapNotNull { nextStatusChangeMillis(it, now) }.minOrNull() ?: return
        val status = _tab.value
        statusWatch = viewModelScope.launch {
            delay(changeAt - now + STATUS_CHANGE_GRACE_MILLIS)
            repeat(STATUS_CHANGE_CHECKS) {
                // A failed background refetch is retried silently rather than replacing the list
                val result = fetch(status)
                if (status != _tab.value) return@launch
                if (result is WorkshopListUiState.Success && result != _uiState.value) {
                    show(result)
                    return@launch
                }
                delay(STATUS_CHANGE_RETRY_MILLIS)
            }
        }
    }
}

// --- Detail and admin actions --------------------------------------------------------------------

sealed interface WorkshopDetailUiState {
    data object Loading : WorkshopDetailUiState
    data class Success(
        val project: WorkshopProject,
        // An admin action is in flight
        val busy: Boolean = false
    ) : WorkshopDetailUiState {
        // The backend only sends the link to students while registration is open
        val registrationLink: String?
            get() = project.formLink?.takeIf { project.status == ProjectStatus.OPEN && it.isNotBlank() }
    }
    data class Error(val message: UiText) : WorkshopDetailUiState
    // Deleted by the admin; the screen goes back
    data object Deleted : WorkshopDetailUiState
}

class WorkshopDetailViewModel(
    private val api: MedRationalApi,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkshopDetailUiState>(WorkshopDetailUiState.Loading)
    val uiState: StateFlow<WorkshopDetailUiState> = _uiState.asStateFlow()

    // One-shot feedback for an action, cleared by onMessageShown()
    private val _message = MutableStateFlow<UiText?>(null)
    val message: StateFlow<UiText?> = _message.asStateFlow()

    private var projectId: Long? = null

    // Refetches when the project is due to open or close
    private var statusWatch: Job? = null

    fun load(id: Long) {
        projectId = id
        // Keep the project on screen while refreshing
        if (_uiState.value !is WorkshopDetailUiState.Success) {
            _uiState.value = WorkshopDetailUiState.Loading
        }
        viewModelScope.launch {
            val result = fetch(id)
            val current = _uiState.value
            if (result is WorkshopDetailUiState.Error && current is WorkshopDetailUiState.Success) {
                _message.value = result.message
                _uiState.value = current.copy(busy = false)
            } else {
                show(result)
            }
        }
    }

    private suspend fun fetch(id: Long): WorkshopDetailUiState = try {
        val response = api.getWorkshop(id)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            WorkshopDetailUiState.Success(body)
        } else {
            WorkshopDetailUiState.Error(workshopErrorMessage(response.code()))
        }
    } catch (e: IOException) {
        WorkshopDetailUiState.Error(NETWORK_ERROR)
    } catch (e: HttpException) {
        WorkshopDetailUiState.Error(workshopErrorMessage(e.code()))
    }

    private fun show(state: WorkshopDetailUiState) {
        _uiState.value = state
        statusWatch?.cancel()
        if (state is WorkshopDetailUiState.Success) watchStatus(state.project)
    }

    private fun watchStatus(project: WorkshopProject) {
        val now = clock()
        val changeAt = nextStatusChangeMillis(project, now) ?: return
        statusWatch = viewModelScope.launch {
            delay(changeAt - now + STATUS_CHANGE_GRACE_MILLIS)
            repeat(STATUS_CHANGE_CHECKS) {
                // A failed background refetch is retried silently; an admin action in flight wins
                val result = fetch(project.id)
                val current = _uiState.value
                if (result is WorkshopDetailUiState.Success && result.project.status != project.status &&
                    current is WorkshopDetailUiState.Success && !current.busy
                ) {
                    show(result)
                    return@launch
                }
                delay(STATUS_CHANGE_RETRY_MILLIS)
            }
        }
    }

    fun refresh() {
        projectId?.let(::load)
    }

    // Admin only
    fun changeStatus(status: ProjectStatus) {
        val current = startAction() ?: return
        if (current.project.status == status) {
            _uiState.value = current
            return
        }
        viewModelScope.launch {
            runAction {
                val response = api.updateWorkshopStatus(current.project.id, status)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    show(WorkshopDetailUiState.Success(body))
                } else {
                    fail(workshopErrorMessage(response.code()))
                }
            }
        }
    }

    // Admin only; permanent
    fun delete() {
        val current = startAction() ?: return
        viewModelScope.launch {
            runAction {
                val response = api.deleteWorkshop(current.project.id)
                if (response.isSuccessful) {
                    _uiState.value = WorkshopDetailUiState.Deleted
                } else {
                    fail(workshopErrorMessage(response.code()))
                }
            }
        }
    }

    // Admin only
    fun uploadAttachment(file: MultipartBody.Part) {
        val current = startAction() ?: return
        viewModelScope.launch {
            runAction {
                val response = api.uploadWorkshopAttachment(current.project.id, file)
                val attachment = response.body()
                if (response.isSuccessful && attachment != null) {
                    _uiState.update { state ->
                        if (state is WorkshopDetailUiState.Success) {
                            state.copy(
                                project = state.project.copy(attachments = state.project.attachments + attachment),
                                busy = false
                            )
                        } else {
                            state
                        }
                    }
                    _message.value = UiText.Res(R.string.workshop_attached, listOf(attachment.fileName))
                } else {
                    fail(workshopErrorMessage(response.code()))
                }
            }
        }
    }

    // Admin only
    fun deleteAttachment(attachmentId: Long) {
        startAction() ?: return
        viewModelScope.launch {
            runAction {
                val response = api.deleteWorkshopAttachment(attachmentId)
                if (response.isSuccessful) {
                    _uiState.update { state ->
                        if (state is WorkshopDetailUiState.Success) {
                            state.copy(
                                project = state.project.copy(
                                    attachments = state.project.attachments.filterNot { it.id == attachmentId }
                                ),
                                busy = false
                            )
                        } else {
                            state
                        }
                    }
                } else {
                    fail(workshopErrorMessage(response.code()))
                }
            }
        }
    }

    fun onMessageShown() {
        _message.value = null
    }

    // Marks the screen busy and returns the project, or null when it isn't loaded or already busy
    private fun startAction(): WorkshopDetailUiState.Success? {
        val current = _uiState.value as? WorkshopDetailUiState.Success ?: return null
        if (current.busy) return null
        _uiState.value = current.copy(busy = true)
        return current
    }

    private suspend fun runAction(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: IOException) {
            fail(NETWORK_ERROR)
        } catch (e: HttpException) {
            fail(workshopErrorMessage(e.code()))
        }
    }

    private fun fail(message: UiText) {
        _message.value = message
        _uiState.update { state ->
            if (state is WorkshopDetailUiState.Success) state.copy(busy = false) else state
        }
    }
}

// --- Project form (admin) -----------------------------------------------------------------------

data class WorkshopForm(
    val title: String = "",
    val description: String = "",
    val formLink: String = "",
    // Digits only; blank means no limit
    val capacity: String = "",
    val opensAtMillis: Long? = null,
    val closesAtMillis: Long? = null,
    // Chosen when creating; both types work the same in the app
    val projectType: ProjectType = ProjectType.FIRST_COME_FIRST_SERVED
)

/**
 * The first problem with [form], or null when it can be sent. With [nowMillis] (new projects), the
 * opening time must be after it: a time in the past is almost always a wrong date or time.
 */
fun validateWorkshopForm(form: WorkshopForm, nowMillis: Long? = null): UiText? {
    val link = form.formLink.trim()
    return when {
        form.title.isBlank() -> UiText.Res(R.string.workshop_error_title)
        form.description.isBlank() -> UiText.Res(R.string.workshop_error_description)
        link.isEmpty() -> UiText.Res(R.string.workshop_error_link)
        !(link.startsWith("https://") || link.startsWith("http://")) || link.length <= "https://".length ->
            UiText.Res(R.string.workshop_error_link_scheme)
        form.capacity.isNotBlank() && (form.capacity.trim().toIntOrNull() ?: 0) <= 0 ->
            UiText.Res(R.string.workshop_error_capacity)
        form.opensAtMillis == null -> UiText.Res(R.string.workshop_error_opens)
        form.closesAtMillis == null -> UiText.Res(R.string.workshop_error_closes)
        form.closesAtMillis <= form.opensAtMillis -> UiText.Res(R.string.workshop_error_close_after_open)
        nowMillis != null && form.opensAtMillis <= nowMillis ->
            UiText.Res(R.string.workshop_error_open_future)
        else -> null
    }
}

// Call only with a form validateWorkshopForm accepted
private fun WorkshopForm.toRequest() = CreateWorkshopProjectRequest(
    title = title.trim(),
    description = description.trim(),
    projectType = projectType,
    formLink = formLink.trim(),
    capacity = capacity.trim().toIntOrNull(),
    registrationOpensAt = toIsoLocalDateTime(requireNotNull(opensAtMillis)),
    registrationClosesAt = toIsoLocalDateTime(requireNotNull(closesAtMillis))
)

/** The form prefilled with this project, for editing. Admins receive the link in every status. */
fun WorkshopProject.toForm() = WorkshopForm(
    title = title,
    description = description,
    formLink = formLink.orEmpty(),
    capacity = capacity?.toString().orEmpty(),
    opensAtMillis = parseIsoLocalDateTime(registrationOpensAt),
    closesAtMillis = parseIsoLocalDateTime(registrationClosesAt),
    projectType = projectType ?: ProjectType.FIRST_COME_FIRST_SERVED
)

// --- New project (admin) -------------------------------------------------------------------------

/** A file picked on the new-project form, already read for upload. */
data class PendingAttachment(val fileName: String, val part: MultipartBody.Part)

sealed interface NewWorkshopUiState {
    data object Idle : NewWorkshopUiState
    data object Sending : NewWorkshopUiState
    // failedFiles: picked files that couldn't be attached; the project exists regardless
    data class Created(val projectId: Long, val failedFiles: List<String> = emptyList()) : NewWorkshopUiState
    data class Error(val message: UiText) : NewWorkshopUiState
}

class NewWorkshopViewModel(
    private val api: MedRationalApi,
    // The signed-in admin, recorded as the author
    private val authorId: Long,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewWorkshopUiState>(NewWorkshopUiState.Idle)
    val uiState: StateFlow<NewWorkshopUiState> = _uiState.asStateFlow()

    /** Creates the project, then attaches [attachments] one by one. */
    fun submit(form: WorkshopForm, attachments: List<PendingAttachment> = emptyList()) {
        if (_uiState.value == NewWorkshopUiState.Sending) return
        validateWorkshopForm(form, nowMillis = clock())?.let {
            _uiState.value = NewWorkshopUiState.Error(it)
            return
        }
        val request = form.toRequest()
        _uiState.value = NewWorkshopUiState.Sending
        viewModelScope.launch {
            _uiState.value = try {
                val response = api.createWorkshop(authorId, request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    NewWorkshopUiState.Created(body.id, failedFiles = attach(body.id, attachments))
                } else {
                    NewWorkshopUiState.Error(workshopErrorMessage(response.code()))
                }
            } catch (e: IOException) {
                NewWorkshopUiState.Error(NETWORK_ERROR)
            } catch (e: HttpException) {
                NewWorkshopUiState.Error(workshopErrorMessage(e.code()))
            }
        }
    }

    // Names of the files that couldn't be attached
    private suspend fun attach(projectId: Long, attachments: List<PendingAttachment>): List<String> =
        attachments.filterNot { attachment ->
            try {
                api.uploadWorkshopAttachment(projectId, attachment.part).isSuccessful
            } catch (e: IOException) {
                false
            } catch (e: HttpException) {
                false
            }
        }.map { it.fileName }

    // Lets the form be submitted again after an error was shown
    fun onErrorShown() {
        if (_uiState.value is NewWorkshopUiState.Error) _uiState.value = NewWorkshopUiState.Idle
    }
}

// --- Edit project (admin) ------------------------------------------------------------------------

sealed interface EditWorkshopUiState {
    data object Loading : EditWorkshopUiState
    data class LoadError(val message: UiText) : EditWorkshopUiState
    // initial seeds the form; the screen owns the edits from there
    data class Editing(val initial: WorkshopForm, val saving: Boolean = false) : EditWorkshopUiState
    data object Saved : EditWorkshopUiState
}

class EditWorkshopViewModel(
    private val api: MedRationalApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditWorkshopUiState>(EditWorkshopUiState.Loading)
    val uiState: StateFlow<EditWorkshopUiState> = _uiState.asStateFlow()

    // One-shot feedback for a rejected save, cleared by onMessageShown()
    private val _message = MutableStateFlow<UiText?>(null)
    val message: StateFlow<UiText?> = _message.asStateFlow()

    private var project: WorkshopProject? = null

    fun load(id: Long) {
        // Already loaded: keep the edits, e.g. across a configuration change
        if (project?.id == id) return
        _uiState.value = EditWorkshopUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val response = api.getWorkshop(id)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    project = body
                    EditWorkshopUiState.Editing(body.toForm())
                } else {
                    EditWorkshopUiState.LoadError(workshopErrorMessage(response.code()))
                }
            } catch (e: IOException) {
                EditWorkshopUiState.LoadError(NETWORK_ERROR)
            } catch (e: HttpException) {
                EditWorkshopUiState.LoadError(workshopErrorMessage(e.code()))
            }
        }
    }

    fun save(form: WorkshopForm) {
        val original = project ?: return
        val current = _uiState.value as? EditWorkshopUiState.Editing ?: return
        if (current.saving) return
        validateWorkshopForm(form)?.let {
            _message.value = it
            return
        }
        // The type isn't on the form, so it stays as it was
        val request = form.copy(projectType = original.projectType ?: ProjectType.FIRST_COME_FIRST_SERVED).toRequest()
        _uiState.value = current.copy(saving = true)
        viewModelScope.launch {
            val error = try {
                val response = api.updateWorkshop(original.id, request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    null
                } else {
                    workshopErrorMessage(response.code())
                }
            } catch (e: IOException) {
                NETWORK_ERROR
            } catch (e: HttpException) {
                workshopErrorMessage(e.code())
            }
            if (error == null) {
                _uiState.value = EditWorkshopUiState.Saved
            } else {
                _message.value = error
                _uiState.value = current.copy(saving = false)
            }
        }
    }

    fun onMessageShown() {
        _message.value = null
    }
}
