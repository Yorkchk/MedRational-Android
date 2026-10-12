package com.example.medrational_android.data.api

import android.content.Context
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.BuildConfig
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.download.shouldSendToken
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
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

internal fun workshopAttachmentUrl(baseUrl: String, attachmentId: Long, action: String): String =
    "${baseUrl}api/v1/workshops/attachments/$attachmentId/$action"

object ApiClient {
    // Set per build type from local.properties (api.baseUrl / api.releaseBaseUrl), see app/build.gradle.kts.
    // Not `const`: a const would be inlined into every caller, and incremental builds would keep the old URL
    // after local.properties changes.
    val BASE_URL: String = BuildConfig.BASE_URL
    private var tokenManager: TokenManager? = null

    fun initialize(context: Context) {
        tokenManager = TokenManager(context.applicationContext)
    }

    // Images are loaded through the backend: their publicUrl points at the private R2 bucket
    fun filePreviewUrl(fileId: Long): String = "${BASE_URL}api/v1/files/$fileId/preview"

    // Workshop attachments too: their fileUrl is the private R2 endpoint, which answers 400
    fun workshopAttachmentPreviewUrl(attachmentId: Long): String =
        workshopAttachmentUrl(BASE_URL, attachmentId, "preview")

    fun workshopAttachmentDownloadUrl(attachmentId: Long): String =
        workshopAttachmentUrl(BASE_URL, attachmentId, "download")

    // Inside ApiClient.kt
    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        val token = tokenManager?.getToken()

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        // If the server rejects the token (user deleted or revoked), clear the stored credentials
        if (response.code == 401 || response.code == 403) {
            tokenManager?.clearToken()
            // The phone stays subscribed to workshop pushes: an expired session mustn't make users
            // miss a registration opening
        }

        response
    }

    private const val PREVIEW_READ_TIMEOUT_SECONDS = 120

    private fun Interceptor.Chain.isPreview() = request().url.encodedPath.endsWith("/preview")

    // First-time Office-to-PDF conversion on the backend can take longer than the default timeout
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

    // Preview PDFs are only header-logged: dumping megabytes of binary into logcat takes seconds
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
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    val api: MedRationalApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson)) // <--- pass lenient gson here
            .build()
            .create(MedRationalApi::class.java)
    }
}