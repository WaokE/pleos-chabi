package com.example.chabi.zxing

import com.example.chabi.handoff.QrCode
import com.example.chabi.handoff.QrEncoder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ZxingQrEncoder(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) : QrEncoder {
    override suspend fun encode(text: String): QrCode = withContext(dispatcher) {
        val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 4,
        ))
        QrCode(bits.width) { x, y -> bits[x, y] }
    }
}
