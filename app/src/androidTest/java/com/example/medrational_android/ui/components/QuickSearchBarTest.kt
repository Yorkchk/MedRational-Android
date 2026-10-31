package com.example.medrational_android.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.medrational_android.R
import com.example.medrational_android.ui.theme.SwisraTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickSearchBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val placeholder = "Rechercher une catégorie"
    private val advancedLabel = context.getString(R.string.search_advanced_short)
    private val advancedDescription = context.getString(R.string.search_advanced)

    private var lastQuery = ""
    private var advancedClicks = 0

    // Typing opens the keyboard; left open, it takes the next tests' taps
    @After
    fun closeKeyboard() {
        Espresso.closeSoftKeyboard()
    }

    private fun showBar(widthDp: Int) {
        composeRule.setContent {
            var query by remember { mutableStateOf("") }
            val config = Configuration(LocalConfiguration.current).apply { screenWidthDp = widthDp }
            CompositionLocalProvider(LocalConfiguration provides config) {
                SwisraTheme(darkTheme = false) {
                    Box(modifier = Modifier.requiredWidth(widthDp.dp)) {
                        QuickSearchBar(
                            query = query,
                            onQueryChange = {
                                query = it
                                lastQuery = it
                            },
                            placeholder = placeholder,
                            onAdvancedSearch = { advancedClicks++ }
                        )
                    }
                }
            }
        }
    }

    @Test
    fun typingFiltersAndTheCrossClears() {
        showBar(widthDp = 360)

        composeRule.onNodeWithText(placeholder).performTextInput("cardio")
        assertEquals("cardio", lastQuery)

        composeRule.onNodeWithContentDescription(context.getString(R.string.common_clear)).performClick()
        assertEquals("", lastQuery)
    }

    // A phone keeps the field wide: Advanced is an icon at its end, without the label
    @Test
    fun aPhoneShowsAdvancedAsAnIconInTheField() {
        showBar(widthDp = 360)

        composeRule.onAllNodesWithText(advancedLabel).assertCountEquals(0)
        composeRule.onNodeWithContentDescription(advancedDescription).performClick()

        assertEquals(1, advancedClicks)
    }

    @Test
    fun aTabletShowsTheLabeledAdvancedButton() {
        // Tablet width that still fits the test device in portrait
        showBar(widthDp = 700)

        composeRule.onNodeWithText(advancedLabel).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(advancedDescription).performClick()

        assertEquals(1, advancedClicks)
    }
}
