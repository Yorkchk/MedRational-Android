package com.example.medrational_android.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareFileNamesTest {

    @Test
    fun `keeps accents, spaces and the extension`() {
        assertEquals("Cardiologie – ECG été.pdf", shareFileName("Cardiologie – ECG été.pdf", 3))
    }

    @Test
    fun `replaces characters a file system rejects`() {
        assertEquals("a_b_c_d.pdf", shareFileName("""a/b\c:d.pdf""", 3))
    }

    @Test
    fun `never leaves a path or a hidden file`() {
        assertEquals("_.._etc", shareFileName("../../etc", 3))
        assertEquals("notes.pdf", shareFileName(".notes.pdf", 3))
    }

    @Test
    fun `falls back to the file id when the name is blank`() {
        assertEquals("file_9", shareFileName(null, 9))
        assertEquals("file_9", shareFileName("   ", 9))
    }

    @Test
    fun `prefers the backend's content type`() {
        assertEquals("application/pdf", shareMimeType("application/pdf", "notes.bin"))
    }

    @Test
    fun `guesses Office types from the extension`() {
        assertEquals(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            shareMimeType(null, "Cours.PPTX")
        )
        assertEquals("image/jpeg", shareMimeType("", "scan.jpeg"))
    }

    @Test
    fun `falls back to a generic binary type`() {
        assertEquals("application/octet-stream", shareMimeType(null, "archive.xyz"))
        assertEquals("application/octet-stream", shareMimeType("weird", "noextension"))
    }

    @Test
    fun `one type is kept as it is`() {
        assertEquals("application/pdf", commonMimeType(listOf("application/pdf")))
        assertEquals("application/pdf", commonMimeType(listOf("application/pdf", "application/pdf")))
    }

    @Test
    fun `types of one family share their family`() {
        assertEquals("image/*", commonMimeType(listOf("image/png", "image/jpeg")))
    }

    @Test
    fun `mixed types fall back to any type`() {
        assertEquals("*/*", commonMimeType(listOf("application/pdf", "image/png")))
    }
}
