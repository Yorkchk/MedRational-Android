package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.model.ChangePasswordRequest
import com.example.medrational_android.data.model.ConfirmCodeRequest
import com.example.medrational_android.data.model.RequestEmailChangeRequest
import com.example.medrational_android.data.model.UpdateProfileRequest
import com.example.medrational_android.data.model.UserProfile
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private const val NETWORK_ERROR = "Network error. Check your connection and try again."
private const val NAME_MAX_LENGTH = 50
private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private val CODE_REGEX = Regex("^[0-9]{6}$")
private val PHONE_REGEX = Regex("^[0-9]{10}$")

/**
 * The backend's own message for a failed call: "error" (thrown errors such as a wrong code),
 * "message", or the first field error of a validation failure. Falls back to [fallback].
 */
internal fun profileErrorMessage(response: Response<*>, fallback: String): String {
    val raw = try {
        response.errorBody()?.string()
    } catch (e: IOException) {
        null
    }
    if (raw.isNullOrBlank()) return "$fallback (HTTP ${response.code()})."
    return try {
        val json = JsonParser.parseString(raw).asJsonObject
        val fieldError = json.getAsJsonObject("errors")?.entrySet()?.firstOrNull()?.value?.asString
        fieldError
            ?: json.get("error")?.takeIf { it.isJsonPrimitive }?.asString
            ?: json.get("message")?.takeIf { it.isJsonPrimitive }?.asString
            ?: "$fallback (HTTP ${response.code()})."
    } catch (e: RuntimeException) {
        "$fallback (HTTP ${response.code()})."
    }
}

/** Null when the names and phone can be sent, otherwise what to fix. Same rules as sign-up. */
internal fun validateProfileFields(firstName: String, lastName: String, phone: String): String? = when {
    firstName.isBlank() || lastName.isBlank() -> "First and last name are required."
    firstName.trim().length > NAME_MAX_LENGTH || lastName.trim().length > NAME_MAX_LENGTH ->
        "Names must be at most $NAME_MAX_LENGTH characters."
    phone.isNotBlank() && !PHONE_REGEX.matches(phone.trim()) -> "Phone number must be exactly 10 digits."
    else -> null
}

internal fun validateNewPassword(newPassword: String, repeatPassword: String): String? = when {
    newPassword.length < 6 -> "Password must be at least 6 characters."
    newPassword != repeatPassword -> "Passwords don't match."
    else -> null
}

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val profile: UserProfile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

sealed interface ProfileSaveState {
    data object Idle : ProfileSaveState
    data object Saving : ProfileSaveState
    data object Saved : ProfileSaveState
    data class Error(val message: String) : ProfileSaveState
}

class ProfileViewModel(
    private val api: MedRationalApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<ProfileSaveState>(ProfileSaveState.Idle)
    val saveState: StateFlow<ProfileSaveState> = _saveState.asStateFlow()

    fun load() {
        // Keep the profile on screen while refreshing, e.g. after coming back from an email change
        if (_uiState.value !is ProfileUiState.Success) {
            _uiState.value = ProfileUiState.Loading
        }
        viewModelScope.launch {
            _uiState.value = try {
                val response = api.getMyProfile()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    ProfileUiState.Success(body)
                } else {
                    ProfileUiState.Error(profileErrorMessage(response, "Couldn't load your profile"))
                }
            } catch (e: IOException) {
                ProfileUiState.Error(NETWORK_ERROR)
            } catch (e: HttpException) {
                ProfileUiState.Error("Couldn't load your profile (HTTP ${e.code()}).")
            }
        }
    }

    /** Saves names and phone directly: the user is already signed in, so no code is needed. */
    fun save(firstName: String, lastName: String, phone: String) {
        validateProfileFields(firstName, lastName, phone)?.let {
            _saveState.value = ProfileSaveState.Error(it)
            return
        }
        _saveState.value = ProfileSaveState.Saving
        viewModelScope.launch {
            _saveState.value = try {
                val response = api.updateMyProfile(
                    UpdateProfileRequest(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        phoneNumber = phone.trim().ifBlank { null }
                    )
                )
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    _uiState.value = ProfileUiState.Success(body)
                    tokenManager.saveUserName(body.fullName)
                    ProfileSaveState.Saved
                } else {
                    ProfileSaveState.Error(profileErrorMessage(response, "Couldn't save your profile"))
                }
            } catch (e: IOException) {
                ProfileSaveState.Error(NETWORK_ERROR)
            } catch (e: HttpException) {
                ProfileSaveState.Error("Couldn't save your profile (HTTP ${e.code()}).")
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = ProfileSaveState.Idle
    }
}

sealed interface ChangeEmailUiState {
    data class EnterEmail(val isSending: Boolean = false, val error: String? = null) : ChangeEmailUiState
    // notice: e.g. "A new code was sent" after a resend
    data class EnterCode(
        val newEmail: String,
        val isBusy: Boolean = false,
        val error: String? = null,
        val notice: String? = null
    ) : ChangeEmailUiState
    data object Done : ChangeEmailUiState
}

/** A code goes to the new address; confirming it switches the account and its sign-in token. */
class ChangeEmailViewModel(
    private val api: MedRationalApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChangeEmailUiState>(ChangeEmailUiState.EnterEmail())
    val uiState: StateFlow<ChangeEmailUiState> = _uiState.asStateFlow()

    fun requestCode(newEmail: String) {
        val email = newEmail.trim()
        val error = when {
            !EMAIL_REGEX.matches(email) -> "Enter a valid email address."
            email.equals(tokenManager.getEmail(), ignoreCase = true) -> "This is already your email."
            else -> null
        }
        if (error != null) {
            _uiState.value = ChangeEmailUiState.EnterEmail(error = error)
            return
        }
        _uiState.value = ChangeEmailUiState.EnterEmail(isSending = true)
        viewModelScope.launch {
            _uiState.value = when (val failure = sendCode(email)) {
                null -> ChangeEmailUiState.EnterCode(newEmail = email)
                else -> ChangeEmailUiState.EnterEmail(error = failure)
            }
        }
    }

    fun resendCode() {
        val state = _uiState.value as? ChangeEmailUiState.EnterCode ?: return
        _uiState.value = state.copy(isBusy = true, error = null, notice = null)
        viewModelScope.launch {
            _uiState.value = when (val failure = sendCode(state.newEmail)) {
                null -> state.copy(isBusy = false, error = null, notice = "A new code was sent to ${state.newEmail}.")
                else -> state.copy(isBusy = false, error = failure, notice = null)
            }
        }
    }

    /** Back to the address field, e.g. to fix a typo. */
    fun editEmail() {
        _uiState.value = ChangeEmailUiState.EnterEmail()
    }

    fun confirm(code: String) {
        val state = _uiState.value as? ChangeEmailUiState.EnterCode ?: return
        if (!CODE_REGEX.matches(code.trim())) {
            _uiState.value = state.copy(error = "Please enter the 6-digit code.", notice = null)
            return
        }
        _uiState.value = state.copy(isBusy = true, error = null, notice = null)
        viewModelScope.launch {
            _uiState.value = try {
                val response = api.confirmEmailChange(ConfirmCodeRequest(code.trim()))
                val auth = response.body()
                if (response.isSuccessful && auth != null) {
                    // The old token named the old email and no longer works
                    tokenManager.saveAuth(
                        token = auth.token,
                        role = auth.role,
                        userId = auth.userId,
                        email = auth.email,
                        fullName = auth.fullName
                    )
                    ChangeEmailUiState.Done
                } else {
                    state.copy(isBusy = false, error = profileErrorMessage(response, "Couldn't change your email"))
                }
            } catch (e: IOException) {
                state.copy(isBusy = false, error = NETWORK_ERROR)
            } catch (e: HttpException) {
                state.copy(isBusy = false, error = "Couldn't change your email (HTTP ${e.code()}).")
            }
        }
    }

    // Null on success, otherwise the message to show
    private suspend fun sendCode(email: String): String? = try {
        val response = api.requestEmailChangeCode(RequestEmailChangeRequest(email))
        if (response.isSuccessful) null else profileErrorMessage(response, "Couldn't send the code")
    } catch (e: IOException) {
        NETWORK_ERROR
    } catch (e: HttpException) {
        "Couldn't send the code (HTTP ${e.code()})."
    }
}

sealed interface ChangePasswordUiState {
    data class RequestCode(val isSending: Boolean = false, val error: String? = null) : ChangePasswordUiState
    data class EnterCode(
        val isBusy: Boolean = false,
        val error: String? = null,
        val notice: String? = null
    ) : ChangePasswordUiState
    data object Done : ChangePasswordUiState
}

/** A code goes to the current address; it is sent with the new password. */
class ChangePasswordViewModel(private val api: MedRationalApi) : ViewModel() {

    private val _uiState = MutableStateFlow<ChangePasswordUiState>(ChangePasswordUiState.RequestCode())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun requestCode() {
        _uiState.value = ChangePasswordUiState.RequestCode(isSending = true)
        viewModelScope.launch {
            _uiState.value = when (val failure = sendCode()) {
                null -> ChangePasswordUiState.EnterCode()
                else -> ChangePasswordUiState.RequestCode(error = failure)
            }
        }
    }

    fun resendCode() {
        val state = _uiState.value as? ChangePasswordUiState.EnterCode ?: return
        _uiState.value = state.copy(isBusy = true, error = null, notice = null)
        viewModelScope.launch {
            _uiState.value = when (val failure = sendCode()) {
                null -> state.copy(isBusy = false, error = null, notice = "A new code was sent.")
                else -> state.copy(isBusy = false, error = failure, notice = null)
            }
        }
    }

    fun confirm(code: String, newPassword: String, repeatPassword: String) {
        val state = _uiState.value as? ChangePasswordUiState.EnterCode ?: return
        val error = if (!CODE_REGEX.matches(code.trim())) {
            "Please enter the 6-digit code."
        } else {
            validateNewPassword(newPassword, repeatPassword)
        }
        if (error != null) {
            _uiState.value = state.copy(error = error, notice = null)
            return
        }
        _uiState.value = state.copy(isBusy = true, error = null, notice = null)
        viewModelScope.launch {
            _uiState.value = try {
                val response = api.changePassword(ChangePasswordRequest(code.trim(), newPassword))
                if (response.isSuccessful) {
                    ChangePasswordUiState.Done
                } else {
                    state.copy(isBusy = false, error = profileErrorMessage(response, "Couldn't change your password"))
                }
            } catch (e: IOException) {
                state.copy(isBusy = false, error = NETWORK_ERROR)
            } catch (e: HttpException) {
                state.copy(isBusy = false, error = "Couldn't change your password (HTTP ${e.code()}).")
            }
        }
    }

    private suspend fun sendCode(): String? = try {
        val response = api.requestPasswordChangeCode()
        if (response.isSuccessful) null else profileErrorMessage(response, "Couldn't send the code")
    } catch (e: IOException) {
        NETWORK_ERROR
    } catch (e: HttpException) {
        "Couldn't send the code (HTTP ${e.code()})."
    }
}
