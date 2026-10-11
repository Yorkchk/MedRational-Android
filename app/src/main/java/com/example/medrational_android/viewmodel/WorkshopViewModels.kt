package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.CreateWorkshopProjectRequest
import com.example.medrational_android.data.model.ProjectStatus
import com.example.medrational_android.data.model.ProjectType
import com.example.medrational_android.data.model.WorkshopProject
import com.example.medrational_android.data.model.toIsoLocalDateTime
import com.example.medrational_android.data.reminders.WorkshopReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import retrofit2.HttpException
import java.io.IOException

private const val NETWORK_ERROR = "Network error. Check your connection and try again."

// Turns the backend's workshop error codes into something a user can act on
internal fun workshopErrorMessage(code: Int): String = when (code) {
    400 -> "Please check the form: every field is required and registration must close after it opens."
    404 -> "This project no longer exists."
    413 -> "Files must be 10 MB or smaller."
    else -> "Something went wrong (HTTP $code). Please try again."
}

// --- List ----------------------------------------------------------------------------------------

sealed interface WorkshopListUiState {
    data object Loading : WorkshopListUiState
    data class Success(val projects: List<WorkshopProject>) : WorkshopListUiState
    data class Error(val message: String) : WorkshopListUiState
}

/** Tabs filter by status; a null tab shows every project. Each load refreshes the device's reminders. */
class WorkshopListViewModel(
    private val api: MedRationalApi,
    private val reminders: WorkshopReminderScheduler
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

    fun load() {
        val status = _tab.value
        viewModelScope.launch {
            val result = try {
                val response = api.getWorkshops(status)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    reminders.schedule(body.content)
                    WorkshopListUiState.Success(body.content)
                } else {
                    WorkshopListUiState.Error(workshopErrorMessage(response.code()))
                }
            } catch (e: IOException) {
                WorkshopListUiState.Error(NETWORK_ERROR)
            } catch (e: HttpException) {
                WorkshopListUiState.Error(workshopErrorMessage(e.code()))
            }
            // Ignore a slow response for a tab the user already left
            if (status == _tab.value) _uiState.value = result
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
    data class Error(val message: String) : WorkshopDetailUiState
    // Deleted by the admin; the screen goes back
    data object Deleted : WorkshopDetailUiState
}

class WorkshopDetailViewModel(
    private val api: MedRationalApi,
    private val reminders: WorkshopReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkshopDetailUiState>(WorkshopDetailUiState.Loading)
    val uiState: StateFlow<WorkshopDetailUiState> = _uiState.asStateFlow()

    // One-shot feedback for an action, cleared by onMessageShown()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var projectId: Long? = null

    fun load(id: Long) {
        projectId = id
        // Keep the project on screen while refreshing
        if (_uiState.value !is WorkshopDetailUiState.Success) {
            _uiState.value = WorkshopDetailUiState.Loading
        }
        viewModelScope.launch {
            val result = try {
                val response = api.getWorkshop(id)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    reminders.schedule(listOf(body))
                    WorkshopDetailUiState.Success(body)
                } else {
                    if (response.code() == 404) reminders.cancel(id)
                    WorkshopDetailUiState.Error(workshopErrorMessage(response.code()))
                }
            } catch (e: IOException) {
                WorkshopDetailUiState.Error(NETWORK_ERROR)
            } catch (e: HttpException) {
                WorkshopDetailUiState.Error(workshopErrorMessage(e.code()))
            }
            val current = _uiState.value
            _uiState.value = if (result is WorkshopDetailUiState.Error && current is WorkshopDetailUiState.Success) {
                _message.value = result.message
                current.copy(busy = false)
            } else {
                result
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
                    reminders.schedule(listOf(body))
                    _uiState.value = WorkshopDetailUiState.Success(body)
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
                    reminders.cancel(current.project.id)
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
                    _message.value = "Attached ${attachment.fileName}."
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

    private fun fail(message: String) {
        _message.value = message
        _uiState.update { state ->
            if (state is WorkshopDetailUiState.Success) state.copy(busy = false) else state
        }
    }
}

// --- New project (admin) -------------------------------------------------------------------------

data class WorkshopForm(
    val title: String = "",
    val description: String = "",
    val formLink: String = "",
    // Digits only; blank means no limit
    val capacity: String = "",
    val opensAtMillis: Long? = null,
    val closesAtMillis: Long? = null
)

/** The first problem with [form], or null when it can be sent. */
fun validateWorkshopForm(form: WorkshopForm): String? {
    val link = form.formLink.trim()
    return when {
        form.title.isBlank() -> "Enter a title."
        form.description.isBlank() -> "Enter a description."
        link.isEmpty() -> "Enter the registration link."
        !(link.startsWith("https://") || link.startsWith("http://")) || link.length <= "https://".length ->
            "The registration link must start with http:// or https://."
        form.capacity.isNotBlank() && (form.capacity.trim().toIntOrNull() ?: 0) <= 0 ->
            "Capacity must be a positive number, or left empty for no limit."
        form.opensAtMillis == null -> "Pick when registration opens."
        form.closesAtMillis == null -> "Pick when registration closes."
        form.closesAtMillis <= form.opensAtMillis -> "Registration must close after it opens."
        else -> null
    }
}

sealed interface NewWorkshopUiState {
    data object Idle : NewWorkshopUiState
    data object Sending : NewWorkshopUiState
    data class Created(val projectId: Long) : NewWorkshopUiState
    data class Error(val message: String) : NewWorkshopUiState
}

class NewWorkshopViewModel(
    private val api: MedRationalApi,
    private val reminders: WorkshopReminderScheduler,
    // The signed-in admin, recorded as the author
    private val authorId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewWorkshopUiState>(NewWorkshopUiState.Idle)
    val uiState: StateFlow<NewWorkshopUiState> = _uiState.asStateFlow()

    fun submit(form: WorkshopForm) {
        if (_uiState.value == NewWorkshopUiState.Sending) return
        validateWorkshopForm(form)?.let {
            _uiState.value = NewWorkshopUiState.Error(it)
            return
        }
        val request = CreateWorkshopProjectRequest(
            title = form.title.trim(),
            description = form.description.trim(),
            projectType = ProjectType.FIRST_COME_FIRST_SERVED,
            formLink = form.formLink.trim(),
            capacity = form.capacity.trim().toIntOrNull(),
            registrationOpensAt = toIsoLocalDateTime(requireNotNull(form.opensAtMillis)),
            registrationClosesAt = toIsoLocalDateTime(requireNotNull(form.closesAtMillis))
        )
        _uiState.value = NewWorkshopUiState.Sending
        viewModelScope.launch {
            _uiState.value = try {
                val response = api.createWorkshop(authorId, request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    reminders.schedule(listOf(body))
                    NewWorkshopUiState.Created(body.id)
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

    // Lets the form be submitted again after an error was shown
    fun onErrorShown() {
        if (_uiState.value is NewWorkshopUiState.Error) _uiState.value = NewWorkshopUiState.Idle
    }
}
