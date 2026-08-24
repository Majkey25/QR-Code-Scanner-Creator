package com.majkeylab.qrscannercreator

import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class WifiSecurity(val wireValue: String) {
    OPEN("nopass"),
    WPA("WPA"),
}

enum class ModuleShape {
    SQUARE,
    ROUNDED,
    DOTS,
}

sealed interface QrDraft {
    fun payload(): Result<String>

    data class Text(val value: String) : QrDraft {
        override fun payload(): Result<String> = buildPayload { value.required("Text") }
    }

    data class Website(val value: String) : QrDraft {
        override fun payload(): Result<String> =
            buildPayload {
                val input = value.required("Website")
                val normalized = if ("://" in input) input else "https://$input"
                val uri = URI(normalized)
                require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) {
                    "Enter a valid http or https address"
                }
                normalized
            }
    }

    data class Wifi(
        val ssid: String,
        val password: String,
        val security: WifiSecurity,
        val hidden: Boolean,
    ) : QrDraft {
        override fun payload(): Result<String> =
            buildPayload {
                val network = ssid.required("Network name")
                if (security == WifiSecurity.WPA) {
                    require(password.length in 8..63) { "Wi-Fi password must contain 8 to 63 characters" }
                }
                val secret = if (security == WifiSecurity.OPEN) "" else password
                "WIFI:T:${security.wireValue};S:${network.escapeWifi()};P:${secret.escapeWifi()};H:$hidden;;"
            }
    }

    data class Contact(
        val name: String,
        val phone: String = "",
        val email: String = "",
        val organization: String = "",
    ) : QrDraft {
        override fun payload(): Result<String> =
            buildPayload {
                val lines = mutableListOf("BEGIN:VCARD", "VERSION:3.0", "FN:${name.required("Name").escapeVcard()}")
                if (organization.isNotBlank()) lines += "ORG:${organization.trim().escapeVcard()}"
                if (phone.isNotBlank()) lines += "TEL:${phone.trim().escapeVcard()}"
                if (email.isNotBlank()) lines += "EMAIL:${email.trim().escapeVcard()}"
                lines += "END:VCARD"
                lines.joinToString("\n")
            }
    }

    data class Email(
        val address: String,
        val subject: String = "",
        val body: String = "",
    ) : QrDraft {
        override fun payload(): Result<String> =
            buildPayload {
                val recipient = address.required("Email")
                val at = recipient.lastIndexOf('@')
                require(at in 1..<recipient.lastIndex) { "Enter a valid email address" }
                "MATMSG:TO:${recipient.escapeField()};SUB:${subject.trim().escapeField()};BODY:${body.trim().escapeField()};;"
            }
    }

    data class Phone(val number: String) : QrDraft {
        override fun payload(): Result<String> = buildPayload { "tel:${number.required("Phone number")}" }
    }

    data class Sms(
        val number: String,
        val message: String = "",
    ) : QrDraft {
        override fun payload(): Result<String> =
            buildPayload { "SMSTO:${number.required("Phone number")}:${message.trim().escapeField()}" }
    }

    data class Location(
        val latitude: String,
        val longitude: String,
    ) : QrDraft {
        override fun payload(): Result<String> =
            buildPayload {
                val lat = latitude.trim().toDoubleOrNull()
                val lon = longitude.trim().toDoubleOrNull()
                require(lat != null && lat in -90.0..90.0) { "Latitude must be between -90 and 90" }
                require(lon != null && lon in -180.0..180.0) { "Longitude must be between -180 and 180" }
                "geo:${latitude.trim()},${longitude.trim()}"
            }
    }

    data class Calendar(
        val title: String,
        val start: String,
        val end: String,
        val location: String = "",
        val description: String = "",
    ) : QrDraft {
        override fun payload(): Result<String> =
            buildPayload {
                val startTime = LocalDateTime.parse(start.trim())
                val endTime = LocalDateTime.parse(end.trim())
                require(endTime.isAfter(startTime)) { "End must be after start" }
                buildList {
                    add("BEGIN:VCALENDAR")
                    add("VERSION:2.0")
                    add("BEGIN:VEVENT")
                    add("SUMMARY:${title.required("Title").escapeVcard()}")
                    add("DTSTART:${startTime.format(QR_DATE_TIME)}")
                    add("DTEND:${endTime.format(QR_DATE_TIME)}")
                    if (location.isNotBlank()) add("LOCATION:${location.trim().escapeVcard()}")
                    if (description.isNotBlank()) add("DESCRIPTION:${description.trim().escapeVcard()}")
                    add("END:VEVENT")
                    add("END:VCALENDAR")
                }.joinToString("\n")
            }
    }
}

data class QrStyle(
    val foreground: String = "#0B57F0",
    val finder: String = "#111111",
    val background: String = "#FFFFFF",
    val moduleShape: ModuleShape = ModuleShape.ROUNDED,
    val finderShape: ModuleShape = ModuleShape.SQUARE,
) {
    fun parse(): Result<ParsedQrStyle> =
        buildPayload {
            val parsed =
                ParsedQrStyle(
                    foreground = foreground.parseColor(),
                    finder = finder.parseColor(),
                    background = background.parseColor(),
                    moduleShape = moduleShape,
                    finderShape = finderShape,
                )
            require(contrast(parsed.foreground, parsed.background) >= MIN_QR_CONTRAST) {
                "Foreground and background need more contrast"
            }
            require(contrast(parsed.finder, parsed.background) >= MIN_QR_CONTRAST) {
                "Finder and background need more contrast"
            }
            parsed
        }
}

data class ParsedQrStyle(
    val foreground: Int,
    val finder: Int,
    val background: Int,
    val moduleShape: ModuleShape,
    val finderShape: ModuleShape,
)

private val QR_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
private const val MIN_QR_CONTRAST = 3.0

private inline fun <T> buildPayload(block: () -> T): Result<T> = runCatching(block)

private fun String.required(label: String): String = trim().also { require(it.isNotEmpty()) { "$label is required" } }

private fun String.escapeWifi(): String = buildString {
    this@escapeWifi.forEach { character ->
        if (character in "\\;,:\"") append('\\')
        append(character)
    }
}

private fun String.escapeField(): String =
    replace("\\", "\\\\").replace(";", "\\;").replace(":", "\\:").replace(",", "\\,")

private fun String.escapeVcard(): String =
    replace("\\", "\\\\").replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")

private fun String.parseColor(): Int {
    require(length == 7 && first() == '#' && drop(1).all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        "Use a color in #RRGGBB format"
    }
    return (0xFF000000 or drop(1).toLong(16)).toInt()
}

private fun contrast(first: Int, second: Int): Double {
    val light = maxOf(first.luminance(), second.luminance())
    val dark = minOf(first.luminance(), second.luminance())
    return (light + 0.05) / (dark + 0.05)
}

private fun Int.luminance(): Double {
    fun channel(shift: Int): Double {
        val value = ((this ushr shift) and 0xFF) / 255.0
        return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}
