package com.example.medrational_android.ui.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.medrational_android.viewmodel.AuthUiState
import com.example.medrational_android.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onSignUpCompleted: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }

    // Validation rules
    val isFirstNameValid = firstName.trim().isNotEmpty() && firstName.length <= 50
    val isLastNameValid = lastName.trim().isNotEmpty() && lastName.length <= 50
    val cleanPhone = phoneNumber.filter { it.isDigit() }
    val isPhoneValid = cleanPhone.length == 10
    val isEmailValid = email.trim().isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val isPasswordValid = password.length >= 6

    val isFormValid = isFirstNameValid && isLastNameValid && isPhoneValid && isEmailValid && isPasswordValid

    val isOtpStep = uiState is AuthUiState.OtpSent
    val isLoading = uiState is AuthUiState.Loading

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.SignUpCompleted) {
            viewModel.resetState()
            onSignUpCompleted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isOtpStep) "Verify Account" else "Create Account") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isOtpStep) viewModel.resetState() else onBackClick()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isOtpStep) {
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))

                // First Name (Max 50 chars)
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { if (it.length <= 50) firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = firstName.isNotEmpty() && !isFirstNameValid,
                    supportingText = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            if (firstName.isNotEmpty() && !isFirstNameValid) {
                                Text("Max 50 characters", color = MaterialTheme.colorScheme.error)
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            Text("${firstName.length}/50")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Last Name (Max 50 chars)
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { if (it.length <= 50) lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = lastName.isNotEmpty() && !isLastNameValid,
                    supportingText = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            if (lastName.isNotEmpty() && !isLastNameValid) {
                                Text("Max 50 characters", color = MaterialTheme.colorScheme.error)
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            Text("${lastName.length}/50")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Phone Number (Digits only, exactly 10 digits)
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.length <= 10) phoneNumber = digits
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. 0661234567") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = phoneNumber.isNotEmpty() && !isPhoneValid,
                    supportingText = {
                        if (phoneNumber.isNotEmpty() && !isPhoneValid) {
                            Text("Must be exactly 10 digits (${cleanPhone.length}/10)", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Email Address
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = email.isNotEmpty() && !isEmailValid,
                    supportingText = {
                        if (email.isNotEmpty() && !isEmailValid) {
                            Text("Please enter a valid email address", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = null)
                        }
                    },
                    isError = password.isNotEmpty() && !isPasswordValid,
                    supportingText = {
                        if (password.isNotEmpty() && !isPasswordValid) {
                            Text("Minimum 6 characters", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Send Button: Enabled only when all constraints pass
                Button(
                    onClick = { viewModel.registerUser(firstName, lastName, cleanPhone, email, password) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isLoading && isFormValid
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Send Verification Code")
                    }
                }
            } else {
                val currentEmail = (uiState as? AuthUiState.OtpSent)?.email ?: email
                Text(
                    text = "Verify Your Email",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We sent a 6-digit code to $currentEmail",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otpCode = it },
                    label = { Text("6-Digit Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.verifySignUpOtp(currentEmail, otpCode) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isLoading && otpCode.length == 6
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Verify & Complete Registration")
                    }
                }
            }

            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as AuthUiState.Error).error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}