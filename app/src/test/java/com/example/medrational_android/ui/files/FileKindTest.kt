package com.example.medrational_android.ui.files

import org.junit.Assert.assertEquals
import org.junit.Test

class FileKindTest {

    @Test
    fun `kind follows the extension`() {
        assertEquals(FileKind.PDF, fileKind("ecg.pdf", null))
        assertEquals(FileKind.DOCUMENT, fileKind("notes.DOCX", null))
        assertEquals(FileKind.SLIDES, fileKind("cours.pptx", null))
        assertEquals(FileKind.SHEET, fileKind("bilan.csv", null))
        assertEquals(FileKind.OTHER, fileKind("archive.zip", "application/zip"))
    }

    @Test
    fun `images are recognised by extension or content type`() {
        assertEquals(FileKind.IMAGE, fileKind("radio.jpg", null))
        assertEquals(FileKind.IMAGE, fileKind("scan", "image/png"))
    }
}
