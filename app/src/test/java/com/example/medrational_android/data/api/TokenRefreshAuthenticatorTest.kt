package com.example.medrational_android.data.api

import com.example.medrational_android.data.auth.RenewResult
import com.example.medrational_android.data.auth.SessionRenewer
import com.example.medrational_android.data.auth.SessionStore
import com.example.medrational_android.data.model.AuthResponse
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TokenRefreshAuthenticatorTest {

    private val baseUrl = "https://api.swisra.app/"

    private class Store(var jwt: String?, var refresh: String?) : SessionStore {
        override fun getToken() = jwt
        override fun getRefreshToken() = refresh
        override fun saveRenewedSession(token: String, refreshToken: String, role: String) {
            jwt = token
            refresh = refreshToken
        }
        override fun clearToken() {
            jwt = null
            refresh = null
        }
    }

    private fun unauthorized(url: String, token: String?, prior: Response? = null): Response {
        val request = Request.Builder().url(url).apply { token?.let { header("Authorization", "Bearer $it") } }.build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .priorResponse(prior)
            .build()
    }

    private fun authenticator(store: Store, result: RenewResult) =
        TokenRefreshAuthenticator({ SessionRenewer(store, { result }) }, baseUrl)

    @Test
    fun `an expired token is renewed and the request retried with the new one`() {
        val store = Store("old-jwt", "refresh-1")

        val retry = authenticator(store, RenewResult.Renewed("new-jwt", "refresh-2", "ROLE_USER"))
            .authenticate(null, unauthorized("${baseUrl}api/v1/users/me", "old-jwt"))

        assertEquals("Bearer new-jwt", retry?.header("Authorization"))
        assertEquals("${baseUrl}api/v1/users/me", retry?.url.toString())
    }

    @Test
    fun `a request already retried once is not retried again`() {
        val store = Store("new-jwt", "refresh-2")
        val first = unauthorized("${baseUrl}api/v1/users/me", "old-jwt")

        val retry = authenticator(store, RenewResult.Renewed("newer-jwt", "refresh-3", "ROLE_USER"))
            .authenticate(null, unauthorized("${baseUrl}api/v1/users/me", "new-jwt", prior = first))

        assertNull(retry)
    }

    // The token must never leave our API
    @Test
    fun `another host is never sent the token`() {
        val store = Store("old-jwt", "refresh-1")

        val retry = authenticator(store, RenewResult.Renewed("new-jwt", "refresh-2", "ROLE_USER"))
            .authenticate(null, unauthorized("https://example.com/image.png", null))

        assertNull(retry)
        assertEquals("old-jwt", store.jwt)
    }

    @Test
    fun `an ended session gives up and signs out`() {
        val store = Store("old-jwt", "refresh-1")

        val retry = authenticator(store, RenewResult.Rejected)
            .authenticate(null, unauthorized("${baseUrl}api/v1/users/me", "old-jwt"))

        assertNull(retry)
        assertNull(store.jwt)
    }

    @Test
    fun `before initialization there is nothing to renew`() {
        val retry = TokenRefreshAuthenticator({ null }, baseUrl)
            .authenticate(null, unauthorized("${baseUrl}api/v1/users/me", "old-jwt"))

        assertNull(retry)
    }

    private fun auth(refreshToken: String?) =
        AuthResponse("jwt", "ROLE_USER", 1L, "a@b.c", "Amina", "Session renewed", refreshToken)

    @Test
    fun `the refresh answer is read as renewed, rejected or unavailable`() {
        assertEquals(RenewResult.Renewed("jwt", "refresh-2", "ROLE_USER"), renewResultOf(200, auth("refresh-2")))
        assertEquals(RenewResult.Rejected, renewResultOf(401, null))
        // Server trouble isn't the end of the session
        assertEquals(RenewResult.Unavailable, renewResultOf(500, null))
        assertEquals(RenewResult.Unavailable, renewResultOf(503, null))
        // An older backend without refresh tokens: nothing usable
        assertEquals(RenewResult.Unavailable, renewResultOf(200, auth(null)))
    }
}
