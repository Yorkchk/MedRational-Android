package com.example.medrational_android.data.api

import com.example.medrational_android.data.model.AuthResponse
import com.example.medrational_android.data.model.RefreshTokenRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Session renewal and sign-out. Called from OkHttp's authenticator, which runs on OkHttp's own
 * thread and needs an answer before returning, so these calls are blocking. Served by a client
 * without the authenticator, so a refused renewal can't trigger another one.
 */
interface SessionApi {

    // 401 when the session is over: signed out, password changed, or unused for 60 days
    @POST("api/v1/auth/refresh")
    fun refresh(@Body request: RefreshTokenRequest): Call<AuthResponse>

    @POST("api/v1/auth/logout")
    fun logout(@Body request: RefreshTokenRequest): Call<Void>
}
