package com.example.medrational_android.data.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    // Must match the backend's PushNotificationService and FcmPushNotificationService
    @Test
    fun `topic and channel match the backend`() {
        assertEquals("workshops", WorkshopPush.TOPIC)
        assertEquals("workshop_reminders", WorkshopPush.CHANNEL_ID)
    }
}
