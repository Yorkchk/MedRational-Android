package com.example.medrational_android.ui.suggestions

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.medrational_android.data.upload.PickedFile
import com.example.medrational_android.data.upload.describePickedFile
import com.example.medrational_android.data.upload.exceedsLimit
import com.example.medrational_android.data.upload.toPart
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.io.IOException

/**
 * Opens the system file picker and hands back the chosen file, already checked against the 10 MB
 * limit. Returns the function that opens the picker.
 */
@Composable
fun rememberAttachmentPicker(onPicked: (PickedFile) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val picked = try {
                describePickedFile(context, uri)
            } catch (e: IOException) {
                null
            } catch (e: SecurityException) {
                null
            }
            when {
                picked == null -> toast(context, "Couldn't read the selected file.")
                picked.exceedsLimit() -> toast(context, "Files must be 10 MB or smaller.")
                else -> onPicked(picked)
            }
        }
    }
    return { launcher.launch("*/*") }
}

/**
 * Reads a picked file for upload: success(null) when nothing was picked, failure (after telling the
 * user) when the file can't be read anymore.
 */
suspend fun readAttachment(context: Context, file: PickedFile?): Result<MultipartBody.Part?> {
    if (file == null) return Result.success(null)
    return try {
        Result.success(file.toPart(context, "file"))
    } catch (e: IOException) {
        toast(context, "Couldn't read ${file.name}.")
        Result.failure(e)
    } catch (e: SecurityException) {
        toast(context, "Couldn't read ${file.name}.")
        Result.failure(e)
    }
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
