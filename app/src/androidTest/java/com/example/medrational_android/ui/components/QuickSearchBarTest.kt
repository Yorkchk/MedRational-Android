package com.example.medrational_android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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

    private var lastQuery = ""
    private var advancedClicks = 0

    // Typing opens the keyboard; left open, it takes the next tests' taps
    @After
    fun closeKeyboard() {
        Espresso.closeSoftKeyboard()
    }

    private fun showBar() {
        composeRule.setContent {
            var query by remember { mutableStateOf("") }
            SwisraTheme(darkTheme = false) {
                // A phone's width: the Advanced label must still fit next to the field
                Box(modifier = Modifier.width(360.dp)) {
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

    @Test
    fun typingFiltersAndTheCrossClears() {
        showBar()

        composeRule.onNodeWithText(placeholder).performTextInput("cardio")
        assertEquals("cardio", lastQuery)

        composeRule.onNodeWithContentDescription(context.getString(R.string.common_clear)).performClick()
        assertEquals("", lastQuery)
    }

    @Test
    fun theAdvancedButtonShowsItsLabelAndOpensTheFullSearch() {
        showBar()

        composeRule.onNodeWithText(context.getString(R.string.search_advanced_short)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.search_advanced)).performClick()

        assertEquals(1, advancedClicks)
    }
}
