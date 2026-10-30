package com.example.medrational_android.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.medrational_android.ui.theme.SwisraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Card lists are one column on phones and two on tablets. */
@RunWith(AndroidJUnit4::class)
class AdaptiveCardListTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showList(widthDp: Int) {
        composeRule.setContent {
            val config = Configuration(LocalConfiguration.current).apply { screenWidthDp = widthDp }
            CompositionLocalProvider(LocalConfiguration provides config) {
                SwisraTheme(darkTheme = false) {
                    Box(modifier = Modifier.requiredWidth(widthDp.dp)) {
                        AdaptiveCardList {
                            items(listOf("Card 1", "Card 2", "Card 3", "Card 4")) { label ->
                                Text(label, modifier = Modifier.fillMaxWidth().height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bounds(label: String): DpRect = composeRule.onNodeWithText(label).getUnclippedBoundsInRoot()

    @Test
    fun aPhoneStacksTheCards() {
        showList(widthDp = 360)

        val first = bounds("Card 1")
        val second = bounds("Card 2")
        assertEquals(first.left, second.left)
        assertTrue("Card 2 should be below Card 1", second.top >= first.bottom)
    }

    @Test
    fun aTabletShowsTwoColumns() {
        showList(widthDp = 800)

        val first = bounds("Card 1")
        val second = bounds("Card 2")
        assertEquals(first.top, second.top)
        assertTrue("Card 2 should be right of Card 1", second.left >= first.right)
    }
}
