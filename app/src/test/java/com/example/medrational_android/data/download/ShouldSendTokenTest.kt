package com.example.medrational_android.data.download

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShouldSendTokenTest {

    private val baseUrl = "http://10.0.2.2:8080/"

    @Test
    fun `api downloads carry the token so the backend can record them`() {
        assertTrue(shouldSendToken("${baseUrl}api/v1/downloads/file/5", baseUrl))
        assertTrue(shouldSendToken("${baseUrl}api/v1/suggestions/3/attachment", baseUrl))
    }

    @Test
    fun `public storage urls never see the token`() {
        assertFalse(shouldSendToken("https://pub-123.r2.dev/WorkshopAttachments/Project_3/program.pdf", baseUrl))
    }

    // A look-alike host must not match on a shared prefix
    @Test
    fun `a host that only starts like the api does not get the token`() {
        assertFalse(shouldSendToken("http://10.0.2.2:80800/evil", baseUrl))
    }
}
