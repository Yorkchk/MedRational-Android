package com.example.medrational_android.data.api

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthorizeImageRequestTest {

    private val baseUrl = "http://10.0.2.2:8080/"

    private fun request(url: String) = Request.Builder().url(url).build()

    // Workshop attachment images need a signed-in user; Coil's client used to send no token, so the
    // backend refused them and the card showed a file icon instead of the image
    @Test
    fun `workshop attachment images carry the token`() {
        val image = request(workshopAttachmentUrl(baseUrl, 4, "preview"))

        val authorized = authorizeImageRequest(image, "jwt", baseUrl)

        assertEquals("Bearer jwt", authorized.header("Authorization"))
    }

    @Test
    fun `images from other hosts never see the token`() {
        val external = request("https://cdn.example.com/photo.png")

        assertNull(authorizeImageRequest(external, "jwt", baseUrl).header("Authorization"))
    }

    @Test
    fun `signed out, images go without a token`() {
        val image = request(workshopAttachmentUrl(baseUrl, 4, "preview"))

        assertNull(authorizeImageRequest(image, null, baseUrl).header("Authorization"))
        assertNull(authorizeImageRequest(image, "", baseUrl).header("Authorization"))
    }
}
