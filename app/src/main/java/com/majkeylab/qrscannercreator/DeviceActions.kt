package com.majkeylab.qrscannercreator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings

enum class ActionOutcome {
    STARTED,
    COPIED,
    WIFI_SUGGESTED,
    WIFI_PASSWORD_COPIED,
}

object DeviceActions {
    fun perform(context: Context, result: ScanResult): Result<ActionOutcome> =
        runCatching {
            when (result) {
                is ScanResult.Web -> context.launch(Intent(Intent.ACTION_VIEW, Uri.parse(result.url)))
                is ScanResult.Wifi -> connectWifi(context, result)
                is ScanResult.Contact -> context.launch(result.insertIntent())
                is ScanResult.Phone ->
                    context.launch(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", result.number, null)))
                is ScanResult.Email -> context.launch(result.emailIntent())
                is ScanResult.Sms -> context.launch(result.smsIntent())
                is ScanResult.Geo -> context.launch(result.mapsIntent())
                is ScanResult.Event -> context.launch(result.calendarIntent())
                is ScanResult.Text -> {
                    context.copy(result.raw)
                    ActionOutcome.COPIED
                }
            }
        }

    fun shareText(context: Context, value: String) {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, value)
            }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_qr_content)))
    }
}

private fun connectWifi(context: Context, wifi: ScanResult.Wifi): ActionOutcome {
    if (wifi.encryption in setOf(WifiEncryption.WEP, WifiEncryption.UNKNOWN)) {
        if (wifi.password.isNotEmpty()) context.copy(wifi.password)
        return context.launch(Intent(Settings.Panel.ACTION_WIFI)).let { ActionOutcome.WIFI_PASSWORD_COPIED }
    }

    val builder = WifiNetworkSuggestion.Builder().setSsid(wifi.ssid)
    if (wifi.encryption == WifiEncryption.WPA) builder.setWpa2Passphrase(wifi.password)
    val suggestion = builder.build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return context.launch(
            Intent(Settings.ACTION_WIFI_ADD_NETWORKS).putParcelableArrayListExtra(
                Settings.EXTRA_WIFI_NETWORK_LIST,
                arrayListOf(suggestion),
            ),
        )
    }

    val wifiManager = context.getSystemService(WifiManager::class.java)
    val status = wifiManager.addNetworkSuggestions(listOf(suggestion))
    require(
        status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS ||
            status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE,
    ) { "Android rejected the Wi-Fi network" }
    context.launch(Intent(Settings.Panel.ACTION_WIFI))
    return ActionOutcome.WIFI_SUGGESTED
}

private fun ScanResult.Contact.insertIntent(): Intent =
    Intent(Intent.ACTION_INSERT).apply {
        type = ContactsContract.Contacts.CONTENT_TYPE
        putExtra(ContactsContract.Intents.Insert.NAME, name)
        putExtra(ContactsContract.Intents.Insert.PHONE, phone)
        putExtra(ContactsContract.Intents.Insert.EMAIL, email)
        putExtra(ContactsContract.Intents.Insert.COMPANY, organization)
    }

private fun ScanResult.Email.emailIntent(): Intent =
    Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", address, null)).apply {
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

private fun ScanResult.Sms.smsIntent(): Intent =
    Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null)).apply {
        putExtra("sms_body", message)
    }

private fun ScanResult.Geo.mapsIntent(): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude"))

private fun ScanResult.Event.calendarIntent(): Intent =
    Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.EVENT_LOCATION, location)
        putExtra(CalendarContract.Events.DESCRIPTION, description)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
    }

private fun Context.copy(value: String) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("QR content", value))
}

private fun Context.launch(intent: Intent): ActionOutcome {
    require(intent.resolveActivity(packageManager) != null) { "No compatible app is installed" }
    startActivity(intent)
    return ActionOutcome.STARTED
}
