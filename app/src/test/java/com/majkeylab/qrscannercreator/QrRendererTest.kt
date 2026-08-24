package com.majkeylab.qrscannercreator

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
    fun rendererCreatesContrastingQrWithQuietZone() {
        val style = QrStyle().parse().getOrThrow()
        val image = QrRenderer.renderPixels("https://example.com", style, 512)

        assertEquals(512 * 512, image.pixels.size)
        assertEquals(style.background, image.pixels.first())
        assertTrue(image.pixels.contains(style.foreground))
        assertTrue(image.pixels.contains(style.finder))
    }
}
