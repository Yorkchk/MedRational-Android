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

data class LoginRequest(val email: String, val password: String)
data class VerifyOtpRequest(val email: String, val code: String)
data class AuthResponse(val token: String, val message: String)