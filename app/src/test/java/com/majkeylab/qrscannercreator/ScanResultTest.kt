package com.majkeylab.qrscannercreator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanResultTest {
    @Test
    fun webUrlAcceptsOnlyHttpAndHttps() {
        assertEquals("https://example.com/path", safeWebUrl("https://example.com/path"))
        assertEquals("http://example.com", safeWebUrl("http://example.com"))
        assertNull(safeWebUrl("javascript:alert(1)"))
        assertNull(safeWebUrl("file:///data/user/0/private"))
        assertNull(safeWebUrl("not a url"))
    }

    @Test
    fun resultTypesExposeOneSafePrimaryAction() {
        assertEquals(ScanAction.OPEN_WEB, ScanResult.Web("https://example.com").action)
        assertEquals(ScanAction.CONNECT_WIFI, ScanResult.Wifi("Guest", "", WifiEncryption.OPEN).action)
        assertEquals(ScanAction.COPY, ScanResult.Text("hello").action)
    }

    @Test
    fun rawParserUnderstandsWifiAndEscapes() {
        val result = parseScanResult("WIFI:T:WPA;S:Cafe\\;5G;P:password123;H:false;;")

        assertEquals(
            ScanResult.Wifi(
                ssid = "Cafe;5G",
                password = "password123",
                encryption = WifiEncryption.WPA,
                raw = "WIFI:T:WPA;S:Cafe\\;5G;P:password123;H:false;;",
            ),
            result,
        )
    }

    @Test
    fun rawParserUnderstandsVcard() {
        val raw = "BEGIN:VCARD\nVERSION:3.0\nFN:Jane Doe\nTEL:+420123456789\nEMAIL:jane@example.com\nEND:VCARD"
        val result = parseScanResult(raw)

        assertEquals(
            ScanResult.Contact(
                name = "Jane Doe",
                phone = "+420123456789",
                email = "jane@example.com",
                organization = "",
                raw = raw,
            ),
            result,
        )
    }

    @Test
    fun rawParserKeepsUnsafeUriAsText() {
        assertEquals(ScanResult.Text("javascript:alert(1)"), parseScanResult("javascript:alert(1)"))
    }

    @Test
    fun rawParserReadsOpaqueMailtoQuery() {
        val raw = "mailto:jane@example.com?subject=Hello&body=See%20you"

        assertEquals(
            ScanResult.Email("jane@example.com", "Hello", "See you", raw),
            parseScanResult(raw),
        )
    }
}
