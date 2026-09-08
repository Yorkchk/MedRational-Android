package com.example.medrational_android.data.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
class AndroidDownloader(private val context: Context) {

    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    fun downloadFile(url: String, fileName: String): Long {
        val cleanFileName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")

        val request = DownloadManager.Request(Uri.parse(url))
            .setMimeType(getMimeType(cleanFileName))
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(true)
            .setAllowedOverMetered(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setTitle(cleanFileName)
            .setDescription("Downloading study resource...")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, cleanFileName)

        val downloadId = downloadManager.enqueue(request)

        // Register receiver to automatically check status when completed/failed
        registerDownloadReceiver(downloadId, cleanFileName)

        return downloadId
    }

    private fun registerDownloadReceiver(downloadId: Long, fileName: String) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id == downloadId) {
                    checkStatus(downloadId, fileName)
                    context.unregisterReceiver(this)
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun checkStatus(downloadId: Long, fileName: String) {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor != null && cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    Log.d("DownloadManager", "Successfully downloaded $fileName")
                    Toast.makeText(context, "$fileName downloaded successfully", Toast.LENGTH_SHORT).show()
                }
                DownloadManager.STATUS_FAILED -> {
                    Log.e("DownloadManager", "Download failed for $fileName. Reason code: $reason")
                    val explanation = when (reason) {
                        DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume transfer (stream length issue)"
                        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "No external storage device found"
                        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
                        DownloadManager.ERROR_FILE_ERROR -> "Storage permission or file system error"
                        DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data parsing error"
                        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space"
                        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
                        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code (check backend 4xx/5xx)"
                        else -> "HTTP Error / Code: $reason"
                    }
                    Toast.makeText(context, "Download failed: $explanation", Toast.LENGTH_LONG).show()
                }
            }
            cursor.close()
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            fileName.endsWith(".zip", ignoreCase = true) -> "application/zip"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            else -> "application/octet-stream"
        }
    }
}