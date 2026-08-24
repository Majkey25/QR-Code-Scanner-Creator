package com.majkeylab.qrscannercreator

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer

internal fun decodeQrPixels(
    width: Int,
    height: Int,
    pixels: IntArray,
): String? {
    require(width > 0 && height > 0 && pixels.size == width * height)
    val reader =
        MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.CHARACTER_SET to "UTF-8",
                    DecodeHintType.TRY_HARDER to true,
                ),
            )
        }
    return try {
        val source = RGBLuminanceSource(width, height, pixels)
        reader.decode(BinaryBitmap(HybridBinarizer(source))).text.takeIf(String::isNotBlank)
    } catch (_: ReaderException) {
        null
    }
}
