package com.majkeylab.qrscannercreator

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import java.io.File
import java.io.FileOutputStream

data class QrImage(
    val size: Int,
    val pixels: IntArray,
    val background: Int,
) {
    fun toBitmap(): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        }

    fun withLogo(logo: Bitmap): QrImage {
        val output = pixels.copyOf()
        val logoSize = (size * 0.18f).toInt()
        val clearSize = (size * 0.23f).toInt()
        val clearStart = (size - clearSize) / 2
        for (y in 0 until clearSize) {
            for (x in 0 until clearSize) {
                if (ModuleShape.ROUNDED.contains(x, y, clearSize)) {
                    output[(clearStart + y) * size + clearStart + x] = background
                }
            }
        }

        val scaled = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)
        val logoPixels = IntArray(logoSize * logoSize)
        scaled.getPixels(logoPixels, 0, logoSize, 0, 0, logoSize, logoSize)
        val logoStart = (size - logoSize) / 2
        logoPixels.forEachIndexed { index, color ->
            if (color ushr 24 >= 128) {
                val x = index % logoSize
                val y = index / logoSize
                output[(logoStart + y) * size + logoStart + x] = color or 0xFF000000.toInt()
            }
        }
        return copy(pixels = output)
    }
}

object QrRenderer {
    fun renderPixels(payload: String, style: ParsedQrStyle, size: Int = 1024): QrImage {
        require(payload.isNotBlank()) { "QR content is required" }
        require(payload.toByteArray(Charsets.UTF_8).size <= MAX_QR_CONTENT_BYTES) {
            "QR content is too long"
        }
        require(size in 256..2048) { "QR image size must be between 256 and 2048 pixels" }

        val matrix =
            Encoder.encode(
                payload,
                ErrorCorrectionLevel.H,
                mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"),
            ).matrix
        val totalModules = matrix.width + QUIET_ZONE_MODULES * 2
        val moduleSize = size / totalModules
        val origin = (size - moduleSize * totalModules) / 2 + QUIET_ZONE_MODULES * moduleSize
        val pixels = IntArray(size * size) { style.background }

        for (moduleY in 0 until matrix.height) {
            for (moduleX in 0 until matrix.width) {
                if (matrix[moduleX, moduleY].toInt() != 1) continue
                val finder = isFinderModule(moduleX, moduleY, matrix.width)
                drawModule(
                    pixels = pixels,
                    imageSize = size,
                    left = origin + moduleX * moduleSize,
                    top = origin + moduleY * moduleSize,
                    moduleSize = moduleSize,
                    color = if (finder) style.finder else style.foreground,
                    shape = if (finder) style.finderShape else style.moduleShape,
                )
            }
        }
        return QrImage(size, pixels, style.background)
    }

    fun share(context: Context, image: QrImage) {
        val directory = File(context.cacheDir, "shared").also { check(it.exists() || it.mkdirs()) }
        val file = File(directory, "qr-code.png")
        FileOutputStream(file).use { output ->
            check(image.toBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "QR image could not be saved"
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("QR code", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_qr_code)))
    }
}

private const val QUIET_ZONE_MODULES = 4
private const val MAX_QR_CONTENT_BYTES = 1200

private fun isFinderModule(x: Int, y: Int, width: Int): Boolean =
    (x < 7 && y < 7) || (x >= width - 7 && y < 7) || (x < 7 && y >= width - 7)

private fun drawModule(
    pixels: IntArray,
    imageSize: Int,
    left: Int,
    top: Int,
    moduleSize: Int,
    color: Int,
    shape: ModuleShape,
) {
    for (localY in 0 until moduleSize) {
        for (localX in 0 until moduleSize) {
            if (shape.contains(localX, localY, moduleSize)) {
                pixels[(top + localY) * imageSize + left + localX] = color
            }
        }
    }
}

private fun ModuleShape.contains(x: Int, y: Int, size: Int): Boolean =
    when (this) {
        ModuleShape.SQUARE -> true
        ModuleShape.DOTS -> {
            val radius = size / 2.0
            val dx = x + 0.5 - radius
            val dy = y + 0.5 - radius
            dx * dx + dy * dy <= radius * radius
        }
        ModuleShape.ROUNDED -> {
            val radius = maxOf(1, size / 3)
            val innerEnd = size - radius
            if (x in radius until innerEnd || y in radius until innerEnd) {
                true
            } else {
                val centerX = if (x < radius) radius - 0.5 else innerEnd - 0.5
                val centerY = if (y < radius) radius - 0.5 else innerEnd - 0.5
                val dx = x - centerX
                val dy = y - centerY
                dx * dx + dy * dy <= radius * radius
            }
        }
    }
