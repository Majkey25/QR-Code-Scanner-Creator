package com.majkeylab.qrscannercreator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class QrImageDecoderTest {
    @Test
    fun renderedQrDecodesFromPixels() {
        val expected = "https://example.com/gallery"
        val image = QrRenderer.renderPixels(expected, QrStyle().parse().getOrThrow(), 512)

        assertEquals(expected, decodeQrPixels(image.size, image.size, image.pixels))
    }

    @Test
    fun imageWithoutQrReturnsNull() {
        assertNull(decodeQrPixels(64, 64, IntArray(64 * 64) { 0xFFFFFFFF.toInt() }))
    }

    @Test
    fun invalidPixelDimensionsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            decodeQrPixels(10, 10, IntArray(99))
        }
    }
}
