package com.example.medrational_android.ui.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.medrational_android.R
import com.example.medrational_android.data.model.Reasoning
import com.example.medrational_android.data.model.StudyFile
import com.example.medrational_android.ui.theme.SwisraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/** Download, Favorite and Share must stay on one line, even on a small phone. */
@RunWith(AndroidJUnit4::class)
class FileHeaderLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val file = StudyFile(
        id = 1,
        reasoningId = 2,
        fileName = "Cardiologie - insuffisance cardiaque aiguë et chronique.pdf",
        fileType = "application/pdf",
        publicUrl = "",
        fileSizeBytes = 2_400_000
    )

    private val reasoning = Reasoning(id = 2, categoryId = 3, title = "Raisonnement clinique", content = null)

    @Test
    fun actionsFitOnOneLine_whenNotFavorited() = assertActionsOnOneLine(isFavorited = false)

    @Test
    fun actionsFitOnOneLine_whenFavorited() = assertActionsOnOneLine(isFavorited = true)

    private fun assertActionsOnOneLine(isFavorited: Boolean) {
        composeRule.setContent {
            SwisraTheme(darkTheme = false) {
                Box(modifier = Modifier.width(320.dp)) {
                    FileHeader(
                        file = file,
                        reasoning = reasoning,
                        isFavorited = isFavorited,
                        onToggleFavorite = {},
                        onDownload = {},
                        isSharing = false,
                        onShare = {}
                    )
                }
            }
        }

        val download = composeRule.onNodeWithText(context.getString(R.string.common_download), useUnmergedTree = true)
        val layouts = mutableListOf<TextLayoutResult>()
        download.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertEquals("Download label wrapped", 1, layouts.single().lineCount)

        val favoriteLabel = context.getString(if (isFavorited) R.string.favorite_remove else R.string.favorite_add)
        val favorite = composeRule.onNodeWithContentDescription(favoriteLabel, useUnmergedTree = true)
        val share = composeRule.onNodeWithContentDescription(context.getString(R.string.common_share), useUnmergedTree = true)

        val downloadCenter = download.fetchSemanticsNode().boundsInRoot.center.y
        val tolerancePx = with(composeRule.density) { 2.dp.toPx() }
        listOf(favorite, share).forEach { node ->
            val center = node.fetchSemanticsNode().boundsInRoot.center.y
            assertTrue("Actions are not on one line", abs(center - downloadCenter) <= tolerancePx)
        }
    }
}
