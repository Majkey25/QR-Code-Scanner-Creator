package com.majkeylab.qrscannercreator

import java.net.URI
import java.net.URLDecoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class ScanAction {
    OPEN_WEB,
    CONNECT_WIFI,
    ADD_CONTACT,
    DIAL,
    EMAIL,
    SMS,
    MAPS,
    ADD_EVENT,
    COPY,
}

enum class WifiEncryption {
    OPEN,
    WPA,
    WEP,
    UNKNOWN,
}

sealed interface ScanResult {
    val raw: String
    val action: ScanAction

    data class Web(val url: String) : ScanResult {
        override val raw: String = url
        override val action: ScanAction = ScanAction.OPEN_WEB
    }

    data class Wifi(
        val ssid: String,
        val password: String,
        val encryption: WifiEncryption,
        override val raw: String = ssid,
    ) : ScanResult {
        override val action: ScanAction = ScanAction.CONNECT_WIFI
    }

    data class Contact(
        val name: String,
        val phone: String,
        val email: String,
        val organization: String,
        override val raw: String,
    ) : ScanResult {
        override val action: ScanAction = ScanAction.ADD_CONTACT
    }

    data class Phone(
        val number: String,
        override val raw: String,
    ) : ScanResult {
        override val action: ScanAction = ScanAction.DIAL
    }

    data class Email(
        val address: String,
        val subject: String,
        val body: String,
        override val raw: String,
    ) : ScanResult {
        override val action: ScanAction = ScanAction.EMAIL
    }

    data class Sms(
        val number: String,
        val message: String,
        override val raw: String,
    ) : ScanResult {
        override val action: ScanAction = ScanAction.SMS
    }

    data class Geo(
        val latitude: Double,
        val longitude: Double,
        override val raw: String,
    ) : ScanResult {
        override val action: ScanAction = ScanAction.MAPS
    }

    data class Event(
        val title: String,
        val location: String,
        val description: String,
        val startMillis: Long,
        val endMillis: Long,
        override val raw: String,
    ) : ScanResult {
        override val action: ScanAction = ScanAction.ADD_EVENT
    }

    data class Text(override val raw: String) : ScanResult {
        override val action: ScanAction = ScanAction.COPY
    }
}

fun parseScanResult(value: String): ScanResult {
    val raw = value.trim()
    safeWebUrl(raw)?.let { return ScanResult.Web(it) }
    return when {
        raw.startsWith("WIFI:", ignoreCase = true) -> parseWifi(raw) ?: ScanResult.Text(raw)
        raw.startsWith("BEGIN:VCARD", ignoreCase = true) -> parseContact(raw) ?: ScanResult.Text(raw)
        raw.startsWith("MATMSG:", ignoreCase = true) -> parseMatmsg(raw) ?: ScanResult.Text(raw)
        raw.startsWith("mailto:", ignoreCase = true) -> parseMailto(raw) ?: ScanResult.Text(raw)
        raw.startsWith("tel:", ignoreCase = true) ->
            raw.substringAfter(':').takeIf(String::isNotBlank)?.let { ScanResult.Phone(it, raw) }
                ?: ScanResult.Text(raw)
        raw.startsWith("SMSTO:", ignoreCase = true) -> parseSms(raw, "SMSTO:") ?: ScanResult.Text(raw)
        raw.startsWith("sms:", ignoreCase = true) -> parseSms(raw, "sms:") ?: ScanResult.Text(raw)
        raw.startsWith("geo:", ignoreCase = true) -> parseGeo(raw) ?: ScanResult.Text(raw)
        raw.startsWith("BEGIN:VCALENDAR", ignoreCase = true) -> parseEvent(raw) ?: ScanResult.Text(raw)
        else -> ScanResult.Text(raw)
    }
}

internal fun safeWebUrl(value: String): String? =
    runCatching {
        val uri = URI(value.trim())
        value.trim().takeIf {
            uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
        }
    }.getOrNull()

private fun parseWifi(raw: String): ScanResult.Wifi? {
    val fields = parseFields(raw.substringAfter(':'))
    val ssid = fields["S"]?.takeIf(String::isNotBlank) ?: return null
    val encryption =
        when (fields["T"]?.uppercase()) {
            null, "", "NOPASS" -> WifiEncryption.OPEN
            "WPA", "WPA2", "WPA3", "SAE" -> WifiEncryption.WPA
            "WEP" -> WifiEncryption.WEP
            else -> WifiEncryption.UNKNOWN
        }
    return ScanResult.Wifi(ssid, fields["P"].orEmpty(), encryption, raw)
}

private fun parseContact(raw: String): ScanResult.Contact? {
    val fields = parseLines(raw)
    val name = fields.firstValue("FN")
    val phone = fields.firstValue("TEL")
    val email = fields.firstValue("EMAIL")
    val organization = fields.firstValue("ORG")
    if (listOf(name, phone, email, organization).all(String::isBlank)) return null
    return ScanResult.Contact(name, phone, email, organization, raw)
}

private fun parseMatmsg(raw: String): ScanResult.Email? {
    val fields = parseFields(raw.substringAfter(':'))
    val address = fields["TO"]?.takeIf(String::isNotBlank) ?: return null
    return ScanResult.Email(address, fields["SUB"].orEmpty(), fields["BODY"].orEmpty(), raw)
}

private fun parseMailto(raw: String): ScanResult.Email? =
    runCatching {
        val uri = URI(raw)
        val content = uri.rawSchemeSpecificPart
        val address = decode(content.substringBefore('?')).takeIf(String::isNotBlank) ?: return null
        val query = parseQuery(content.substringAfter('?', ""))
        ScanResult.Email(address, query["subject"].orEmpty(), query["body"].orEmpty(), raw)
    }.getOrNull()

private fun parseSms(raw: String, prefix: String): ScanResult.Sms? {
    val parts = splitEscaped(raw.substring(prefix.length), ':')
    val number = parts.firstOrNull()?.takeIf(String::isNotBlank) ?: return null
    return ScanResult.Sms(number, parts.drop(1).joinToString(":").unescape(), raw)
}

private fun parseGeo(raw: String): ScanResult.Geo? {
    val coordinates = raw.substringAfter(':').substringBefore('?').split(',', limit = 2)
    val latitude = coordinates.getOrNull(0)?.toDoubleOrNull() ?: return null
    val longitude = coordinates.getOrNull(1)?.toDoubleOrNull() ?: return null
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    return ScanResult.Geo(latitude, longitude, raw)
}

private fun parseEvent(raw: String): ScanResult.Event? =
    runCatching {
        val fields = parseLines(raw)
        val start = LocalDateTime.parse(fields.firstValue("DTSTART"), QR_DATE_TIME)
        val end = LocalDateTime.parse(fields.firstValue("DTEND"), QR_DATE_TIME)
        val zone = ZoneId.systemDefault()
        ScanResult.Event(
            title = fields.firstValue("SUMMARY"),
            location = fields.firstValue("LOCATION"),
            description = fields.firstValue("DESCRIPTION"),
            startMillis = start.atZone(zone).toInstant().toEpochMilli(),
            endMillis = end.atZone(zone).toInstant().toEpochMilli(),
            raw = raw,
        )
    }.getOrNull()

private fun parseFields(value: String): Map<String, String> =
    splitEscaped(value, ';').mapNotNull { field ->
        val separator = field.indexOfUnescaped(':')
        if (separator <= 0) {
            null
        } else {
            field.substring(0, separator).uppercase() to field.substring(separator + 1).unescape()
        }
    }.toMap()

private fun parseLines(value: String): List<Pair<String, String>> =
    value.lineSequence().mapNotNull { line ->
        val separator = line.indexOf(':')
        if (separator <= 0) {
            null
        } else {
            line.substring(0, separator).substringBefore(';').uppercase() to line.substring(separator + 1).unescape()
        }
    }.toList()

private fun List<Pair<String, String>>.firstValue(key: String): String =
    firstOrNull { it.first == key }?.second.orEmpty()

private fun parseQuery(value: String): Map<String, String> =
    value.split('&').mapNotNull { part ->
        val separator = part.indexOf('=')
        if (separator <= 0) null else decode(part.substring(0, separator)).lowercase() to decode(part.substring(separator + 1))
    }.toMap()

private fun splitEscaped(value: String, delimiter: Char): List<String> {
    val parts = mutableListOf<String>()
    val current = StringBuilder()
    var escaped = false
    value.forEach { character ->
        when {
            escaped -> {
                current.append('\\').append(character)
                escaped = false
            }
            character == '\\' -> escaped = true
            character == delimiter -> {
                parts += current.toString()
                current.clear()
            }
            else -> current.append(character)
        }
    }
    if (escaped) current.append('\\')
    parts += current.toString()
    return parts
}

private fun String.indexOfUnescaped(target: Char): Int {
    var escaped = false
    forEachIndexed { index, character ->
        when {
            escaped -> escaped = false
            character == '\\' -> escaped = true
            character == target -> return index
        }
    }
    return -1
}

private fun String.unescape(): String = buildString {
    var escaped = false
    this@unescape.forEach { character ->
        when {
            escaped -> {
                append(if (character == 'n') '\n' else character)
                escaped = false
            }
            character == '\\' -> escaped = true
            else -> append(character)
        }
    }
    if (escaped) append('\\')
}

private fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")

private val QR_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
