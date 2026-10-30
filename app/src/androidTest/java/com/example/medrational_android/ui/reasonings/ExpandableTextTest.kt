package com.example.medrational_android.ui.reasonings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.medrational_android.R
import com.example.medrational_android.ui.theme.SwisraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Long descriptions are cut with Show more, and expand in full. */
@RunWith(AndroidJUnit4::class)
class ExpandableTextTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val showMore = context.getString(R.string.reasoning_show_more)
    private val showLess = context.getString(R.string.reasoning_show_less)

    private fun show(text: String) {
        composeRule.setContent {
            SwisraTheme(darkTheme = false) {
                Box(modifier = Modifier.width(360.dp)) {
                    ExpandableText(text = text, stateKey = "test")
                }
            }
        }
    }

    @Test
    fun aLongTextCanBeExpandedAndCollapsed() {
        show(List(40) { "Une phrase de la description." }.joinToString(" "))

        composeRule.onNodeWithText(showMore).performClick()
        composeRule.onNodeWithText(showLess).assertIsDisplayed()

        composeRule.onNodeWithText(showLess).performClick()
        composeRule.onNodeWithText(showMore).assertIsDisplayed()
    }

    @Test
    fun aShortTextHasNoToggle() {
        show("Une courte description.")

        composeRule.onAllNodesWithText(showMore).assertCountEquals(0)
    }
}
