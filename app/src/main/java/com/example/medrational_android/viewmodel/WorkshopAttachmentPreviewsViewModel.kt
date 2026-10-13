package com.example.medrational_android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medrational_android.R
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.api.MedRationalApi
import com.example.medrational_android.data.model.WorkshopAttachment
import com.example.medrational_android.data.model.isImageFile
import com.example.medrational_android.data.preview.saveToCache
import com.example.medrational_android.ui.text.UiText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.io.IOException

/** The preview of each attachment on a project page, by attachment id. */
class WorkshopAttachmentPreviewsViewModel(
    private val api: MedRationalApi,
    // Converted PDFs are cached here as workshop-{attachmentId}.pdf so reopening a project is instant
    private val previewDir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _previews = MutableStateFlow<Map<Long, PreviewState>>(emptyMap())
    val previews: StateFlow<Map<Long, PreviewState>> = _previews.asStateFlow()

    /** Starts the previews of attachments not seen yet; removed ones are dropped. */
    fun load(attachments: List<WorkshopAttachment>) {
        val known = _previews.value
        val toFetch = attachments.filter { it.id !in known && it.previewable && !isImage(it) }
        _previews.value = attachments.associate { attachment ->
            attachment.id to (known[attachment.id] ?: initialPreview(attachment))
        }
        toFetch.forEach { loadPdf(it.id) }
    }

    fun retry(attachmentId: Long) {
        if (_previews.value[attachmentId] !is PreviewState.Unavailable) return
        setPreview(attachmentId, PreviewState.Loading)
        loadPdf(attachmentId)
    }

    // The cached PDF couldn't be opened (e.g. truncated); drop it so a retry downloads it again
    fun onPreviewCorrupted(attachmentId: Long) {
        val preview = _previews.value[attachmentId] as? PreviewState.Pdf ?: return
        preview.file.delete()
        setPreview(attachmentId, PreviewState.Unavailable(UiText.Res(R.string.preview_error_corrupted)))
    }

    private fun isImage(attachment: WorkshopAttachment) = isImageFile(attachment.fileName, null)

    private fun initialPreview(attachment: WorkshopAttachment): PreviewState = when {
        isImage(attachment) -> PreviewState.Image(ApiClient.workshopAttachmentPreviewUrl(attachment.id))
        attachment.previewable -> PreviewState.Loading
        else -> PreviewState.NotSupported
    }

    private fun loadPdf(attachmentId: Long) {
        viewModelScope.launch {
            val cached = File(previewDir, "workshop-$attachmentId.pdf")
            if (cached.exists() && cached.length() > 0) {
                setPreview(attachmentId, PreviewState.Pdf(cached))
                return@launch
            }
            val preview = try {
                val response = api.getWorkshopAttachmentPreview(attachmentId)
                val body = response.body()
                when {
                    response.isSuccessful && body != null -> PreviewState.Pdf(saveToCache(body, cached, ioDispatcher))
                    response.code() == 415 -> PreviewState.NotSupported
                    response.code() == 503 -> PreviewState.Unavailable(UiText.Res(R.string.preview_error_busy))
                    else -> PreviewState.Unavailable(UiText.Res(R.string.preview_error_http, listOf(response.code())))
                }
            } catch (e: IOException) {
                PreviewState.Unavailable(UiText.Res(R.string.preview_error_network))
            } catch (e: HttpException) {
                PreviewState.Unavailable(UiText.Res(R.string.preview_error_http, listOf(e.code())))
            }
            setPreview(attachmentId, preview)
        }
    }

    // Ignored once the attachment is gone, e.g. deleted while its preview was loading
    private fun setPreview(attachmentId: Long, preview: PreviewState) {
        _previews.update { previews ->
            if (attachmentId in previews) previews + (attachmentId to preview) else previews
        }
    }
}
