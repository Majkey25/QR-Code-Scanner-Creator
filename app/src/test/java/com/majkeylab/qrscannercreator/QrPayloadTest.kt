package com.majkeylab.qrscannercreator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrPayloadTest {
    @Test
    fun textRejectsBlankContent() {
        assertTrue(QrDraft.Text("  ").payload().isFailure)
    }

    @Test
    fun websiteAddsHttpsAndRejectsUnsafeSchemes() {
        assertEquals("https://example.com/path", QrDraft.Website("example.com/path").payload().getOrThrow())
        assertTrue(QrDraft.Website("javascript:alert(1)").payload().isFailure)
    }

    @Test
    fun wifiEscapesReservedCharacters() {
        val payload =
            QrDraft.Wifi(
                ssid = "Cafe;5G\\",
                password = "p:ass,word",
                security = WifiSecurity.WPA,
                hidden = true,
            ).payload().getOrThrow()

        assertEquals("WIFI:T:WPA;S:Cafe\\;5G\\\\;P:p\\:ass\\,word;H:true;;", payload)
    }

    @Test
    fun contactCreatesVcardWithEscapedFields() {
        val payload =
            QrDraft.Contact(
                name = "Doe; Jane",
                phone = "+420123456789",
                email = "jane@example.com",
                organization = "ACME, s.r.o.",
            ).payload().getOrThrow()

        assertEquals(
            "BEGIN:VCARD\nVERSION:3.0\nFN:Doe\\; Jane\nORG:ACME\\, s.r.o.\nTEL:+420123456789\nEMAIL:jane@example.com\nEND:VCARD",
            payload,
        )
    }

    @Test
    fun locationValidatesCoordinateRange() {
        assertEquals("geo:50.087,14.421", QrDraft.Location("50.087", "14.421").payload().getOrThrow())
        assertTrue(QrDraft.Location("91", "14.421").payload().isFailure)
        assertTrue(QrDraft.Location("north", "east").payload().isFailure)
    }

    @Test
    fun calendarRequiresEndAfterStart() {
        val valid =
            QrDraft.Calendar(
                title = "Dentist",
                start = "2026-08-24T15:00",
                end = "2026-08-24T15:30",
                location = "Prague",
                description = "Check-up",
            ).payload().getOrThrow()

        assertTrue(valid.contains("DTSTART:20260824T150000"))
        assertTrue(
            QrDraft.Calendar(
                title = "Dentist",
                start = "2026-08-24T15:30",
                end = "2026-08-24T15:00",
            ).payload().isFailure,
        )
    }

    @Test
    fun styleRejectsInvalidColorsAndWeakContrast() {
        assertTrue(QrStyle(foreground = "blue").parse().isFailure)
        assertTrue(QrStyle(foreground = "#777777", background = "#888888").parse().isFailure)
        assertEquals(0xFF0B57F0.toInt(), QrStyle().parse().getOrThrow().foreground)
    }
}
