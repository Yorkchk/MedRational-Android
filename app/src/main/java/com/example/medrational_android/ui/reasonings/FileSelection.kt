package com.example.medrational_android.ui.reasonings

import androidx.compose.runtime.saveable.Saver

/**
 * Files picked on a category's page, to share them (or tag them, for admins).
 * Selecting stays on with nothing picked, so the Select button can open it with empty checkboxes.
 */
data class FileSelection(val active: Boolean = false, val ids: List<Long> = emptyList()) {

    fun start(): FileSelection = copy(active = true)

    // Unpicking the last file keeps the selection open; the user leaves it with the close button
    fun toggle(id: Long): FileSelection = copy(active = true, ids = if (id in ids) ids - id else ids + id)

    fun clear(): FileSelection = FileSelection()

    companion object {
        // Saved across rotation: the active flag, then the ids
        val Saver: Saver<FileSelection, Any> = Saver(
            save = { listOf(it.active, it.ids) },
            restore = {
                val saved = it as List<*>
                FileSelection(active = saved[0] as Boolean, ids = (saved[1] as List<*>).map { id -> id as Long })
            }
        )
    }
}
