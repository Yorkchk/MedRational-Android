package com.example.medrational_android.ui.files

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.medrational_android.R
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.ui.theme.Spacing
import com.example.medrational_android.viewmodel.ShareState
import com.example.medrational_android.viewmodel.ShareViewModel
import com.example.medrational_android.viewmodel.SharedItem
import com.example.medrational_android.viewmodel.commonMimeType

/**
 * Opens Android's share sheet for [items]: Gmail, Drive, OneDrive, Quick Share… each app handles its own
 * sign-in and upload. The files must sit in the cache's `shared/` folder (res/xml/file_paths.xml).
 */
fun Context.shareFiles(items: List<SharedItem>) {
    if (items.isEmpty()) return
    val uris = items.map { FileProvider.getUriForFile(this, "$packageName.fileprovider", it.file) }
    val send = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uris.single())
            // Prefills the subject when emailing it to yourself
            putExtra(Intent.EXTRA_SUBJECT, items.single().file.name)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
        }
    }.apply {
        type = commonMimeType(items.map { it.mimeType })
        // Lets the share sheet show a thumbnail and the receiving app read every file
        clipData = ClipData.newRawUri(items.first().file.name, uris.first()).also { clip ->
            uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, getString(R.string.share_no_app), Toast.LENGTH_LONG).show()
    }
}

/**
 * What every screen that shares does with [viewModel]'s state: a progress dialog while files are fetched,
 * then the share sheet, or a toast when it failed. [onShared] runs once the sheet has opened. A screen that
 * shows a single file's progress on its own button passes [singleFileProgressInline].
 */
@Composable
fun ShareEffects(
    viewModel: ShareViewModel,
    singleFileProgressInline: Boolean = false,
    onShared: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        when (val current = state) {
            is ShareState.Ready -> {
                context.shareFiles(current.items)
                if (current.failedCount > 0) {
                    val message = context.resources.getQuantityString(R.plurals.share_partial, current.failedCount, current.failedCount)
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                viewModel.onShareHandled()
                onShared()
            }
            is ShareState.Error -> {
                Toast.makeText(context, current.message.asString(context), Toast.LENGTH_LONG).show()
                viewModel.onShareHandled()
            }
            ShareState.Idle, is ShareState.Preparing -> Unit
        }
    }

    val preparing = state as? ShareState.Preparing
    if (preparing != null && !(singleFileProgressInline && preparing.total == 1)) {
        SharePreparingDialog(preparing = preparing, onCancel = viewModel::cancel)
    }
}

@Composable
private fun SharePreparingDialog(preparing: ShareState.Preparing, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.common_share)) },
        text = {
            Column {
                Text(
                    text = if (preparing.total == 0) {
                        stringResource(R.string.share_preparing_zip)
                    } else {
                        stringResource(R.string.share_preparing_files, preparing.done, preparing.total)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                val progressModifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.md)
                if (preparing.total == 0) {
                    LinearProgressIndicator(modifier = progressModifier)
                } else {
                    LinearProgressIndicator(
                        progress = { preparing.done.toFloat() / preparing.total },
                        modifier = progressModifier
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
