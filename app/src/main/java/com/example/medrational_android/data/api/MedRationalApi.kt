package com.example.medrational_android.data.api

import com.example.medrational_android.data.model.*
import okhttp3.MultipartBody
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

    @DELETE("api/v1/files/{id}")
    suspend fun deleteFile(@Path("id") id: Long): Response<Unit>


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
}