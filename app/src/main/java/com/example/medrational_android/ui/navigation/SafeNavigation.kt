package com.example.medrational_android.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

// A screen is RESUMED only once its enter transition has finished. While screens change, the old
// one is still drawn and still takes taps: back then ☰ (same corner) popped twice, emptying the
// back stack and leaving a blank white page
internal fun acceptsNavigation(currentState: Lifecycle.State?): Boolean =
    currentState == Lifecycle.State.RESUMED

/** Back from a tap or a finished screen; ignored while screens are changing. */
fun NavController.popBackStackSafely(): Boolean =
    acceptsNavigation(currentBackStackEntry?.lifecycle?.currentState) && popBackStack()

/** Opens [route] from a tap; ignored while screens are changing, so a double tap opens it once. */
fun NavController.navigateSafely(route: String) {
    if (acceptsNavigation(currentBackStackEntry?.lifecycle?.currentState)) navigate(route)
}
