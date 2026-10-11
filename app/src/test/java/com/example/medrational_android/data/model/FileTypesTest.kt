package com.example.medrational_android.data.model

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
    fun `name without extension keeps dotted names intact`() {
        assertEquals("Cardio v1.2 notes", fileNameWithoutExtension("Cardio v1.2 notes.pdf"))
        assertEquals("README", fileNameWithoutExtension("README"))
    }

    @Test
    fun `file sizes use B, KB and MB`() {
        assertEquals("512 B", formatFileSize(512))
        assertEquals("2 KB", formatFileSize(2048))
        assertEquals("2.5 MB", formatFileSize(2_621_440))
    }

    @Test
    fun `meta line joins type, size and upload date`() {
        assertEquals(
            "DOCX · 2 KB · 12 Mar 2026",
            fileMetaLine("notes.docx", null, 2048, "2026-03-12T10:15:30")
        )
        assertEquals("PDF", fileMetaLine("a.pdf", "application/pdf", null, "not a date"))
        assertEquals("ZIP", fileMetaLine("bundle", "application/zip", 0, null))
    }
}
