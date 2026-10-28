package com.example.medrational_android.data.api

import com.example.medrational_android.data.auth.SessionRenewer
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp calls this on a 401: the access token expired (or was never there), so the session is
 * renewed and the request sent once more with the new token. Only requests to [baseUrl] are
 * retried, so the token never goes to another host. [renewer] is read on each 401, since the
 * clients are built before ApiClient.initialize runs.
 */
class TokenRefreshAuthenticator(
    private val renewer: () -> SessionRenewer?,
    private val baseUrl: String,
    private val signOutWhenOver: Boolean = true
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        if (!request.url.toString().startsWith(baseUrl)) return null
        // Already retried once with a renewed token: don't loop
        if (response.priorResponse != null) return null

        val sentToken = request.header(AUTHORIZATION)?.removePrefix(BEARER)
        val token = renewer()?.tokenAfterUnauthorized(sentToken, signOutWhenOver) ?: return null
        return request.newBuilder().header(AUTHORIZATION, BEARER + token).build()
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val BEARER = "Bearer "
    }
}
