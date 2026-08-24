package com.majkeylab.qrscannercreator

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

data class PickedPhone(
    val name: String,
    val number: String,
    val selectionId: Long = System.nanoTime(),
)

class MainActivity : ComponentActivity() {
    private var scanResult by mutableStateOf<ScanResult?>(null)
    private var scanError by mutableStateOf<String?>(null)
    private var actionNotice by mutableStateOf<String?>(null)
    private var actionError by mutableStateOf<String?>(null)
    private var pickedPhone by mutableStateOf<PickedPhone?>(null)
    private var pickedLogo by mutableStateOf<Bitmap?>(null)

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.getStringExtra(ScannerActivity.EXTRA_RESULT)?.let { raw ->
                    recordCompletedScan(this)
                    showScanInterstitial(this) {
                        scanResult = parseScanResult(raw)
                        scanError = null
                    }
                }
            }
        }

    private val phonePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data ?: return@registerForActivityResult
            val projection =
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                )
            runCatching {
                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    check(cursor.moveToFirst())
                    val name = cursor.getString(0).orEmpty()
                    val number = cursor.getString(1).orEmpty()
                    check(number.isNotBlank())
                    pickedPhone = PickedPhone(name, number)
                } ?: error("Contact was unavailable")
            }.onFailure { actionError = getString(R.string.contact_pick_failed) }
        }

    private val logoPicker =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) return@registerForActivityResult
            runCatching {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val largest = maxOf(info.size.width, info.size.height)
                    if (largest > MAX_LOGO_DIMENSION) {
                        val scale = MAX_LOGO_DIMENSION.toDouble() / largest
                        decoder.setTargetSize(
                            (info.size.width * scale).roundToInt(),
                            (info.size.height * scale).roundToInt(),
                        )
                    }
                }
            }.onSuccess { pickedLogo = it }
                .onFailure { actionError = getString(R.string.pick_image_failed) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QrApp(
                scanResult = scanResult,
                scanError = scanError,
                actionNotice = actionNotice,
                actionError = actionError,
                pickedPhone = pickedPhone,
                pickedLogo = pickedLogo,
                onScan = {
                    scanError = null
                    actionError = null
                    scannerLauncher.launch(Intent(this, ScannerActivity::class.java))
                },
                onPerformAction = ::performAction,
                onCopy = { performAction(ScanResult.Text(it)) },
                onShareText = { DeviceActions.shareText(this, it) },
                onPickPhone = {
                    phonePicker.launch(
                        Intent(Intent.ACTION_PICK).apply {
                            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                        },
                    )
                },
                onPickLogo = {
                    logoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onClearLogo = { pickedLogo = null },
                onShareImage = { image ->
                    runCatching { QrRenderer.share(this, image) }
                        .onFailure { actionError = getString(R.string.action_failed) }
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        PremiumController.refresh(this)
    }

    private fun performAction(result: ScanResult) {
        actionError = null
        actionNotice = null
        DeviceActions.perform(this, result)
            .onSuccess { outcome ->
                actionNotice =
                    when {
                        outcome == ActionOutcome.COPIED -> getString(R.string.copied)
                        result !is ScanResult.Wifi -> null
                        outcome == ActionOutcome.WIFI_PASSWORD_COPIED ->
                            getString(R.string.wifi_password_copied)
                        else -> getString(R.string.wifi_ready)
                    }
            }
            .onFailure { actionError = getString(R.string.action_failed) }
    }

    private companion object {
        const val MAX_LOGO_DIMENSION = 1024
    }
}
