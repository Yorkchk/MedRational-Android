package com.example.medrational_android.data.api

import android.content.Context
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.BuildConfig
import com.example.medrational_android.data.auth.RenewResult
import com.example.medrational_android.data.auth.SessionRenewer
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.download.shouldSendToken
import com.example.medrational_android.data.model.AuthResponse
import com.example.medrational_android.data.model.RefreshTokenRequest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import com.google.gson.GsonBuilder

/**
 * [request] with the user's token when it goes to our API (workshop attachment images need it), and
 * unchanged for any other host, which must never see the token.
 */
internal fun authorizeImageRequest(request: Request, token: String?, baseUrl: String): Request =
    if (!token.isNullOrBlank() && shouldSendToken(request.url.toString(), baseUrl)) {
        request.newBuilder().header("Authorization", "Bearer $token").build()
    } else {
        request
    }

/** Responses that are files rather than JSON: given a long read timeout and never body-logged. */
internal fun isBinaryPath(encodedPath: String): Boolean =
    encodedPath.endsWith("/preview") || encodedPath.contains("/api/v1/downloads/")

internal fun workshopAttachmentUrl(baseUrl: String, attachmentId: Long, action: String): String =
    "${baseUrl}api/v1/workshops/attachments/$attachmentId/$action"

/** `POST /auth/refresh` as a [RenewResult]: only a 401 ends the session; anything else is retried later. */
internal fun renewResultOf(code: Int, body: AuthResponse?): RenewResult {
    val refreshToken = body?.refreshToken
    return when {
        code in 200..299 && refreshToken != null -> RenewResult.Renewed(body.token, refreshToken, body.role)
        code == 401 -> RenewResult.Rejected
        else -> RenewResult.Unavailable
    }
}

object ApiClient {
    // Set per build type from local.properties (api.baseUrl / api.releaseBaseUrl), see app/build.gradle.kts.
    // Not `const`: a const would be inlined into every caller, and incremental builds would keep the old URL
    // after local.properties changes.
    val BASE_URL: String = BuildConfig.BASE_URL
    private var tokenManager: TokenManager? = null
    private var sessionRenewer: SessionRenewer? = null

    fun initialize(context: Context) {
        val manager = TokenManager(context.applicationContext)
        tokenManager = manager
        sessionRenewer = SessionRenewer(manager, ::renewOverNetwork)
    }

    /**
     * The access token, renewed first if it expires within minutes. Blocking: call it off the main
     * thread. For the system DownloadManager, which gets a token once and can't renew it.
     */
    fun freshAccessToken(): String? = sessionRenewer?.freshToken()

    /** Tells the backend this device signed out, so its session can't be renewed. Best effort. */
    fun endSessionOnServer() {
        val refreshToken = tokenManager?.getRefreshToken() ?: return
        thread(name = "sign-out") {
            try {
                sessionApi.logout(RefreshTokenRequest(refreshToken)).execute()
            } catch (e: IOException) {
                // Offline: the session simply expires on the server after 60 days unused
            }
        }
    }

    private fun renewOverNetwork(refreshToken: String): RenewResult = try {
        val response = sessionApi.refresh(RefreshTokenRequest(refreshToken)).execute()
        renewResultOf(response.code(), response.body())
    } catch (e: IOException) {
        RenewResult.Unavailable
    }

    // Images are loaded through the backend: their publicUrl points at the private R2 bucket
    fun filePreviewUrl(fileId: Long): String = "${BASE_URL}api/v1/files/$fileId/preview"

    // Workshop attachments too: their fileUrl is the private R2 endpoint, which answers 400
    fun workshopAttachmentPreviewUrl(attachmentId: Long): String =
        workshopAttachmentUrl(BASE_URL, attachmentId, "preview")

    fun workshopAttachmentDownloadUrl(attachmentId: Long): String =
        workshopAttachmentUrl(BASE_URL, attachmentId, "download")

    // Admin only, like every suggestion endpoint but sending one
    fun suggestionAttachmentPreviewUrl(suggestionId: Long): String =
        "${BASE_URL}api/v1/suggestions/$suggestionId/attachment/preview"

    fun suggestionAttachmentDownloadUrl(suggestionId: Long): String =
        "${BASE_URL}api/v1/suggestions/$suggestionId/attachment"

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        val token = tokenManager?.getToken()

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        // A 401 (expired token) never gets here unhandled: the authenticator renews the session and
        // retries, and signs out itself when the session is over. Offline renewals keep the user
        // signed in
        val response = chain.proceed(requestBuilder.build())

        // 403: signed in but not allowed, e.g. a student on an admin endpoint. Clears the stored
        // credentials, as before
        if (response.code == 403) {
            tokenManager?.clearToken()
            // The phone stays subscribed to workshop pushes: a signed-out user mustn't miss a
            // registration opening
        }

        response
    }

    private const val PREVIEW_READ_TIMEOUT_SECONDS = 120

    // Previews and shared files: binary bodies that can be large or slow to produce
    private fun Interceptor.Chain.isPreview() = isBinaryPath(request().url.encodedPath)

    // First-time Office-to-PDF conversion on the backend can take longer than the default timeout,
    // and so can fetching a large file to share
    private val previewTimeoutInterceptor = Interceptor { chain ->
        if (chain.isPreview()) {
            chain.withReadTimeout(PREVIEW_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS).proceed(chain.request())
        } else {
            chain.proceed(chain.request())
        }
    }

    // Full bodies (incl. JWTs) are logged in debug only
    private val bodyLogger = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    // Preview PDFs and shared files are only header-logged: dumping megabytes of binary into logcat takes seconds
    private val headerLogger = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
    }

    private val loggingInterceptor = Interceptor { chain ->
        if (chain.isPreview()) headerLogger.intercept(chain) else bodyLogger.intercept(chain)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(previewTimeoutInterceptor)
        .addInterceptor(loggingInterceptor)
        // On a 401: renew the session and retry once
        .authenticator(TokenRefreshAuthenticator({ sessionRenewer }, BASE_URL))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Coil's client. Images come from our API, and workshop attachments need the token. Unlike the API
     * client, a refused image doesn't sign the user out, and image bytes aren't logged.
     */
    val imageHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(authorizeImageRequest(chain.request(), tokenManager?.getToken(), BASE_URL))
            }
            // An expired token is renewed like on the API client, but a refused renewal leaves the
            // sign-out to the API client
            .authenticator(TokenRefreshAuthenticator({ sessionRenewer }, BASE_URL, signOutWhenOver = false))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    // Renewal and sign-out, without the authenticator: a refused renewal must not trigger another
    private val sessionApi: SessionApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(bodyLogger)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(SessionApi::class.java)
    }

    val api: MedRationalApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson)) // <--- pass lenient gson here
            .build()
            .create(MedRationalApi::class.java)
    }
}