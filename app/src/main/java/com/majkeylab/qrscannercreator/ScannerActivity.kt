package com.majkeylab.qrscannercreator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ScannerActivity : ComponentActivity() {
    private var cameraGranted by mutableStateOf(false)

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            cameraGranted = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        setContent {
            QrTheme {
                ScannerScreen(
                    cameraGranted = cameraGranted,
                    onRequestCamera = { permissionRequest.launch(Manifest.permission.CAMERA) },
                    onResult = ::finishWithResult,
                    onClose = ::finish,
                )
            }
        }
    }

    private fun finishWithResult(raw: String) {
        if (raw.isBlank()) return
        runOnUiThread {
            setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_RESULT, raw))
            finish()
        }
    }

    companion object {
        const val EXTRA_RESULT = "qr_result"
    }
}

@Composable
private fun ScannerScreen(
    cameraGranted: Boolean,
    onRequestCamera: () -> Unit,
    onResult: (String) -> Unit,
    onClose: () -> Unit,
) {
    var manualVisible by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf(false) }

    if (cameraGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreview(
                onResult = onResult,
                onError = { cameraError = true },
            )
            Box(
                modifier =
                    Modifier.align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                        .widthIn(max = 300.dp)
                        .aspectRatio(1f)
                        .border(3.dp, Color.White, RoundedCornerShape(18.dp)),
            )
            TextButton(
                onClick = onClose,
                modifier =
                    Modifier.align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 44.dp)
                        .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(18.dp)),
            ) {
                Text(
                    text = stringResource(R.string.scanner_close),
                    color = Color.White,
                )
            }
            Surface(
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .padding(24.dp)
                        .widthIn(max = 520.dp)
                        .fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.56f),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        stringResource(if (cameraError) R.string.camera_unavailable else R.string.point_at_qr),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedButton(
                        onClick = { manualVisible = true },
                        border = BorderStroke(1.dp, Color.White),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.enter_content_manually), color = Color.White)
                    }
                }
            }
        }
    } else {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.background,
                            ),
                        ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.padding(24.dp).widthIn(max = 480.dp).fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painterResource(R.drawable.ic_qr_code),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.camera_permission_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.camera_permission_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onRequestCamera,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(stringResource(R.string.allow_camera))
                    }
                    OutlinedButton(
                        onClick = { manualVisible = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(stringResource(R.string.enter_content_manually))
                    }
                    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.scanner_close))
                    }
                }
            }
        }
    }

    if (manualVisible) {
        ManualEntryDialog(
            onDismiss = { manualVisible = false },
            onResult = onResult,
        )
    }
}

@Composable
private fun ManualEntryDialog(
    onDismiss: () -> Unit,
    onResult: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.enter_content_manually)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= MAX_MANUAL_CONTENT_CHARS) value = it },
                label = { Text(stringResource(R.string.manual_content)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(18.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { onResult(value) }, enabled = value.isNotBlank()) {
                Text(stringResource(R.string.use_content))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun CameraPreview(
    onResult: (String) -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            provider?.unbindAll()
            executor.shutdownNow()
        }
    }

    AndroidView(
        factory = {
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                val providerFuture = ProcessCameraProvider.getInstance(context)
                providerFuture.addListener(
                    {
                        runCatching {
                            val cameraProvider = providerFuture.get().also { provider = it }
                            val preview = Preview.Builder().build().also { it.surfaceProvider = surfaceProvider }
                            val analysis =
                                ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { it.setAnalyzer(executor, QrAnalyzer(onResult)) }
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis,
                            )
                        }.onFailure { onError() }
                    },
                    ContextCompat.getMainExecutor(context),
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private class QrAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val found = AtomicBoolean(false)
    private val reader =
        MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.CHARACTER_SET to "UTF-8",
                    DecodeHintType.TRY_HARDER to true,
                ),
            )
        }
    private var luminance = ByteArray(0)

    override fun analyze(image: ImageProxy) {
        if (found.get()) {
            image.close()
            return
        }
        try {
            val bytes = image.copyLuminance() ?: return
            val source =
                PlanarYUVLuminanceSource(
                    bytes,
                    image.width,
                    image.height,
                    0,
                    0,
                    image.width,
                    image.height,
                    false,
                )
            val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            if (result.text.isNotBlank() && found.compareAndSet(false, true)) onResult(result.text)
        } catch (_: ReaderException) {
            // No QR code in this frame.
        } finally {
            reader.reset()
            image.close()
        }
    }

    private fun ImageProxy.copyLuminance(): ByteArray? {
        val plane = planes.firstOrNull() ?: return null
        val imageWidth = this.width
        val imageHeight = this.height
        val required = imageWidth * imageHeight
        if (luminance.size != required) luminance = ByteArray(required)
        val buffer = plane.buffer
        for (row in 0 until imageHeight) {
            val rowStart = row * plane.rowStride
            for (column in 0 until imageWidth) {
                val sourceIndex = rowStart + column * plane.pixelStride
                if (sourceIndex >= buffer.limit()) return null
                luminance[row * imageWidth + column] = buffer[sourceIndex]
            }
        }
        return luminance
    }
}

private const val MAX_MANUAL_CONTENT_CHARS = 4096
