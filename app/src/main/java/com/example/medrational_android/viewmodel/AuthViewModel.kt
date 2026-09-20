package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.model.LoginRequest
import com.example.medrational_android.data.model.UserRegisterRequest
import com.example.medrational_android.data.model.UserVerifyOtpRequest
import com.example.medrational_android.data.model.VerifyOtpRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class OtpSent(val email: String, val message: String) : AuthUiState()
    data class LoginSuccess(val role: String, val message: String) : AuthUiState()
    data object SignUpCompleted : AuthUiState()
    data class Error(val error: String) : AuthUiState()
}

class AuthViewModel(
    private val tokenManager: TokenManager,
    private val api: MedRationalApi = ApiClient.api
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun requestLoginOtp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password are required.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = api.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = AuthUiState.OtpSent(
                        email = email.trim(),
                        message = response.body()?.message ?: "Verification code sent."
                    )
                } else {
                    _uiState.value = AuthUiState.Error(extractErrorMessage(response.errorBody()?.string()) ?: "Invalid credentials.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Network error.")
            }
        }
    }

    fun verifyLoginOtp(email: String, code: String) {
        if (code.isBlank() || code.length < 6) {
            _uiState.value = AuthUiState.Error("Please enter a 6-digit code.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = api.verifyOtp(VerifyOtpRequest(email.trim(), code.trim()))
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

                    _uiState.value = AuthUiState.LoginSuccess(
                        role = userRole,
                        message = auth.message
                    )
                } else {
                    _uiState.value = AuthUiState.Error(
                        extractErrorMessage(response.errorBody()?.string())
                            ?: "Invalid or expired verification code."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Network error.")
            }
        }
    }

    fun registerUser(firstName: String, lastName: String, phone: String, email: String, password: String) {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("All fields are required.")
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
                        phoneNumber = phone.trim()
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    val serverMsg = response.body()?.message ?: "Verification code sent."
                    _uiState.value = AuthUiState.OtpSent(
                        email = email.trim(),
                        message = serverMsg
                    )
                } else {
                    _uiState.value = AuthUiState.Error(extractErrorMessage(response.errorBody()?.string()) ?: "Registration failed.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Network error.")
            }
        }
    }

    fun verifySignUpOtp(email: String, code: String) {
        if (code.isBlank() || code.length < 6) {
            _uiState.value = AuthUiState.Error("Please enter a 6-digit code.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = api.verifyUserOtp(UserVerifyOtpRequest(email.trim(), code.trim()))
                if (response.isSuccessful) {
                    _uiState.value = AuthUiState.SignUpCompleted
                } else {
                    _uiState.value = AuthUiState.Error(extractErrorMessage(response.errorBody()?.string()) ?: "Invalid code.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Network error.")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun extractErrorMessage(rawBody: String?): String? {
        if (rawBody.isNullOrBlank()) return null
        return try {
            val json = JSONObject(rawBody)
            json.optString("message", json.optString("error", rawBody))
        } catch (_: Exception) {
            rawBody
        }
    }
}