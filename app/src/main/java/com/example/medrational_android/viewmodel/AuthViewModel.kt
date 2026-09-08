package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.model.LoginRequest
import com.example.medrational_android.data.model.VerifyOtpRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthStep {
    object EnterCredentials : AuthStep
    object EnterOtp : AuthStep
    object Authenticated : AuthStep
}

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(private val tokenManager: TokenManager) : ViewModel() {

    private val _currentStep = MutableStateFlow<AuthStep>(
        if (tokenManager.isLoggedIn()) AuthStep.Authenticated else AuthStep.EnterCredentials
    )
    val currentStep: StateFlow<AuthStep> = _currentStep.asStateFlow()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    var savedEmail: String = ""
        private set

    fun submitCredentials(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter both email and password")
            return
        }

        savedEmail = email.trim()
        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val response = ApiClient.api.login(LoginRequest(savedEmail, password))
                if (response.isSuccessful) {
                    _uiState.value = AuthUiState.Idle
                    _currentStep.value = AuthStep.EnterOtp
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Invalid credentials"
                    _uiState.value = AuthUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Failed to connect to server")
            }
        }
    }

    fun submitOtp(code: String) {
        if (code.isBlank() || code.length < 6) {
            _uiState.value = AuthUiState.Error("Enter a valid 6-digit OTP")
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val response = ApiClient.api.verifyOtp(VerifyOtpRequest(savedEmail, code.trim()))
                if (response.isSuccessful) {
                    val token = response.body()?.token
                    if (!token.isNullOrBlank()) {
                        tokenManager.saveToken(token)
                        _uiState.value = AuthUiState.Idle
                        _currentStep.value = AuthStep.Authenticated
                    } else {
                        _uiState.value = AuthUiState.Error("Token missing from server response")
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "OTP verification failed"
                    _uiState.value = AuthUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Verification error")
            }
        }
    }

    fun logout() {
        tokenManager.clearToken()
        _currentStep.value = AuthStep.EnterCredentials
        _uiState.value = AuthUiState.Idle
        savedEmail = ""
    }
}