package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.R
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.model.LoginRequest
import com.example.medrational_android.data.model.UserRegisterRequest
import com.example.medrational_android.data.model.UserVerifyOtpRequest
import com.example.medrational_android.data.model.ResetPasswordRequest
import com.example.medrational_android.data.model.ForgotPasswordRequest
import com.example.medrational_android.ui.text.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class OtpSent(val email: String) : AuthUiState()
    data class LoginSuccess(val role: String) : AuthUiState()
    data object SignUpCompleted : AuthUiState()
    data class Error(val error: UiText) : AuthUiState()
    data object PasswordResetCompleted : AuthUiState()
}

private val NETWORK_ERROR = UiText.Res(R.string.error_network)
private val INVALID_CODE = UiText.Res(R.string.auth_error_invalid_code)
private val ENTER_CODE = UiText.Res(R.string.auth_error_enter_code)

/**
 * The backend's own messages are in English, so the app shows its messages instead, picked from
 * the HTTP status. 400 means the backend rejected what was typed (see AuthServiceImpl).
 */
class AuthViewModel(
    private val tokenManager: TokenManager,
    private val api: MedRationalApi = ApiClient.api
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Email + password only: sign-in never asks for a code. */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error(UiText.Res(R.string.auth_error_credentials_required))
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = api.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!

                    // Use the role returned from the backend DTO
                    val userRole = auth.role

                    tokenManager.saveAuth(
                        token = auth.token,
                        role = userRole,
                        userId = auth.userId,
                        email = auth.email,
                        fullName = auth.fullName
                    )

                    _uiState.value = AuthUiState.LoginSuccess(role = userRole)
                } else {
                    // 403: right password, but the sign-up code was never entered
                    _uiState.value = AuthUiState.Error(
                        if (response.code() == 403) UiText.Res(R.string.auth_error_email_not_verified)
                        else UiText.Res(R.string.auth_error_invalid_credentials)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(NETWORK_ERROR)
            }
        }
    }

    fun registerUser(firstName: String, lastName: String, phone: String, email: String, password: String) {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error(UiText.Res(R.string.auth_error_all_fields_required))
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val fullName = "${firstName.trim()} ${lastName.trim()}"
                val response = api.requestUserOtp(
                    UserRegisterRequest(
                        fullName = fullName,
                        email = email.trim(),
                        password = password,
                        phoneNumber = phone.trim(),
                        firstName = firstName.trim(),
                        lastName = lastName.trim()
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = AuthUiState.OtpSent(email = email.trim())
                } else {
                    _uiState.value = AuthUiState.Error(
                        when (response.code()) {
                            400 -> UiText.Res(R.string.auth_error_check_fields)
                            409 -> UiText.Res(R.string.auth_error_email_taken)
                            else -> UiText.Res(R.string.auth_error_registration_failed)
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(NETWORK_ERROR)
            }
        }
    }

    fun verifySignUpOtp(email: String, code: String) {
        if (code.isBlank() || code.length < 6) {
            _uiState.value = AuthUiState.Error(ENTER_CODE)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = api.verifyUserOtp(UserVerifyOtpRequest(email.trim(), code.trim()))
                if (response.isSuccessful) {
                    _uiState.value = AuthUiState.SignUpCompleted
                } else {
                    _uiState.value = AuthUiState.Error(INVALID_CODE)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(NETWORK_ERROR)
            }
        }
    }

    fun requestPasswordResetOtp(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error(UiText.Res(R.string.auth_error_email_required))
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = api.forgotPassword(ForgotPasswordRequest(email.trim()))
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = AuthUiState.OtpSent(email = email.trim())
                } else {
                    _uiState.value = AuthUiState.Error(
                        if (response.code() == 400) UiText.Res(R.string.auth_error_no_account)
                        else UiText.Res(R.string.auth_error_reset_code_failed)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(NETWORK_ERROR)
            }
        }
    }

    fun confirmPasswordReset(email: String, code: String, newPassword: String) {
        if (code.isBlank() || code.length < 6) {
            _uiState.value = AuthUiState.Error(ENTER_CODE)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = api.resetPassword(
                    ResetPasswordRequest(
                        email = email.trim(),
                        code = code.trim(),
                        newPassword = newPassword
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = AuthUiState.PasswordResetCompleted
                } else {
                    _uiState.value = AuthUiState.Error(
                        if (response.code() == 400) INVALID_CODE
                        else UiText.Res(R.string.auth_error_reset_failed)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(NETWORK_ERROR)
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
