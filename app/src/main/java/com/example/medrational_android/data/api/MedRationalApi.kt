package com.example.medrational_android.data.api

import com.example.medrational_android.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface MedRationalApi {

    // Categories
    @GET("api/v1/categories")
    suspend fun getCategories(): Response<List<Category>>

    @POST("api/v1/categories")
    suspend fun createCategory(@Body request: CreateCategoryRequest): Response<Category>

    @DELETE("api/v1/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long): Response<Unit>

    // Reasonings
    @GET("api/v1/reasonings/category/{categoryId}")
    suspend fun getReasoningsByCategory(@Path("categoryId") categoryId: Long): Response<List<Reasoning>>

    @GET("api/v1/reasonings/{id}")
    suspend fun getReasoning(@Path("id") id: Long): Response<Reasoning>

    @POST("api/v1/reasonings")
    suspend fun createReasoning(@Body request: CreateReasoningRequest): Response<Reasoning>

    @DELETE("api/v1/reasonings/{id}")
    suspend fun deleteReasoning(@Path("id") id: Long): Response<Unit>

    // Files
    @Multipart
    @POST("api/v1/files/upload/{reasoningId}")
    suspend fun uploadFile(
        @Path("reasoningId") reasoningId: Long,
        @Part file: MultipartBody.Part
    ): Response<StudyFile>

    // PDF rendition of the file; 415 when the format can't be previewed, 503 when conversion fails
    @Streaming
    @GET("api/v1/files/{id}/preview")
    suspend fun getFilePreview(@Path("id") id: Long): Response<ResponseBody>

    // The original file, for sharing; the backend records it as a download (Recent downloads)
    @Streaming
    @GET("api/v1/downloads/file/{id}")
    suspend fun downloadStudyFile(@Path("id") id: Long): Response<ResponseBody>

    @DELETE("api/v1/files/{id}")
    suspend fun deleteFile(@Path("id") id: Long): Response<Unit>

    // Hashtags (attach/detach are admin only; the backend deletes a hashtag once no file uses it)
    @GET("api/v1/files/{fileId}/hashtags")
    suspend fun getFileHashtags(@Path("fileId") fileId: Long): Response<List<Hashtag>>

    @POST("api/v1/files/{fileId}/hashtags")
    suspend fun attachHashtags(
        @Path("fileId") fileId: Long,
        @Body request: AttachHashtagsRequest
    ): Response<List<Hashtag>>

    @DELETE("api/v1/files/{fileId}/hashtags/{hashtagId}")
    suspend fun detachHashtag(
        @Path("fileId") fileId: Long,
        @Path("hashtagId") hashtagId: Long
    ): Response<Unit>

    // Ratings: the rater comes from the JWT. Each call returns the file's new average and count
    @GET("api/v1/files/{fileId}/ratings")
    suspend fun getFileRating(@Path("fileId") fileId: Long): Response<FileRatingSummary>

    @POST("api/v1/files/{fileId}/ratings")
    suspend fun rateFile(
        @Path("fileId") fileId: Long,
        @Body request: RateFileRequest
    ): Response<FileRatingSummary>

    @DELETE("api/v1/files/{fileId}/ratings")
    suspend fun removeFileRating(@Path("fileId") fileId: Long): Response<FileRatingSummary>

    // Admin only: a 403 here would log a regular user out, so only call it when TokenManager.isAdmin()
    @GET("api/v1/admin/analytics/trending-hashtags")
    suspend fun getHashtagUsage(@Query("limit") limit: Int = 500): Response<List<HashtagUsage>>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthMessageResponse>

    // 2. Unified Login OTP Verify -> /api/v1/auth/verify-otp
    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>

    // 3. User Register -> /api/v1/auth/user/register
    @POST("api/v1/auth/user/register")
    suspend fun requestUserOtp(@Body request: UserRegisterRequest): Response<AuthMessageResponse>

    // 4. User Register OTP Verify -> /api/v1/auth/user/verify-registration
    @POST("api/v1/auth/user/verify-registration")
    suspend fun verifyUserOtp(@Body request: UserVerifyOtpRequest): Response<UserAuthResponse>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<AuthMessageResponse>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<AuthMessageResponse>

    // Profile: the user comes from the JWT. Wrong or expired codes answer 400, never 401/403
    @GET("api/v1/users/me")
    suspend fun getMyProfile(): Response<UserProfile>

    @PUT("api/v1/users/me")
    suspend fun updateMyProfile(@Body request: UpdateProfileRequest): Response<UserProfile>

    // Emails a code to the current address
    @POST("api/v1/users/me/password/code")
    suspend fun requestPasswordChangeCode(): Response<AuthMessageResponse>

    @PUT("api/v1/users/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<AuthMessageResponse>

    // Emails a code to the new address
    @POST("api/v1/users/me/email/code")
    suspend fun requestEmailChangeCode(@Body request: RequestEmailChangeRequest): Response<AuthMessageResponse>

    // Returns a new token: the old one is tied to the old email and stops working
    @PUT("api/v1/users/me/email")
    suspend fun confirmEmailChange(@Body request: ConfirmCodeRequest): Response<AuthResponse>

    @GET("api/v1/search/files")
    suspend fun searchFiles(
        @Query("query") query: String?,
        @Query("categoryId") categoryId: Long?
    ): Response<PageResponse<FileSearchResult>>

    @POST("api/v1/users/{userId}/favorites/{fileId}/toggle")
    suspend fun toggleFavorite(
        @Path("userId") userId: Long,
        @Path("fileId") fileId: Long
    ): Response<FavoriteToggleResponse>

    @GET("api/v1/users/{userId}/favorites")
    suspend fun getUserFavorites(
        @Path("userId") userId: Long,
        @Query("size") size: Int = 1000 // fetch all favorites so every heart reflects its state
    ): Response<PageResponse<FavoriteResponse>>

    // Suggestions are one-way: users send one, admins read and delete them. Attachments are
    // optional (max 10 MB); a null @Part is left out of the request
    @Multipart
    @POST("api/v1/suggestions")
    suspend fun createSuggestion(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part file: MultipartBody.Part?
    ): Response<SuggestionDetail>

    // Admin only: a 403 here would log a regular user out, so only call it when TokenManager.isAdmin()
    @GET("api/v1/suggestions")
    suspend fun getSuggestionInbox(
        @Query("status") status: SuggestionStatus = SuggestionStatus.PENDING,
        @Query("size") size: Int = 100
    ): Response<PageResponse<SuggestionSummary>>

    // Admin only; marks it read for the admin
    @GET("api/v1/suggestions/{id}")
    suspend fun getSuggestion(@Path("id") id: Long): Response<SuggestionDetail>

    // Admin only. PDF rendition of the attachment (images as-is); 415 when the format can't be previewed
    @Streaming
    @GET("api/v1/suggestions/{id}/attachment/preview")
    suspend fun getSuggestionAttachmentPreview(@Path("id") id: Long): Response<ResponseBody>

    // Admin only
    @DELETE("api/v1/suggestions/{id}")
    suspend fun deleteSuggestion(@Path("id") id: Long): Response<Unit>

    // Newest first, each file once; the backend records single-file downloads itself.
    // A null limit uses the backend default (10)
    @GET("api/v1/users/{userId}/recent-downloads")
    suspend fun getRecentDownloads(
        @Path("userId") userId: Long,
        @Query("limit") limit: Int? = null
    ): Response<List<RecentDownload>>

    // Novelties: every file, newest first, flagged isNew when uploaded since the caller last
    // opened the feed. The caller comes from the JWT, so only call these when signed in
    @GET("api/v1/novelties")
    suspend fun getNovelties(
        @Query("page") page: Int,
        @Query("size") size: Int = 20
    ): Response<NoveltiesFeed>

    // {"unreadCount": n}
    @GET("api/v1/novelties/badge-count")
    suspend fun getNoveltyBadgeCount(): Response<Map<String, Long>>

    // Moves the caller's baseline to now: nothing is new until the next upload
    @POST("api/v1/novelties/mark-seen")
    suspend fun markNoveltiesSeen(): Response<Unit>

    // Workshop projects need a signed-in user. Sorted by opening time; a null status lists all.
    // formLink is null for non-admins unless the project is OPEN
    @GET("api/v1/workshops")
    suspend fun getWorkshops(
        @Query("status") status: ProjectStatus?,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<PageResponse<WorkshopProject>>

    @GET("api/v1/workshops/{id}")
    suspend fun getWorkshop(@Path("id") id: Long): Response<WorkshopProject>

    // Admin only from here on: a 403 would log a regular user out, so only call these when
    // TokenManager.isAdmin()
    @POST("api/v1/workshops")
    suspend fun createWorkshop(
        @Query("authorId") authorId: Long,
        @Body request: CreateWorkshopProjectRequest
    ): Response<WorkshopProject>

    // Changing the dates makes the backend recompute the status and resend the reminders;
    // unchanged dates keep a manually forced status
    @PUT("api/v1/workshops/{id}")
    suspend fun updateWorkshop(
        @Path("id") id: Long,
        @Body request: CreateWorkshopProjectRequest
    ): Response<WorkshopProject>

    @Multipart
    @POST("api/v1/workshops/{id}/attachments")
    suspend fun uploadWorkshopAttachment(
        @Path("id") id: Long,
        @Part file: MultipartBody.Part
    ): Response<WorkshopAttachment>

    @DELETE("api/v1/workshops/attachments/{attachmentId}")
    suspend fun deleteWorkshopAttachment(@Path("attachmentId") attachmentId: Long): Response<Unit>

    // PDF rendition of the attachment (images as-is); 415 when the format can't be previewed
    @Streaming
    @GET("api/v1/workshops/attachments/{attachmentId}/preview")
    suspend fun getWorkshopAttachmentPreview(@Path("attachmentId") attachmentId: Long): Response<ResponseBody>

    // Manual override, e.g. closing early once the form is full
    @PATCH("api/v1/workshops/{id}/status")
    suspend fun updateWorkshopStatus(
        @Path("id") id: Long,
        @Query("status") status: ProjectStatus
    ): Response<WorkshopProject>

    @DELETE("api/v1/workshops/{id}")
    suspend fun deleteWorkshop(@Path("id") id: Long): Response<Unit>

    // --- Admin statistics ---

    // Admin only: a 403 logs the user out, so never call it for other users
    @GET("api/v1/admin/analytics/dashboard")
    suspend fun getAdminStatistics(): Response<AdminStatistics>

    // Signed-in users only. Counts a student opening the file; the backend ignores admins and
    // reopenings within 30 minutes
    @POST("api/v1/files/{id}/views")
    suspend fun recordFileView(@Path("id") fileId: Long): Response<Unit>

    @GET("api/v1/users/{userId}/favorites/{fileId}/status")
    suspend fun isFileFavorited(
        @Path("userId") userId: Long,
        @Path("fileId") fileId: Long
    ): Response<Map<String, Boolean>>
}