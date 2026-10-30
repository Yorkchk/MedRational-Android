package com.example.medrational_android.ui.reasonings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.medrational_android.R
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.ui.theme.SwisraTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Selecting files must be visible on the card, not only behind a long-press. */
@RunWith(AndroidJUnit4::class)
class ReasoningItemCardSelectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val selectLabel = context.getString(R.string.file_select)

    private val reasoning = Reasoning(
        id = 1,
        categoryId = 2,
        title = "Douleur thoracique",
        content = null,
        files = listOf(
            StudyFile(id = 10, reasoningId = 1, fileName = "cours.pdf", fileType = "application/pdf", publicUrl = "", fileSizeBytes = 1_000),
            StudyFile(id = 11, reasoningId = 1, fileName = "fiche.pdf", fileType = "application/pdf", publicUrl = "", fileSizeBytes = 2_000)
        )
    )

    private fun showCard(selectionMode: Boolean, onStartSelection: () -> Unit = {}) {
        composeRule.setContent {
            SwisraTheme(darkTheme = false) {
                ReasoningItemCard(
                    reasoning = reasoning,
                    isAdmin = false,
                    onFileClick = {},
                    onFileLongClick = {},
                    selectionMode = selectionMode,
                    selectedFileIds = emptyList(),
                    onStartSelection = onStartSelection,
                    onDeleteReasoning = {},
                    favoritedIds = emptySet(),
                    ratings = emptyMap(),
                    onToggleFavorite = {},
                    onAddFile = {},
                    onDeleteFile = {},
                    onDownloadReasoningZip = {},
                    onDownloadSingleFile = {},
                    onShareReasoningZip = {},
                    onShareSingleFile = {},
                    onSelectFile = {}
                )
            }
        }
    }

    @Test
    fun theSelectButtonIsVisibleAndStartsSelection() {
        var started = 0
        showCard(selectionMode = false, onStartSelection = { started++ })

        composeRule.onNodeWithText(selectLabel).performClick()

        assertEquals(1, started)
    }

    @Test
    fun selectingShowsAnEmptyCheckboxOnEveryFile() {
        showCard(selectionMode = true)

        composeRule.onAllNodes(isToggleable() and isOff()).assertCountEquals(2)
        composeRule.onAllNodesWithText(selectLabel).assertCountEquals(0)
    }
}
