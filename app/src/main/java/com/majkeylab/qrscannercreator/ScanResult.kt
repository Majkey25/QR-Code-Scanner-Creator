package com.majkeylab.qrscannercreator

import com.google.mlkit.vision.barcode.common.Barcode
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

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

    companion object {
        fun from(barcode: Barcode): ScanResult {
            val raw = barcode.rawValue?.trim().orEmpty()
            return when (barcode.valueType) {
                Barcode.TYPE_URL -> safeWebUrl(barcode.url?.url.orEmpty())?.let(::Web) ?: Text(raw)
                Barcode.TYPE_WIFI -> {
                    val wifi = barcode.wifi ?: return Text(raw)
                    val ssid = wifi.ssid?.trim().orEmpty()
                    if (ssid.isEmpty()) {
                        Text(raw)
                    } else {
                        Wifi(
                            ssid = ssid,
                            password = wifi.password.orEmpty(),
                            encryption =
                                when (wifi.encryptionType) {
                                    Barcode.WiFi.TYPE_OPEN -> WifiEncryption.OPEN
                                    Barcode.WiFi.TYPE_WPA -> WifiEncryption.WPA
                                    Barcode.WiFi.TYPE_WEP -> WifiEncryption.WEP
                                    else -> WifiEncryption.UNKNOWN
                                },
                            raw = raw,
                        )
                    }
                }
                Barcode.TYPE_CONTACT_INFO -> {
                    val contact = barcode.contactInfo ?: return Text(raw)
                    Contact(
                        name = contact.name?.formattedName.orEmpty(),
                        phone = contact.phones.firstOrNull()?.number.orEmpty(),
                        email = contact.emails.firstOrNull()?.address.orEmpty(),
                        organization = contact.organization.orEmpty(),
                        raw = raw,
                    )
                }
                Barcode.TYPE_PHONE ->
                    barcode.phone?.number?.takeIf(String::isNotBlank)?.let { Phone(it, raw) } ?: Text(raw)
                Barcode.TYPE_EMAIL ->
                    barcode.email?.address?.takeIf(String::isNotBlank)?.let {
                        Email(it, barcode.email?.subject.orEmpty(), barcode.email?.body.orEmpty(), raw)
                    } ?: Text(raw)
                Barcode.TYPE_SMS ->
                    barcode.sms?.phoneNumber?.takeIf(String::isNotBlank)?.let {
                        Sms(it, barcode.sms?.message.orEmpty(), raw)
                    } ?: Text(raw)
                Barcode.TYPE_GEO ->
                    barcode.geoPoint?.let { Geo(it.lat, it.lng, raw) } ?: Text(raw)
                Barcode.TYPE_CALENDAR_EVENT -> barcode.calendarEvent?.toScanResult(raw) ?: Text(raw)
                else -> Text(raw)
            }
        }
    }
}

internal fun safeWebUrl(value: String): String? =
    runCatching {
        val uri = URI(value.trim())
        value.trim().takeIf {
            uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
        }
    }.getOrNull()

private fun Barcode.CalendarEvent.toScanResult(raw: String): ScanResult.Event? {
    val startMillis = start?.toEpochMillis() ?: return null
    val endMillis = end?.toEpochMillis() ?: startMillis
    return ScanResult.Event(
        title = summary.orEmpty(),
        location = location.orEmpty(),
        description = description.orEmpty(),
        startMillis = startMillis,
        endMillis = maxOf(startMillis, endMillis),
        raw = raw,
    )
}

private fun Barcode.CalendarDateTime.toEpochMillis(): Long? =
    runCatching {
        val dateTime = LocalDateTime.of(year, month, day, hours, minutes, seconds)
        if (isUtc) {
            dateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        } else {
            dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }.getOrNull()
