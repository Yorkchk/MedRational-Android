package com.example.medrational_android.data.auth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Where the session lives; [TokenManager] in the app, a fake in tests. */
interface SessionStore {
    fun getToken(): String?
    fun getRefreshToken(): String?
    fun saveRenewedSession(token: String, refreshToken: String, role: String)
    fun clearToken()
}

/** What the backend answered to `POST /api/v1/auth/refresh`. */
sealed interface RenewResult {
    data class Renewed(val token: String, val refreshToken: String, val role: String) : RenewResult

    /** 401: the session is over (signed out, password changed, unused for 60 days). */
    data object Rejected : RenewResult

    /** Offline or server trouble: keep the session and try again later. */
    data object Unavailable : RenewResult
}

/**
 * Keeps the user signed in. The access token (JWT) lasts an hour; when it's refused or about to
 * expire, the refresh token is traded for a new pair. Only a rejected refresh token signs the user
 * out, so being offline never does.
 *
 * Thread-safe: requests that hit a 401 together renew once, and the others retry with the result.
 * [renew] blocks on the network, so callers must be off the main thread.
 */
class SessionRenewer(
    private val store: SessionStore,
    private val renew: (refreshToken: String) -> RenewResult,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val lock = Any()

    /**
     * After a 401 for a request sent with [sentToken]: the token to retry with, or null to give up.
     * With [signOutWhenOver], a rejected session clears the stored one.
     */
    fun tokenAfterUnauthorized(sentToken: String?, signOutWhenOver: Boolean = true): String? = synchronized(lock) {
        val current = store.getToken()
        // Another request renewed while this one waited: just retry with its token
        if (current != null && current != sentToken) return current
        renewLocked(signOutWhenOver)
    }

    /**
     * The access token, renewed first when it expires within [marginMillis]. For callers that hand
     * the token to something that can't renew it, like the system DownloadManager.
     */
    fun freshToken(marginMillis: Long = FIVE_MINUTES): String? = synchronized(lock) {
        val current = store.getToken() ?: return null
        val expiresAt = jwtExpiryMillis(current)
        if (expiresAt != null && expiresAt - nowMillis() > marginMillis) return current
        // If the server can't be reached, the current token may still be good enough
        renewLocked(signOutWhenOver = true) ?: store.getToken()
    }

    private fun renewLocked(signOutWhenOver: Boolean): String? {
        val refreshToken = store.getRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            // Signed in before refresh tokens existed, or signed out: nothing to renew
            if (signOutWhenOver) store.clearToken()
            return null
        }
        return when (val result = renew(refreshToken)) {
            is RenewResult.Renewed -> {
                store.saveRenewedSession(result.token, result.refreshToken, result.role)
                result.token
            }
            RenewResult.Rejected -> {
                if (signOutWhenOver) store.clearToken()
                null
            }
            RenewResult.Unavailable -> null
        }
    }

    companion object {
        const val FIVE_MINUTES = 5 * 60 * 1000L
    }
}

private val EXP_CLAIM = Regex("\"exp\"\\s*:\\s*(\\d+)")

/** When [jwt] expires, in epoch millis, from its `exp` claim; null when it can't be read. */
@OptIn(ExperimentalEncodingApi::class)
internal fun jwtExpiryMillis(jwt: String): Long? {
    val payload = jwt.split('.').getOrNull(1) ?: return null
    // JWTs drop the base64 padding, which the decoder needs
    val padded = payload.padEnd((payload.length + 3) / 4 * 4, '=')
    val json = try {
        Base64.UrlSafe.decode(padded).decodeToString()
    } catch (e: IllegalArgumentException) {
        return null
    }
    val seconds = EXP_CLAIM.find(json)?.groupValues?.get(1)?.toLongOrNull() ?: return null
    return seconds * 1000
}
