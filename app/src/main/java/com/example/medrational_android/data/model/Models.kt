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
    val fileSizeBytes: Long?,
    // Backend can render it as a PDF (PDF, DOCX, PPTX, XLSX)
    val previewable: Boolean = false,
    val uploadedAt: String? = null
)

// Mirrors backend HashtagResponseDTO; names are stored lower-case without the leading '#'
data class Hashtag(
    val id: Long,
    val name: String
)

// Mirrors backend AttachHashtagsRequestDTO; unknown names are created on the fly
data class AttachHashtagsRequest(
    val tagNames: List<String>
)

// Mirrors backend TrendingHashtagMetricDTO: every hashtag in use, with how many files carry it
data class HashtagUsage(
    val hashtag: String,
    val attachedFilesCount: Long
)

// Mirrors backend RecentDownloadGlimpseDTO; ids are null when the file or its reasoning was deleted
data class RecentDownload(
    val fileId: Long?,
    val fileName: String?,
    val fileType: String?,
    val fileSizeBytes: Long?,
    val downloadedAt: String?,
    val categoryId: Long?,
    val categoryName: String?,
    val reasoningId: Long?,
    val reasoningTitle: String?
)

// --- Suggestions: mirror the backend Suggestion*DTOs ---

// PENDING: open conversation. CLOSED: archived by an admin, read-only for both sides
enum class SuggestionStatus { PENDING, CLOSED }

// Metadata only; the file is downloaded from /suggestions/{id}/attachment or
// /suggestions/{id}/messages/{messageId}/attachment
data class SuggestionAttachment(
    val fileName: String,
    val contentType: String?,
    val sizeBytes: Long?
)

data class SuggestionSender(
    val userId: Long,
    val fullName: String?,
    val email: String,
    val phoneNumber: String?
)

data class SuggestionMessage(
    val id: Long,
    val fromAdmin: Boolean,
    val authorName: String?,
    val body: String,
    val attachment: SuggestionAttachment?,
    val createdAt: String?
)

data class SuggestionSummary(
    val id: Long,
    val title: String,
    val status: SuggestionStatus,
    val createdAt: String?,
    val lastActivityAt: String?,
    val messageCount: Int = 0,
    // The other side posted since you last opened it
    val unread: Boolean = false,
    val senderName: String?,
    val senderEmail: String?
)

data class SuggestionDetail(
    val id: Long,
    val title: String,
    val description: String,
    val status: SuggestionStatus,
    val createdAt: String?,
    val lastActivityAt: String?,
    val closedAt: String?,
    val attachment: SuggestionAttachment?,
    val sender: SuggestionSender,
    // Oldest first
    val messages: List<SuggestionMessage> = emptyList()
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
data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)
data class FileSearchResult(
    val id: Long,
    val reasoningId: Long?,
    val reasoningTitle: String,
    val categoryId: Long?,
    val categoryName: String,
    val fileName: String,
    val fileType: String?,
    val publicUrl: String,
    val fileSizeBytes: Long?,
    val previewable: Boolean = false,
    val hashtags: List<String> = emptyList()
)

data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long,
    val totalPages: Int,
    val number: Int
)

// Mirrors backend FavoriteResponseDTO: file details are nested under "file"
data class FavoriteResponse(
    val favoriteId: Long,
    val userId: Long,
    val file: FavoriteFile,
    val createdAt: String?
)

data class FavoriteFile(
    val id: Long,
    val fileName: String?,
    val fileType: String?,
    val publicUrl: String?,
    val reasoningId: Long?,
    val reasoningTitle: String?,
    val categoryId: Long?,
    val categoryName: String?
)

data class FavoriteToggleResponse(
    val fileId: Long,
    val isFavorited: Boolean,
    val message: String
)