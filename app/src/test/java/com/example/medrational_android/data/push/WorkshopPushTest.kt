package com.example.medrational_android.data.push

import com.example.medrational_android.BuildConfig
import com.example.medrational_android.TestStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkshopPushTest {

    // As the backend sends it: {"projectId": "5", "type": "WORKSHOP_LAUNCH"}
    @Test
    fun `a workshop push opens its project`() {
        assertEquals("workshops/5", pushRoute(mapOf("projectId" to "5", "type" to "WORKSHOP_LAUNCH")))
    }

    // NewFilesNotificationServiceImpl: one file names it and its reasoning
    @Test
    fun `a push about one new file opens its page`() {
        assertEquals("file/7/12", pushRoute(mapOf("type" to "NEW_FILES", "fileId" to "12", "reasoningId" to "7")))
    }

    @Test
    fun `a push about several new files opens what's new`() {
        assertEquals("novelties", pushRoute(mapOf("type" to "NEW_FILES")))
        // A file the app can't open still leads to the new files
        assertEquals("novelties", pushRoute(mapOf("type" to "NEW_FILES", "fileId" to "12")))
        assertEquals("novelties", pushRoute(mapOf("type" to "NEW_FILES", "fileId" to "abc", "reasoningId" to "7")))
    }

    @Test
    fun `push without a valid target opens nothing`() {
        assertNull(pushRoute(emptyMap()))
        assertNull(pushRoute(mapOf("projectId" to null)))
        assertNull(pushRoute(mapOf("projectId" to "abc")))
        assertNull(pushRoute(mapOf("projectId" to "0")))
        assertNull(pushRoute(mapOf("projectId" to "-3")))
        // A file id only counts in a new-files push
        assertNull(pushRoute(mapOf("fileId" to "12", "reasoningId" to "7")))
        assertNull(pushRoute(mapOf("type" to "SOMETHING_ELSE")))
    }

    @Test
    fun `a tapped push opens its screen when signed in`() {
        assertEquals("workshops/5", routeForPushTap("workshops/5", isLoggedIn = true))
        assertEquals("novelties", routeForPushTap("novelties", isLoggedIn = true))
    }

    // Signed-out phones get pushes too; the API needs a signed-in user, so login comes first
    @Test
    fun `a tapped push leads to login when signed out`() {
        assertEquals("login", routeForPushTap("workshops/5", isLoggedIn = false))
        assertEquals("login", routeForPushTap("file/7/12", isLoggedIn = false))
    }

    @Test
    fun `the project route matches the nav graph`() {
        assertEquals("workshops/12", workshopRoute(12))
    }

    // Must match PushMessage's channels on the backend
    @Test
    fun `channels match the backend`() {
        assertEquals("workshop_reminders", WorkshopPush.CHANNEL_ID)
        assertEquals("new_files", WorkshopPush.NEW_FILES_CHANNEL_ID)
    }

    // Must match push.workshops-topic in the backend's application-{dev,uat,prod}.yaml
    @Test
    fun `each environment has the backend's topic`() {
        assertEquals("workshops-dev", workshopsTopicFor("dev"))
        assertEquals("workshops-uat", workshopsTopicFor("uat"))
        assertEquals("workshops", workshopsTopicFor("prod"))
    }

    // A new flavor must not fall back to prod and notify real students
    @Test(expected = IllegalArgumentException::class)
    fun `an unknown environment has no topic`() {
        workshopsTopicFor("staging")
    }

    @Test
    fun `the build listens to its own environment's topic`() {
        assertEquals(workshopsTopicFor(BuildConfig.FLAVOR), WorkshopPush.TOPIC)
    }

    // A push shown while the app is open, translated like the ones Android shows by itself
    @Test
    fun `a push is shown in the app's language`() {
        val french = TestStrings.french.strings
        val text = localizedPushText("push_opened_fcfs_title", listOf("Suture"), "Spots Available Now: Suture") { french[it] }

        assertEquals("Places disponibles\u00a0: Suture", text)
    }

    // Args as NewFilesNotificationServiceImpl sends them: the file name, then its reasoning
    @Test
    fun `a new file push is shown in the app's language`() {
        val french = TestStrings.french.strings
        val args = listOf("ECG.pdf", "Insuffisance cardiaque")

        assertEquals("Nouveau fichier\u00a0: ECG.pdf", localizedPushText("push_new_file_title", args, "New file: ECG.pdf") { french[it] })
        assertEquals("Dans Insuffisance cardiaque", localizedPushText("push_new_file_body", args, "In") { french[it] })
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
