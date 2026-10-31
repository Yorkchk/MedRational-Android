package com.example.medrational_android.data.upload

import android.net.Uri
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadBatchesTest {

    private fun file(name: String, megabytes: Long?) =
        PickedFile(mockk<Uri>(), name, "application/pdf", megabytes?.let { it * MB })

    private fun names(batches: List<List<PickedFile>>) = batches.map { batch -> batch.map { it.name } }

    // One request, so students get one push
    @Test
    fun `small files share one request`() {
        val batches = uploadBatches(listOf(file("a", 1), file("b", 2), file("c", 3)))

        assertEquals(listOf(listOf("a", "b", "c")), names(batches))
    }

    // The backend refuses requests over 50 MB
    @Test
    fun `a new request starts before the size limit`() {
        val batches = uploadBatches(listOf(file("a", 20), file("b", 20), file("c", 10), file("d", 5)))

        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), names(batches))
    }

    @Test
    fun `a selection exactly at the limit stays in one request`() {
        val batches = uploadBatches(listOf(file("a", 20), file("b", 20), file("c", 5)))

        assertEquals(listOf(listOf("a", "b", "c")), names(batches))
    }

    @Test
    fun `files keep the picked order`() {
        val batches = uploadBatches(listOf(file("a", 15), file("b", 15), file("c", 15), file("d", 15)))

        assertEquals(listOf("a", "b", "c", "d"), names(batches).flatten())
    }

    @Test
    fun `a file of unknown size doesn't split the request`() {
        val batches = uploadBatches(listOf(file("a", 40), file("b", null)))

        assertEquals(listOf(listOf("a", "b")), names(batches))
    }

    @Test
    fun `nothing picked is no request`() {
        assertTrue(uploadBatches(emptyList()).isEmpty())
    }

    // Matches the backend's max-file-size; such files are left out before batching
    @Test
    fun `files over 20 MB are too big`() {
        assertTrue(file("big", 21).exceedsLimit(MAX_UPLOAD_FILE_BYTES))
        assertFalse(file("ok", 20).exceedsLimit(MAX_UPLOAD_FILE_BYTES))
        assertFalse(file("unknown", null).exceedsLimit(MAX_UPLOAD_FILE_BYTES))
    }

    private companion object {
        const val MB = 1024L * 1024
    }
}
