package com.example.medrational_android.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.R
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.model.ChangePasswordRequest
import com.example.medrational_android.data.model.ConfirmCodeRequest
import com.example.medrational_android.data.model.RequestEmailChangeRequest
import com.example.medrational_android.data.model.UpdateProfileRequest
import com.example.medrational_android.data.model.UserProfile
import com.example.medrational_android.ui.text.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private val NETWORK_ERROR = UiText.Res(R.string.error_network)
private val ENTER_CODE = UiText.Res(R.string.auth_error_enter_code)
private const val NAME_MAX_LENGTH = 50
private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private val CODE_REGEX = Regex("^[0-9]{6}$")
private val PHONE_REGEX = Regex("^[0-9]{10}$")

/**
 * What to show for a failed call. The backend answers 400 for what the user typed (a wrong or
 * expired code, an email already in use...) with an English message, so the app shows its own:
 * [badRequest], or "email already in use" when the backend says so. Other statuses show [failed]
 * with the HTTP code.
 */
internal fun profileErrorMessage(
    response: Response<*>,
    @StringRes failed: Int,
    @StringRes badRequest: Int = R.string.profile_error_check_fields
): UiText {
    if (response.code() != 400) return UiText.Res(failed, listOf(response.code()))
    val raw = try {
        response.errorBody()?.string()
    } catch (e: IOException) {
        null
    }
    return if (raw?.contains("already in use", ignoreCase = true) == true) {
        UiText.Res(R.string.profile_error_email_in_use)
    } else {
        UiText.Res(badRequest)
    }
}

/** Null when the names and phone can be sent, otherwise what to fix. Same rules as sign-up. */
internal fun validateProfileFields(firstName: String, lastName: String, phone: String): UiText? = when {
    firstName.isBlank() || lastName.isBlank() -> UiText.Res(R.string.profile_error_names_required)
    firstName.trim().length > NAME_MAX_LENGTH || lastName.trim().length > NAME_MAX_LENGTH ->
        UiText.Res(R.string.profile_error_name_too_long, listOf(NAME_MAX_LENGTH))
    phone.isNotBlank() && !PHONE_REGEX.matches(phone.trim()) -> UiText.Res(R.string.profile_error_phone_digits)
    else -> null
}

internal fun validateNewPassword(newPassword: String, repeatPassword: String): UiText? = when {
    newPassword.length < 6 -> UiText.Res(R.string.profile_error_password_too_short)
    newPassword != repeatPassword -> UiText.Res(R.string.profile_error_passwords_mismatch)
    else -> null
}

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val profile: UserProfile) : ProfileUiState
    data class Error(val message: UiText) : ProfileUiState
}

sealed interface ProfileSaveState {
    data object Idle : ProfileSaveState
    data object Saving : ProfileSaveState
    data object Saved : ProfileSaveState
    data class Error(val message: UiText) : ProfileSaveState
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
                    ProfileUiState.Error(profileErrorMessage(response, R.string.profile_error_load))
                }
            } catch (e: IOException) {
                ProfileUiState.Error(NETWORK_ERROR)
            } catch (e: HttpException) {
                ProfileUiState.Error(UiText.Res(R.string.profile_error_load, listOf(e.code())))
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
                    ProfileSaveState.Error(profileErrorMessage(response, R.string.profile_error_save))
                }
            } catch (e: IOException) {
                ProfileSaveState.Error(NETWORK_ERROR)
            } catch (e: HttpException) {
                ProfileSaveState.Error(UiText.Res(R.string.profile_error_save, listOf(e.code())))
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = ProfileSaveState.Idle
    }
}

sealed interface ChangeEmailUiState {
    data class EnterEmail(val isSending: Boolean = false, val error: UiText? = null) : ChangeEmailUiState
    // notice: e.g. "A new code was sent" after a resend
    data class EnterCode(
        val newEmail: String,
        val isBusy: Boolean = false,
        val error: UiText? = null,
        val notice: UiText? = null
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
            !EMAIL_REGEX.matches(email) -> UiText.Res(R.string.profile_error_invalid_email)
            email.equals(tokenManager.getEmail(), ignoreCase = true) -> UiText.Res(R.string.profile_error_same_email)
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
                null -> state.copy(isBusy = false, error = null, notice = UiText.Res(R.string.profile_code_resent_to, listOf(state.newEmail)))
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
            _uiState.value = state.copy(error = ENTER_CODE, notice = null)
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
                    state.copy(
                        isBusy = false,
                        error = profileErrorMessage(response, R.string.profile_error_change_email, R.string.auth_error_invalid_code)
                    )
                }
            } catch (e: IOException) {
                state.copy(isBusy = false, error = NETWORK_ERROR)
            } catch (e: HttpException) {
                state.copy(isBusy = false, error = UiText.Res(R.string.profile_error_change_email, listOf(e.code())))
            }
        }
    }

    // Null on success, otherwise the message to show
    private suspend fun sendCode(email: String): UiText? = try {
        val response = api.requestEmailChangeCode(RequestEmailChangeRequest(email))
        if (response.isSuccessful) null else profileErrorMessage(response, R.string.profile_error_send_code, R.string.profile_error_invalid_email)
    } catch (e: IOException) {
        NETWORK_ERROR
    } catch (e: HttpException) {
        UiText.Res(R.string.profile_error_send_code, listOf(e.code()))
    }
}

sealed interface ChangePasswordUiState {
    data class RequestCode(val isSending: Boolean = false, val error: UiText? = null) : ChangePasswordUiState
    data class EnterCode(
        val isBusy: Boolean = false,
        val error: UiText? = null,
        val notice: UiText? = null
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
                null -> state.copy(isBusy = false, error = null, notice = UiText.Res(R.string.profile_code_resent))
                else -> state.copy(isBusy = false, error = failure, notice = null)
            }
        }
    }

    fun confirm(code: String, newPassword: String, repeatPassword: String) {
        val state = _uiState.value as? ChangePasswordUiState.EnterCode ?: return
        val error = if (!CODE_REGEX.matches(code.trim())) {
            ENTER_CODE
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
                    state.copy(
                        isBusy = false,
                        error = profileErrorMessage(response, R.string.profile_error_change_password, R.string.auth_error_invalid_code)
                    )
                }
            } catch (e: IOException) {
                state.copy(isBusy = false, error = NETWORK_ERROR)
            } catch (e: HttpException) {
                state.copy(isBusy = false, error = UiText.Res(R.string.profile_error_change_password, listOf(e.code())))
            }
        }
    }

    private suspend fun sendCode(): UiText? = try {
        val response = api.requestPasswordChangeCode()
        if (response.isSuccessful) null else profileErrorMessage(response, R.string.profile_error_send_code)
    } catch (e: IOException) {
        NETWORK_ERROR
    } catch (e: HttpException) {
        UiText.Res(R.string.profile_error_send_code, listOf(e.code()))
    }
}
