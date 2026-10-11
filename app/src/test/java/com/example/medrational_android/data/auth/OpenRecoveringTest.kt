package com.example.medrational_android.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import javax.crypto.AEADBadTagException

class OpenRecoveringTest {

    // Each call to open() pops the next outcome: a value, or an exception to throw
    private class FakeStorage(vararg outcomes: Any) {
        private val outcomes = ArrayDeque(outcomes.toList())
        val log = mutableListOf<String>()

        fun open(): String {
            log += "open"
            return when (val next = outcomes.removeFirst()) {
                is Exception -> throw next
                else -> next as String
            }
        }

        fun reset(name: String): () -> Unit = { log += name }
    }

    @Test
    fun `readable storage opens without resetting`() {
        val storage = FakeStorage("prefs")

        val prefs = openRecovering(storage::open, storage.reset("wipe file"))

        assertEquals("prefs", prefs)
        assertEquals(listOf("open"), storage.log)
    }

    // Regression: a prefs file restored without its Keystore key threw AEADBadTagException
    // from TokenManager's constructor and crashed the app on every launch
    @Test
    fun `undecryptable storage is reset and reopened`() {
        val storage = FakeStorage(AEADBadTagException(), "fresh prefs")

        val prefs = openRecovering(storage::open, storage.reset("wipe file"), storage.reset("new key"))

        assertEquals("fresh prefs", prefs)
        assertEquals(listOf("open", "wipe file", "open"), storage.log)
    }

    @Test
    fun `resets are tried in order until one works`() {
        val storage = FakeStorage(AEADBadTagException(), IOException("corrupt keyset"), "fresh prefs")

        val prefs = openRecovering(storage::open, storage.reset("wipe file"), storage.reset("new key"))

        assertEquals("fresh prefs", prefs)
        assertEquals(listOf("open", "wipe file", "open", "new key", "open"), storage.log)
    }

    @Test
    fun `last failure is rethrown when nothing helps`() {
        val last = AEADBadTagException("still broken")
        val storage = FakeStorage(AEADBadTagException(), last)

        val thrown = assertThrows(AEADBadTagException::class.java) {
            openRecovering(storage::open, storage.reset("wipe file"))
        }

        assertSame(last, thrown)
    }

    @Test
    fun `unrelated errors are not swallowed`() {
        val storage = FakeStorage(IllegalStateException("bug"))

        assertThrows(IllegalStateException::class.java) {
            openRecovering(storage::open, storage.reset("wipe file"))
        }
        assertEquals(listOf("open"), storage.log)
    }
}
