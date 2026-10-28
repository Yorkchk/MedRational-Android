package com.example.medrational_android.data.push

import com.example.medrational_android.TestStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkshopPushTest {

    // As the backend sends it: {"projectId": "5", "type": "WORKSHOP_LAUNCH"}
    @Test
    fun `push data names the project to open`() {
        assertEquals(5L, projectIdFromPush(mapOf("projectId" to "5", "type" to "WORKSHOP_LAUNCH")))
    }

    @Test
    fun `push without a valid project opens nothing`() {
        assertNull(projectIdFromPush(emptyMap()))
        assertNull(projectIdFromPush(mapOf("projectId" to null)))
        assertNull(projectIdFromPush(mapOf("projectId" to "abc")))
        assertNull(projectIdFromPush(mapOf("projectId" to "0")))
        assertNull(projectIdFromPush(mapOf("projectId" to "-3")))
    }

    @Test
    fun `a tapped push opens its project when signed in`() {
        assertEquals("workshops/5", routeForPushTap(5, isLoggedIn = true))
    }

    // Signed-out phones get pushes too; the workshops API needs a signed-in user, so login comes first
    @Test
    fun `a tapped push leads to login when signed out`() {
        assertEquals("login", routeForPushTap(5, isLoggedIn = false))
    }

    @Test
    fun `the project route matches the nav graph`() {
        assertEquals("workshops/12", workshopRoute(12))
    }

    // Must match the backend's FcmPushNotificationService
    @Test
    fun `channel matches the backend`() {
        assertEquals("workshop_reminders", WorkshopPush.CHANNEL_ID)
    }

    // Must match push.workshops-topic in the backend's application-{prod,uat,dev}.yaml
    @Test
    fun `environment topics match the backend profiles`() {
        assertEquals(listOf("workshops", "workshops-uat", "workshops-dev"), WorkshopPush.ENVIRONMENT_TOPICS)
    }

    // A phone that once ran a prod build must stop getting prod pushes on a dev build, and the reverse
    @Test
    fun `a build leaves the other environments' topics`() {
        assertEquals(listOf("workshops-uat", "workshops-dev"), topicsToLeave("workshops"))
        assertEquals(listOf("workshops", "workshops-uat"), topicsToLeave("workshops-dev"))
    }

    // Unit tests run against the debug build, which talks to the dev backend unless overridden
    @Test
    fun `the build's topic is one of the environments'`() {
        assertTrue(WorkshopPush.TOPIC in WorkshopPush.ENVIRONMENT_TOPICS)
        assertTrue(WorkshopPush.TOPIC !in topicsToLeave(WorkshopPush.TOPIC))
    }

    // A push shown while the app is open, translated like the ones Android shows by itself
    @Test
    fun `a push is shown in the app's language`() {
        val french = TestStrings.french.strings
        val text = localizedPushText("push_opened_fcfs_title", listOf("Suture"), "Spots Available Now: Suture") { french[it] }

        assertEquals("Places disponibles\u00a0: Suture", text)
    }

    @Test
    fun `an unknown key falls back to the backend's text`() {
        assertEquals("Spots Available Now: Suture", localizedPushText("push_new_kind_title", listOf("Suture"), "Spots Available Now: Suture") { null })
        assertEquals("Spots Available Now: Suture", localizedPushText(null, emptyList(), "Spots Available Now: Suture") { "unused" })
    }

    // A template expecting the project title must not crash when the backend sends no argument
    @Test
    fun `missing arguments fall back to the backend's text`() {
        assertEquals("Fallback", localizedPushText("push_opened_fcfs_title", emptyList(), "Fallback") { "Places : %1${'$'}s" })
    }
}
