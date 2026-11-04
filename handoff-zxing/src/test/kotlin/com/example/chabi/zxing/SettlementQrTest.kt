package com.example.chabi.zxing

import com.example.chabi.format.*

import com.example.chabi.domain.*
import com.google.zxing.qrcode.decoder.Decoder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SettlementQrTest {
    private fun trip(meters: Long): Trip {
        val time = java.time.LocalDateTime.of(2026, 11, 1, 8, 0)
        return Trip("trip", time, time.plusHours(1), "서울", "도착", meters)
    }
    private suspend fun decode(text: String): String {
        val bits = ZxingQrEncoder().encode(text)
        val dark = (0 until bits.size).flatMap { y -> (0 until bits.size).filter { x -> bits[x, y] }.map { x -> x to y } }
        val left = dark.minOf { it.first }; val top = dark.minOf { it.second }
        val width = dark.maxOf { it.first } - left + 1; val height = dark.maxOf { it.second } - top + 1
        val modules = Array(height) { y -> BooleanArray(width) { x -> bits[x + left, y + top] } }
        return Decoder().decode(modules).text
    }
    @Test fun `Korean phone handoff round trips exact amounts and price provenance`() = runTest {
        val trips = listOf(trip(415_000))
        val draft = SettlementDraft(selectedTripIds = trips.map { it.id }.toSet(), priceSource = "시연용 기준 단가", priceAsOf = "2026-11-04")
        val result = SettlementCalculator.calculate(trips, draft)
        val text = ResultSummary.create(result, draft, true)
        assertEquals(text, decode(text))
        assertTrue(text.contains("56,440원"))
        assertTrue(text.contains("14,110원"))
        assertTrue(text.contains("실제 평균가가 아니에요"))
        assertTrue(text.contains("2026-11-04"))
        assertFalse(text.contains("http"))
    }
    @Test fun `maximum participant and money summary remains encodable with exact allocations`() = runTest {
        val trips = listOf(trip(98_000))
        val draft = SettlementDraft(selectedTripIds = trips.map { it.id }.toSet(), unitPriceInput = "100000", efficiencyInput = "1",
            expenses = listOf(Expense("toll", ExpenseCategory.TOLL, "통행료", 999_999_999)),
            participants = (1..20).map { Participant("p$it", "가나다라마바사아자차카타파하참가자${it}") })
        val result = SettlementCalculator.calculate(trips, draft)
        val text = ResultSummary.create(result, draft, true)
        assertEquals(text, decode(text))
        assertTrue(text.contains(won(result.totalWon)))
        assertTrue(result.shares.all { text.contains(won(it.amountWon)) })
    }
}
