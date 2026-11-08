package com.example.medrational_android.data.crash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportingTest {

    @Test
    fun `installed uat and prod builds report crashes`() {
        assertTrue(crashReportingEnabled(flavor = "uat", debug = false))
        assertTrue(crashReportingEnabled(flavor = "prod", debug = false))
    }

    // Android Studio runs and tests never fill the console with developers' crashes
    @Test
    fun `debug builds never report`() {
        listOf("dev", "uat", "prod").forEach { flavor ->
            assertFalse(flavor, crashReportingEnabled(flavor, debug = true))
        }
    }

    // CI builds devRelease on every PR; the dev environment is for local work only
    @Test
    fun `the dev environment never reports, even in a release build`() {
        assertFalse(crashReportingEnabled(flavor = "dev", debug = false))
    }

    // The test-crash button must never reach the Play Store app
    @Test
    fun `only the UAT release build offers the test crash`() {
        assertTrue(crashTestAvailable(flavor = "uat", debug = false))
        assertFalse(crashTestAvailable(flavor = "prod", debug = false))
        assertFalse(crashTestAvailable(flavor = "uat", debug = true))
        assertFalse(crashTestAvailable(flavor = "dev", debug = false))
    }

    @Test
    fun `the test crash throws an exception that is easy to spot in the console`() {
        val thrown = assertThrows(CrashReportingTestException::class.java) { CrashReporting.crashForTest() }

        assertEquals("Test crash from the UAT Profile screen", thrown.message)
    }
}
