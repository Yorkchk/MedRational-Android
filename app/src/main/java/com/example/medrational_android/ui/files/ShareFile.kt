package com.example.medrational_android.ui.files

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.medrational_android.R
import java.io.File

/**
 * Opens Android's share sheet for [file]: Gmail, Drive, OneDrive, Quick Share… each app handles its own
 * sign-in and upload. The file must sit in the cache's `shared/` folder (res/xml/file_paths.xml).
 */
fun Context.shareFile(file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        // Prefills the subject when emailing it to yourself
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        // Lets the share sheet show a thumbnail and the receiving app read the file
        clipData = ClipData.newRawUri(file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, getString(R.string.share_no_app), Toast.LENGTH_LONG).show()
    }
}
