package com.example.medrational_android.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SessionRenewerTest {

    private class FakeStore(var jwt: String?, var refresh: String?, var role: String = "ROLE_USER") : SessionStore {
        var cleared = false
        override fun getToken() = jwt
        override fun getRefreshToken() = refresh
        override fun saveRenewedSession(token: String, refreshToken: String, role: String) {
            jwt = token
            refresh = refreshToken
            this.role = role
        }
        override fun clearToken() {
            cleared = true
            jwt = null
            refresh = null
        }
    }

    private val now = 1_800_000_000_000L

    private fun jwt(expiresAtMillis: Long): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"HS256"}""".toByteArray())
        val payload = encoder.encodeToString("""{"sub":"a@b.c","exp":${expiresAtMillis / 1000}}""".toByteArray())
        return "$header.$payload.signature"
    }

    @Test
    fun `an expired token is renewed and the new pair stored`() {
        val store = FakeStore("old-jwt", "refresh-1")
        val renewer = SessionRenewer(store, { RenewResult.Renewed("new-jwt", "refresh-2", "ROLE_ADMIN") })

        assertEquals("new-jwt", renewer.tokenAfterUnauthorized("old-jwt"))
        assertEquals("new-jwt", store.jwt)
        assertEquals("refresh-2", store.refresh)
        assertEquals("ROLE_ADMIN", store.role)
    }

    // Signed out, password changed elsewhere, or unused for 60 days
    @Test
    fun `a rejected renewal signs the user out`() {
        val store = FakeStore("old-jwt", "refresh-1")
        val renewer = SessionRenewer(store, { RenewResult.Rejected })

        assertNull(renewer.tokenAfterUnauthorized("old-jwt"))
        assertEquals(true, store.cleared)
    }

    // Offline must never sign anyone out
    @Test
    fun `an unreachable server keeps the session`() {
        val store = FakeStore("old-jwt", "refresh-1")
        val renewer = SessionRenewer(store, { RenewResult.Unavailable })

        assertNull(renewer.tokenAfterUnauthorized("old-jwt"))
        assertEquals(false, store.cleared)
        assertEquals("refresh-1", store.refresh)
    }

    // Image requests don't sign out; the API client does it
    @Test
    fun `a rejected renewal can leave the sign-out to someone else`() {
        val store = FakeStore("old-jwt", "refresh-1")
        val renewer = SessionRenewer(store, { RenewResult.Rejected })

        assertNull(renewer.tokenAfterUnauthorized("old-jwt", signOutWhenOver = false))
        assertEquals(false, store.cleared)
    }

    // Signed in with a version from before refresh tokens: one more sign-in, then it lasts
    @Test
    fun `without a refresh token the old session ends`() {
        val store = FakeStore("old-jwt", null)
        var called = false
        val renewer = SessionRenewer(store, { called = true; RenewResult.Rejected })

        assertNull(renewer.tokenAfterUnauthorized("old-jwt"))
        assertEquals(false, called)
        assertEquals(true, store.cleared)
    }

    @Test
    fun `a request that waited behind a renewal retries with the new token`() {
        val store = FakeStore("already-new-jwt", "refresh-2")
        var calls = 0
        val renewer = SessionRenewer(store, { calls++; RenewResult.Rejected })

        assertEquals("already-new-jwt", renewer.tokenAfterUnauthorized("old-jwt"))
        assertEquals(0, calls)
    }

    @Test
    fun `requests refused together renew only once`() {
        val store = FakeStore("old-jwt", "refresh-1")
        val calls = AtomicInteger()
        val renewer = SessionRenewer(store, {
            calls.incrementAndGet()
            Thread.sleep(50)
            RenewResult.Renewed("new-jwt", "refresh-2", "ROLE_USER")
        })
        val pool = Executors.newFixedThreadPool(5)
        val start = CountDownLatch(1)
        val results = (1..5).map { pool.submit<String?> { start.await(); renewer.tokenAfterUnauthorized("old-jwt") } }

        start.countDown()
        val tokens = results.map { it.get(5, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(1, calls.get())
        assertEquals(List(5) { "new-jwt" }, tokens)
    }

    @Test
    fun `a token valid for a while is used as is`() {
        val token = jwt(now + 30 * 60 * 1000)
        var calls = 0
        val renewer = SessionRenewer(FakeStore(token, "refresh-1"), { calls++; RenewResult.Rejected }, { now })

        assertEquals(token, renewer.freshToken())
        assertEquals(0, calls)
    }

    // DownloadManager gets the token once and can't renew it
    @Test
    fun `a token about to expire is renewed before a download`() {
        val renewer = SessionRenewer(
            FakeStore(jwt(now + 60 * 1000), "refresh-1"),
            { RenewResult.Renewed("new-jwt", "refresh-2", "ROLE_USER") },
            { now }
        )

        assertEquals("new-jwt", renewer.freshToken())
    }

    @Test
    fun `offline the current token is still used for a download`() {
        val token = jwt(now + 60 * 1000)
        val renewer = SessionRenewer(FakeStore(token, "refresh-1"), { RenewResult.Unavailable }, { now })

        assertEquals(token, renewer.freshToken())
    }

    @Test
    fun `signed out there is no token to hand out`() {
        assertNull(SessionRenewer(FakeStore(null, null), { RenewResult.Rejected }).freshToken())
    }

    @Test
    fun `the expiry is read from the token`() {
        assertEquals(now, jwtExpiryMillis(jwt(now)))
    }

    @Test
    fun `an unreadable token has no expiry`() {
        assertNull(jwtExpiryMillis("not-a-jwt"))
        assertNull(jwtExpiryMillis("a.!!!.c"))
    }
}
