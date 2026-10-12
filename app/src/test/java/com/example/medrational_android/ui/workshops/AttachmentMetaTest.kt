package com.example.medrational_android.ui.workshops

import com.example.medrational_android.viewmodel.PreviewState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class AttachmentMetaTest {

    private val pdf = PreviewState.Pdf(File("x.pdf"))

    @Test
    fun `a rendered document shows its type and page count`() {
        assertEquals("PDF · 3 pages", attachmentMeta("program.pdf", pdf, pageCount = 3, imageFailed = false))
        assertEquals("DOCX · 1 page", attachmentMeta("schedule.docx", pdf, pageCount = 1, imageFailed = false))
    }

    @Test
    fun `an image says so`() {
        val image = PreviewState.Image("https://api/preview")
        assertEquals("Image", attachmentMeta("room.png", image, pageCount = null, imageFailed = false))
        assertEquals("Image · Can't be shown", attachmentMeta("room.heic", image, pageCount = null, imageFailed = true))
    }

    @Test
    fun `other states explain the missing preview`() {
        assertEquals("DOCX · Preparing preview…", attachmentMeta("a.docx", PreviewState.Loading, null, false))
        assertEquals("DOCX · Preview unavailable", attachmentMeta("a.docx", PreviewState.Unavailable("x"), null, false))
        assertEquals("ZIP · No preview", attachmentMeta("a.zip", PreviewState.NotSupported, null, false))
        assertEquals("No preview", attachmentMeta("README", PreviewState.NotSupported, null, false))
    }
}
