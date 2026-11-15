package com.example.chabi.zxing

import com.google.zxing.qrcode.decoder.Decoder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettlementQrTest {
    @Test
    fun `settlement QR preserves the complete HTTPS link`() = runTest {
        val url = "https://link.kakaopay.com/__/-example"
        val code = ZxingQrEncoder().encode(url)
        val quietZone = 4
        val width = code.size - quietZone * 2
        val modules = Array(width) { y ->
            BooleanArray(width) { x -> code[x + quietZone, y + quietZone] }
        }
        assertEquals(url, Decoder().decode(modules).text)
    }
}
