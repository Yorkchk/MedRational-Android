package com.example.medrational_android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * uat and prod must only reach their backend over HTTPS, so the JWT never travels in clear text,
 * while dev still reaches a local http:// backend. Unit tests only run the dev flavor, so this reads
 * the source files rather than the merged build.
 */
class NetworkSecurityConfigTest {

    private val androidNs = "http://schemas.android.com/apk/res/android"

    private fun cleartextPermitted(file: File): String {
        assertTrue("Missing ${file.path}", file.exists())
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val baseConfig = document.getElementsByTagName("base-config").item(0)
        return baseConfig.attributes.getNamedItem("cleartextTrafficPermitted").nodeValue
    }

    @Test
    fun `uat and prod forbid cleartext traffic`() {
        assertEquals("false", cleartextPermitted(File("src/main/res/xml/network_security_config.xml")))
        assertFalse(File("src/uat/res/xml/network_security_config.xml").exists())
        assertFalse(File("src/prod/res/xml/network_security_config.xml").exists())
    }

    @Test
    fun `dev allows cleartext traffic to a local backend`() {
        assertEquals("true", cleartextPermitted(File("src/dev/res/xml/network_security_config.xml")))
    }

    @Test
    fun `the manifest uses the network security config`() {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val application = factory.newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
            .getElementsByTagName("application").item(0) as Element
        assertEquals("@xml/network_security_config", application.getAttributeNS(androidNs, "networkSecurityConfig"))
        // On API 24+ the config wins anyway, but a leftover flag would mislead readers
        assertFalse(application.hasAttributeNS(androidNs, "usesCleartextTraffic"))
    }
}
