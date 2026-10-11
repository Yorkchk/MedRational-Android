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

    // Must match the backend's PushNotificationService and FcmPushNotificationService
    @Test
    fun `topic and channel match the backend`() {
        assertEquals("workshops", WorkshopPush.TOPIC)
        assertEquals("workshop_reminders", WorkshopPush.CHANNEL_ID)
    }
}
