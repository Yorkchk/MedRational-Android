package com.example.medrational_android.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Regression: back then ☰ (same corner) during the transition popped home too, leaving a white page
@RunWith(AndroidJUnit4::class)
class SafeNavigationTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        rule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = "home") {
                composable("home") { Text("home") }
                composable("detail") { Text("detail") }
                composable("other") { Text("other") }
            }
        }
        rule.runOnIdle { navController.navigate("detail") }
        rule.waitForIdle()
    }

    @Test
    fun secondBackDuringTransitionKeepsHome() {
        // Freezes the transition, as when the second tap lands before it ends
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread {
            navController.popBackStackSafely()
            navController.popBackStackSafely()
        }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.runOnIdle { assertEquals("home", navController.currentDestination?.route) }
    }

    @Test
    fun backWorksAgainOnceTheTransitionEnds() {
        rule.runOnIdle { navController.navigate("other") }
        rule.waitForIdle()
        rule.runOnIdle { navController.popBackStackSafely() }
        rule.waitForIdle()
        rule.runOnIdle { navController.popBackStackSafely() }
        rule.waitForIdle()

        rule.runOnIdle { assertEquals("home", navController.currentDestination?.route) }
    }

    @Test
    fun doubleTapOpensAScreenOnce() {
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread {
            navController.navigateSafely("other")
            navController.navigateSafely("other")
        }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.runOnIdle {
            navController.popBackStackSafely()
        }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals("detail", navController.currentDestination?.route) }
    }
}
