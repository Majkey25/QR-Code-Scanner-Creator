package com.majkeylab.qrscannercreator

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrRendererTest {
    @Test
    fun rendererRejectsTinyImages() {
        val error = runCatching { QrRenderer.renderPixels("hello", QrStyle().parse().getOrThrow(), 120) }
        assertTrue(error.isFailure)
    }

    @Test
    fun rendererRejectsContentBeyondSafeHighCorrectionCapacity() {
        val error = runCatching { QrRenderer.renderPixels("x".repeat(1201), QrStyle().parse().getOrThrow()) }
        assertTrue(error.isFailure)
    }

    @Test
    fun rendererCreatesContrastingQrWithQuietZone() {
        val style = QrStyle().parse().getOrThrow()
        val image = QrRenderer.renderPixels("https://example.com", style, 512)

        assertEquals(512 * 512, image.pixels.size)
        assertEquals(style.background, image.pixels.first())
        assertTrue(image.pixels.contains(style.foreground))
        assertTrue(image.pixels.contains(style.finder))
    }

    @Test
    fun defaultStyledQrDecodesBackToOriginalContent() {
        val content = "WIFI:T:WPA;S:Guest;P:password123;H:false;;"
        val image = QrRenderer.renderPixels(content, QrStyle().parse().getOrThrow(), 512)
        val source = RGBLuminanceSource(image.size, image.size, image.pixels)
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)))

        assertEquals(content, decoded.text)
    }
}
