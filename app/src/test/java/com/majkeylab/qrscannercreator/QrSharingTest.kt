package com.majkeylab.qrscannercreator

import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class QrSharingTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun laterShareDoesNotOverwritePreviouslyGrantedFile() {
        val directory = temporary.newFolder()
        val first = writeSharedQr(directory) { it.write("first QR".toByteArray()) }
        val second = writeSharedQr(directory) { it.write("second QR".toByteArray()) }

        assertNotEquals(first, second)
        assertEquals("first QR", first.readText())
        assertEquals("second QR", second.readText())
    }

    @Test
    fun sharesStayBoundedAndExpiredExportsAreRemoved() {
        val directory = temporary.newFolder()
        val unrelated = directory.resolve("keep.txt").apply { writeText("unrelated") }
        val expired = directory.resolve("qr-code.png").apply { writeText("expired") }
        assertTrue(expired.setLastModified(System.currentTimeMillis() - 25L * 60 * 60 * 1_000))

        repeat(12) { number ->
            writeSharedQr(directory) { it.write(number.toString().toByteArray()) }
        }

        assertEquals(8, requireNotNull(directory.listFiles()).count { it.extension == "png" })
        assertTrue(!expired.exists())
        assertEquals("unrelated", unrelated.readText())
    }

    @Test
    fun failedWriteRemovesPartialFileAndPreservesPreviousShare() {
        val directory = temporary.newFolder()
        val previous = writeSharedQr(directory) { it.write("complete".toByteArray()) }

        assertThrows(IOException::class.java) {
            writeSharedQr(directory) {
                it.write("partial".toByteArray())
                throw IOException("Fixture write failure")
            }
        }

        assertEquals(listOf(previous), requireNotNull(directory.listFiles()).toList())
        assertEquals("complete", previous.readText())
    }

    @Test
    fun cancellationRemovesPartialFileAndPropagatesUnchanged() {
        val directory = temporary.newFolder()
        val cancellation = CancellationException("Fixture cancellation")

        val thrown = assertThrows(CancellationException::class.java) {
            writeSharedQr(directory) {
                it.write("partial".toByteArray())
                throw cancellation
            }
        }

        assertSame(cancellation, thrown)
        assertTrue(requireNotNull(directory.listFiles()).isEmpty())
    }
}
