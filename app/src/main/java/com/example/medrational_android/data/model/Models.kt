package com.example.medrational_android.data.model

import com.google.gson.annotations.SerializedName

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
    val uploadedAt: String? = null,
    val avgRating: Double = 0.0,
    val totalRatings: Int = 0
)

// Mirrors backend FileRatingSummaryDTO. avgRating is rounded to one decimal; userRating is the
// signed-in user's score (1-5), null when they haven't rated the file
data class FileRatingSummary(
    val fileId: Long,
    val avgRating: Double,
    val totalRatings: Int,
    val userRating: Int? = null
)

data class RateFileRequest(val score: Int)

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
    val reasoningTitle: String?,
    val avgRating: Double = 0.0,
    val totalRatings: Int = 0
)

// Mirrors backend NoveltyFileItemDTO; ids are null when the file's reasoning was deleted
data class NoveltyItem(
    val fileId: Long,
    val fileName: String?,
    val fileType: String?,
    val fileSizeBytes: Long?,
    val uploadedAt: String?,
    val categoryId: Long?,
    val categoryName: String?,
    val reasoningId: Long?,
    val reasoningTitle: String?,
    val hashtags: List<String> = emptyList(),
    val avgRating: Double = 0.0,
    val totalRatings: Int = 0,
    // Uploaded since the user last opened the novelties feed. Older backends sent it as "new"
    @SerializedName(value = "isNew", alternate = ["new"])
    val isNew: Boolean = false
)

// Mirrors backend NoveltiesFeedResponseDTO; newFilesCount counts every new file, not just this page
data class NoveltiesFeed(
    val newFilesCount: Long,
    val feed: PageResponse<NoveltyItem>
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
    val phoneNumber: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)

// The signed-in user's own account (GET/PUT /api/v1/users/me). Role is ROLE_USER or ROLE_ADMIN
data class UserProfile(
    val id: Long,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val fullName: String?,
    val phoneNumber: String?,
    val role: String,
    val createdAt: String?
)

// Names and phone change without a code; a null or blank phone removes it
data class UpdateProfileRequest(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?
)

// The code goes to the new address
data class RequestEmailChangeRequest(val newEmail: String)

data class ConfirmCodeRequest(val code: String)

// The code goes to the current address
data class ChangePasswordRequest(val code: String, val newPassword: String)

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
    val hashtags: List<String> = emptyList(),
    val avgRating: Double = 0.0,
    val totalRatings: Int = 0
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
    val categoryName: String?,
    val avgRating: Double = 0.0,
    val totalRatings: Int = 0
)

data class FavoriteToggleResponse(
    val fileId: Long,
    val isFavorited: Boolean,
    val message: String
)
// --- Workshop projects (/api/v1/workshops) ------------------------------------------------------

// FIRST_COME_FIRST_SERVED: students register on the form link while OPEN, until it fills up.
// MOTIVATION: students apply on the form link with a motivation letter, and a committee picks who
// is accepted. Both work the same in the app; the selection happens on the external form
enum class ProjectType { MOTIVATION, FIRST_COME_FIRST_SERVED }

// The backend scheduler moves UPCOMING -> OPEN at registrationOpensAt and OPEN -> CLOSED at
// registrationClosesAt; admins can also force a status
enum class ProjectStatus { UPCOMING, OPEN, CLOSED }

// Mirrors backend WorkshopProjectResponseDTO. formLink is null for non-admins unless status is OPEN.
// Dates are ISO LocalDateTime in the server's time zone
data class WorkshopProject(
    val id: Long,
    val authorId: Long?,
    val authorEmail: String?,
    val title: String,
    val description: String,
    val projectType: ProjectType?,
    val status: ProjectStatus,
    val formLink: String?,
    // null means no limit
    val capacity: Int? = null,
    val registrationOpensAt: String,
    val registrationClosesAt: String,
    val createdAt: String?,
    val attachments: List<WorkshopAttachment> = emptyList()
)

// fileUrl points at the private R2 bucket and can't be opened: use ApiClient.workshopAttachment*Url
data class WorkshopAttachment(
    val id: Long,
    val fileName: String,
    val fileUrl: String,
    // The backend can preview it (PDF, Word, PowerPoint, Excel or image)
    val previewable: Boolean = false
)

// Mirrors backend CreateWorkshopProjectDTO; dates as "yyyy-MM-dd'T'HH:mm:ss"
data class CreateWorkshopProjectRequest(
    val title: String,
    val description: String,
    val projectType: ProjectType,
    val formLink: String,
    val capacity: Int?,
    val registrationOpensAt: String,
    val registrationClosesAt: String
)
