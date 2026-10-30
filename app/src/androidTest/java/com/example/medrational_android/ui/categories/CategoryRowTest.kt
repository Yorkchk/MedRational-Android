package com.example.medrational_android.ui.categories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.medrational_android.R
import com.example.medrational_android.data.model.Category
import com.example.medrational_android.ui.theme.SwisraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A category's description must be readable on the home screen, not cut after a few words. */
@RunWith(AndroidJUnit4::class)
class CategoryRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val description =
        "Toute la cardiologie de l'externat : douleur thoracique, dyspnée, syncope, " +
            "insuffisance cardiaque et troubles du rythme, avec les raisonnements cliniques associés."

    private fun showRow(isAdmin: Boolean, onClick: () -> Unit = {}) {
        val category = Category(id = 1, name = "Cardiologie", description = description)
        composeRule.setContent {
            SwisraTheme(darkTheme = false) {
                Box(modifier = Modifier.width(360.dp)) {
                    CategoryRow(category = category, isAdmin = isAdmin, onClick = onClick, onDeleteClick = {})
                }
            }
        }
    }

    @Test
    fun aPhoneShowsTheWholeDescription() {
        showRow(isAdmin = false)

        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(description, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        assertTrue("Description should take more than 2 lines, took ${layout.lineCount}", layout.lineCount > 2)
        assertFalse("Description was cut", layout.isLineEllipsized(layout.lineCount - 1))
    }

    @Test
    fun tappingTheRowOpensTheCategory() {
        var opened = 0
        showRow(isAdmin = false, onClick = { opened++ })

        composeRule.onNodeWithText("Cardiologie").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun onlyAdminsSeeTheDeleteButton() {
        showRow(isAdmin = false)
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.categories_delete)).assertCountEquals(0)
    }

    @Test
    fun adminsSeeTheDeleteButton() {
        showRow(isAdmin = true)
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.categories_delete)).assertCountEquals(1)
    }
}
