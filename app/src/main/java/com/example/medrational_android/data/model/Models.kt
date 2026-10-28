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

// The app only lists PENDING ones: suggestions are no longer closed (archived)
enum class SuggestionStatus { PENDING, CLOSED }

// Metadata only; the file is downloaded from /suggestions/{id}/attachment
data class SuggestionAttachment(
    val fileName: String,
    val contentType: String?,
    val sizeBytes: Long?,
    // PDF, DOCX, PPTX, XLSX or an image: /suggestions/{id}/attachment/preview can show it
    val previewable: Boolean = false
)

data class SuggestionSender(
    val userId: Long,
    val fullName: String?,
    val email: String,
    val phoneNumber: String?
)

data class SuggestionSummary(
    val id: Long,
    val title: String,
    val createdAt: String?,
    // No admin opened it yet
    val unread: Boolean = false,
    val senderName: String?,
    val senderEmail: String?
)

data class SuggestionDetail(
    val id: Long,
    val title: String,
    val description: String,
    val createdAt: String?,
    val attachment: SuggestionAttachment?,
    val sender: SuggestionSender
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
data class AuthResponse(
    val token: String,
    val role: String,
    val userId: Long,
    val email: String,
    val fullName: String,
    val message: String,
    // Traded for a new token when this one expires (POST /auth/refresh). Last, with a default, so
    // older backends that don't send it still parse
    val refreshToken: String? = null
)

data class RefreshTokenRequest(val refreshToken: String)
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

// --- Admin statistics (/api/v1/admin/analytics/dashboard) ---
// Mirror backend AdminStatisticsDTO and its parts. Every field has a default so Gson keeps it when
// the backend leaves it out. A "week" is the last 7 days, today included, and students are the
// non-admin accounts

data class AdminStatistics(
    val generatedAt: String? = null,
    val users: UserStats = UserStats(),
    val usage: UsageStats = UsageStats(),
    val lastWeek: ContentStats = ContentStats(),
    val allTime: ContentStats = ContentStats(),
    val trends: TrendStats = TrendStats(),
    val engagement: EngagementStats = EngagementStats()
)

// One chart point: the first day of the period as "yyyy-MM-dd", and its value
data class StatPoint(
    val periodStart: String = "",
    val value: Long = 0
)

data class UserStats(
    val totalStudents: Long = 0,
    val newStudentsThisWeek: Long = 0,
    val newStudentsPreviousWeek: Long = 0,
    val weeklyActiveStudents: Long = 0,
    val weeklyActiveStudentsPreviousWeek: Long = 0,
    val dailyActiveStudents: Long = 0,
    // Students who downloaded or opened at least one file
    val activatedStudents: Long = 0,
    // 12 weeks, oldest first
    val weeklySignUps: List<StatPoint> = emptyList(),
    val weeklyActiveSeries: List<StatPoint> = emptyList()
)

data class UsageStats(
    val totalDownloads: Long = 0,
    val downloadsThisWeek: Long = 0,
    val downloadsPreviousWeek: Long = 0,
    val totalViews: Long = 0,
    val viewsThisWeek: Long = 0,
    val viewsPreviousWeek: Long = 0,
    // 30 days, oldest first
    val dailyDownloads: List<StatPoint> = emptyList(),
    val dailyViews: List<StatPoint> = emptyList()
)

// A ranked file; metricValue is the count the list is ranked by (downloads, views, favorites, ratings)
data class FileMetric(
    val fileId: Long = 0,
    val fileName: String = "",
    // null when the file has no reasoning, so it can't be opened
    val reasoningId: Long? = null,
    val categoryName: String? = null,
    val reasoningTitle: String? = null,
    val metricValue: Long = 0,
    val avgRating: Double? = null,
    val totalRatings: Int? = null
)

data class CategoryActivity(
    val categoryId: Long = 0,
    val categoryName: String = "",
    val downloads: Long = 0,
    val views: Long = 0,
    val favorites: Long = 0
)

data class HashtagActivity(
    val name: String = "",
    val downloads: Long = 0
)

data class ContentStats(
    val topDownloaded: List<FileMetric> = emptyList(),
    val topViewed: List<FileMetric> = emptyList(),
    val mostFavorited: List<FileMetric> = emptyList(),
    // Every category, busiest first
    val categories: List<CategoryActivity> = emptyList(),
    val topHashtags: List<HashtagActivity> = emptyList()
)

// A file or category whose downloads plus views rose against the previous week. reasoningId and
// categoryName are only set for files
data class TrendingItem(
    val id: Long = 0,
    val name: String = "",
    val reasoningId: Long? = null,
    val categoryName: String? = null,
    val thisWeek: Long = 0,
    val previousWeek: Long = 0
)

data class TrendStats(
    val files: List<TrendingItem> = emptyList(),
    val categories: List<TrendingItem> = emptyList()
)

data class EngagementStats(
    val totalFiles: Long = 0,
    val newFilesThisWeek: Long = 0,
    val filesWithoutDownloads: Long = 0,
    val totalFavorites: Long = 0,
    val favoritesThisWeek: Long = 0,
    val totalRatings: Long = 0,
    val ratingsThisWeek: Long = 0,
    // null when nobody rated anything yet
    val averageRating: Double? = null,
    val highestRated: List<FileMetric> = emptyList(),
    val openSuggestions: Long = 0,
    val unreadSuggestions: Long = 0,
    val upcomingWorkshops: Long = 0,
    val openWorkshops: Long = 0,
    val closedWorkshops: Long = 0
)
