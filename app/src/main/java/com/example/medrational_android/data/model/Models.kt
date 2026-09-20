package com.example.medrational_android.data.model

data class Category(
    val id: Long,
    val name: String,
    val description: String?,
    val reasonings: List<Reasoning>? = emptyList()
)

data class Reasoning(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val content: String?,
    val files: List<StudyFile>? = emptyList()
)

data class StudyFile(
    val id: Long,
    val reasoningId: Long,
    val fileName: String,
    val fileType: String?,
    val publicUrl: String,
    val fileSizeBytes: Long?
)

data class AuthMessageResponse(
    val message: String?
)

// In data/model/Models.kt

data class CreateCategoryRequest(
    val name: String,
    val description: String? = null
)

data class CreateReasoningRequest(
    val categoryId: Long,
    val title: String,
    val content: String
)

data class LoginRequest(val email: String, val password: String)
data class VerifyOtpRequest(val email: String, val code: String)
data class AuthResponse(
    val token: String,
    val role: String,
    val userId: Long,
    val email: String,
    val fullName: String,
    val message: String
)
// Under medrational_android/data/model/Models.kt


data class UserRegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val phoneNumber: String? = null
)

// User OTP verification request
data class UserVerifyOtpRequest(
    val email: String,
    val code: String
)

// User Auth Response
data class UserAuthResponse(
    val userId: Long,
    val email: String,
    val fullName: String?,
    val role: String,
    val token: String,
    val message: String
)

data class MessageResponse(
    val message: String
)

