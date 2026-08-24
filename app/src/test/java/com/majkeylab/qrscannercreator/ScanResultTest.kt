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
}
