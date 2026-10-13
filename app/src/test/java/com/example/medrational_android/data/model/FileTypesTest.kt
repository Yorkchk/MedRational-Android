package com.example.medrational_android.data.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileTypesTest {

    @Test
    fun `extension is lowercased and empty when missing`() {
        assertEquals("docx", fileExtension("Notes.DOCX"))
        assertEquals("", fileExtension("README"))
        assertEquals("", fileExtension(".hidden"))
        assertEquals("", fileExtension(null))
    }

    @Test
    fun `images are detected by content type or extension`() {
        assertTrue(isImageFile("scan.bin", "image/png"))
        assertTrue(isImageFile("ecg.JPG", null))
        assertFalse(isImageFile("notes.pdf", "application/pdf"))
    }

    @Test
    fun `common phone and web image formats are images even as octet-stream`() {
        for (name in listOf("a.jpeg", "a.png", "a.webp", "a.gif", "a.bmp", "a.HEIC", "a.heif", "a.avif")) {
            assertTrue(name, isImageFile(name, "application/octet-stream"))
        }
    }

    @Test
    fun `name without extension keeps dotted names intact`() {
        assertEquals("Cardio v1.2 notes", fileNameWithoutExtension("Cardio v1.2 notes.pdf"))
        assertEquals("README", fileNameWithoutExtension("README"))
    }

    @Test
    fun `file sizes use B, KB and MB`() {
        assertEquals("512 B", formatFileSize(512, Locale.ENGLISH))
        assertEquals("2 KB", formatFileSize(2048, Locale.ENGLISH))
        assertEquals("2.5 MB", formatFileSize(2_621_440, Locale.ENGLISH))
    }

    // French counts in octets and uses a decimal comma
    @Test
    fun `file sizes in French use o, Ko and Mo`() {
        assertEquals("512 o", formatFileSize(512, Locale.FRENCH))
        assertEquals("2 Ko", formatFileSize(2048, Locale.FRENCH))
        assertEquals("2,5 Mo", formatFileSize(2_621_440, Locale.FRENCH))
    }

    @Test
    fun `meta line joins type, size and upload date`() {
        assertEquals(
            "DOCX · 2 KB · 12 Mar 2026",
            fileMetaLine("notes.docx", null, 2048, "2026-03-12T10:15:30", Locale.ENGLISH)
        )
        assertEquals("PDF", fileMetaLine("a.pdf", "application/pdf", null, "not a date", Locale.ENGLISH))
        assertEquals("ZIP", fileMetaLine("bundle", "application/zip", 0, null, Locale.ENGLISH))
        assertEquals(
            "DOCX · 2 Ko · 12 mars 2026",
            fileMetaLine("notes.docx", null, 2048, "2026-03-12T10:15:30", Locale.FRENCH)
        )
    }
}
