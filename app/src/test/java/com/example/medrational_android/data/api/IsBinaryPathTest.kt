package com.example.medrational_android.data.api

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsBinaryPathTest {

    @Test
    fun `previews and downloads are binary`() {
        assertTrue(isBinaryPath("/api/v1/files/4/preview"))
        assertTrue(isBinaryPath("/api/v1/downloads/file/4"))
    }

    @Test
    fun `JSON endpoints are not`() {
        assertFalse(isBinaryPath("/api/v1/files/4"))
        assertFalse(isBinaryPath("/api/v1/users/1/recent-downloads"))
    }
}
