package com.example.medrational_android.ui.text

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Text a ViewModel shows, as a string resource so that it's read in the app's language when it is
 * displayed. ViewModels have no Context, so they never hold the translated string themselves.
 * An argument can itself be a [UiText], e.g. "Added %1$s to %2$s" with "2 tags" and "3 files".
 */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = listOf(count)) : UiText

    fun asString(context: Context): String = when (this) {
        is Res -> if (args.isEmpty()) context.getString(id) else context.getString(id, *resolve(args, context))
        is Plural -> context.resources.getQuantityString(id, count, *resolve(args, context))
    }

    private fun resolve(args: List<Any>, context: Context): Array<Any> =
        args.map { if (it is UiText) it.asString(context) else it }.toTypedArray()
}

@Composable
fun UiText.asString(): String = asString(LocalContext.current)
