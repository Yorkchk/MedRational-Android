package com.example.medrational.data.api

import com.example.medrational.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface MedRationalApi {

    // Public / Guest
    @GET("api/v1/categories")
    suspend fun getCategories(): Response<List<Category>>

    @GET("api/v1/categories/{id}")
    suspend fun getCategoryById(@Path("id") id: Long): Response<Category>

    @GET("api/v1/reasonings/category/{categoryId}")
    suspend fun getReasoningsByCategory(@Path("categoryId") categoryId: Long): Response<List<Reasoning>>

    // Auth
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<String>

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>
}