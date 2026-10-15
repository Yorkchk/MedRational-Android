package com.example.medrational_android.ui.files

import com.example.medrational_android.R
import com.example.medrational_android.TestStrings.en
import com.example.medrational_android.TestStrings.fr
import com.example.medrational_android.ui.text.UiText
import com.example.medrational_android.viewmodel.PreviewState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class AttachmentMetaTest {

    private val pdf = PreviewState.Pdf(File("x.pdf"))

    @Test
    fun `a rendered document shows its type and page count`() {
        assertEquals("PDF · 3 pages", en(attachmentMeta("program.pdf", pdf, pageCount = 3, imageFailed = false)))
        assertEquals("DOCX · 1 page", en(attachmentMeta("schedule.docx", pdf, pageCount = 1, imageFailed = false)))
        assertEquals("PDF · 3 pages", fr(attachmentMeta("program.pdf", pdf, pageCount = 3, imageFailed = false)))
    }

    @Test
    fun `an image says so`() {
        val image = PreviewState.Image("https://api/preview")
        assertEquals("Image", en(attachmentMeta("room.png", image, pageCount = null, imageFailed = false)))
        assertEquals("Image · Can't be shown", en(attachmentMeta("room.heic", image, pageCount = null, imageFailed = true)))
    }

    @Test
    fun `other states explain the missing preview`() {
        val unavailable = PreviewState.Unavailable(UiText.Res(R.string.preview_error_network))
        assertEquals("DOCX · Preparing preview…", en(attachmentMeta("a.docx", PreviewState.Loading, null, false)))
        assertEquals("DOCX · Preview unavailable", en(attachmentMeta("a.docx", unavailable, null, false)))
        assertEquals("ZIP · No preview", en(attachmentMeta("a.zip", PreviewState.NotSupported, null, false)))
        assertEquals("No preview", en(attachmentMeta("README", PreviewState.NotSupported, null, false)))
        assertEquals("ZIP · Pas d’aperçu", fr(attachmentMeta("a.zip", PreviewState.NotSupported, null, false)))
    }
}
