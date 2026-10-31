package com.example.medrational_android.ui.reasonings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.medrational_android.R
import com.example.medrational_android.ui.theme.SwisraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The category's description is introduced by a heading, not left as loose text. */
@RunWith(AndroidJUnit4::class)
class CategoryAboutCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theDescriptionHasItsHeading() {
        val description = "Toute la cardiologie de l'externat."
        composeRule.setContent {
            SwisraTheme(darkTheme = false) {
                Box(modifier = Modifier.width(360.dp)) {
                    CategoryAboutCard(description = description, categoryId = 1)
                }
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.category_about)).assertIsDisplayed()
        composeRule.onNodeWithText(description).assertIsDisplayed()
    }
}
