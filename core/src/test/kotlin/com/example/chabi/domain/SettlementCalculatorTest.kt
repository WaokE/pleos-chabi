package com.example.chabi.domain

import com.example.chabi.format.*

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.random.Random

class SettlementCalculatorTest {
    private val start = LocalDateTime.of(2026, 11, 1, 8, 0)
    private fun trip(id: String = "t1", meters: Long = 415_000) =
        Trip(id, start, start.plusHours(2), "서울", "속초", meters)
    private fun draft(people: Int = 4) = SettlementDraft(
        selectedTripIds = setOf("t1"),
        participants = (1..people).map { Participant("p$it", "참가자 $it") },
    )

    @Test fun `415km example costs exactly 56440 won`() {
        val value = EnergyCalculator.estimate(listOf(trip()), EnergyCalculator.settings(draft()))
        assertEquals(56_440L, value.amountWon)
        assertEquals(0, value.consumptionProvidedCount)
    }

    @Test fun `electricity uses km per kWh and electricity price`() {
        val input = draft().copy(energyType = EnergyType.ELECTRIC, efficiencyInput = "5", unitPriceInput = "300")
        assertEquals(24_900L, EnergyCalculator.estimate(listOf(trip()), EnergyCalculator.settings(input)).amountWon)
    }

    @Test fun `provided consumption wins over distance estimate`() {
        val value = trip().copy(consumedUnits = BigDecimal("2.5"))
        val result = EnergyCalculator.estimate(listOf(value), EnergyCalculator.settings(draft()))
        assertEquals(4_250L, result.amountWon)
        assertEquals(1, result.consumptionProvidedCount)
    }

    @Test fun `wrong energy type cannot reuse measured liters as kWh`() {
        val value = trip().copy(consumedUnits = BigDecimal("2.5"))
        val input = draft().copy(energyType = EnergyType.ELECTRIC, efficiencyInput = "5", unitPriceInput = "300")
        val result = EnergyCalculator.estimate(listOf(value), EnergyCalculator.settings(input))
        assertEquals(24_900L, result.amountWon)
        assertEquals(0, result.consumptionProvidedCount)
    }

    @Test fun `trip efficiency is opt in and editable fallback still works`() {
        val input = draft().copy(useTripEfficiency = true)
        val value = trip().copy(averageKmPerUnit = BigDecimal("10"))
        assertEquals(70_550L, EnergyCalculator.estimate(listOf(value), EnergyCalculator.settings(input)).amountWon)
        assertEquals(56_440L, EnergyCalculator.estimate(listOf(trip()), EnergyCalculator.settings(input)).amountWon)
    }

    @Test fun `round once after adding trips`() {
        val trips = listOf(trip("a", 100), trip("b", 100))
        val settings = EnergySettings(EnergyType.FUEL, BigDecimal("3"), 10)
        assertEquals(1L, EnergyCalculator.estimate(trips, settings).amountWon)
        assertEquals(0L, EnergyCalculator.estimate(trips.take(1), settings).amountWon)
    }

    @Test fun `half won rounds up and zero unit price is valid`() {
        assertEquals(1L, EnergyCalculator.estimate(listOf(trip(meters = 500)),
            EnergySettings(EnergyType.FUEL, BigDecimal.ONE, 1)).amountWon)
        assertEquals(0L, EnergyCalculator.estimate(listOf(trip()),
            EnergySettings(EnergyType.FUEL, BigDecimal.ONE, 0)).amountWon)
    }

    @Test fun `every amount splits exactly across 3 4 and 5 people`() {
        for (count in 3..5) {
            val ids = (1..count).map { "p$it" }
            for (amount in 0L..1000L) {
                val split = SettlementCalculator.splitEvenly(amount, ids)
                assertEquals(amount, split.values.sum())
                assertTrue(split.values.max() - split.values.min() <= 1)
            }
        }
    }

    @Test fun `remainder uses registration order deterministically`() {
        assertEquals(linkedMapOf("a" to 3_334L, "b" to 3_334L, "c" to 3_333L),
            SettlementCalculator.splitEvenly(10_001, listOf("a", "b", "c")))
        assertEquals(MAX_MONEY_WON, SettlementCalculator.splitEvenly(MAX_MONEY_WON, listOf("a", "b", "c")).values.sum())
    }

    @Test fun `coffee is paid only by two named participants and totals match`() {
        val input = draft().copy(expenses = listOf(
            Expense("toll", ExpenseCategory.TOLL, "통행료", 24_000),
            Expense("parking", ExpenseCategory.PARKING, "주차비", 8_000),
            Expense("coffee", ExpenseCategory.OTHER, "커피", 10_001, participantIds = setOf("p2", "p1")),
        ))
        val result = SettlementCalculator.calculate(listOf(trip()), input)
        assertEquals(98_441L, result.totalWon)
        assertEquals(listOf(27_111L, 27_110L, 22_110L, 22_110L), result.shares.map { it.amountWon })
        assertEquals(setOf("p1", "p2"), result.costs.last().shares.keys)
        assertEquals(result.totalWon, result.shares.sumOf { it.amountWon })
    }

    @Test fun `many random partial expenses reconcile per line and per participant`() {
        val random = Random(20261104)
        for (count in listOf(3, 4, 5)) repeat(100) {
            val base = draft(count)
            val ids = base.participants.map { it.id }
            val expenses = (0..19).map { index ->
                Expense("e$index", ExpenseCategory.OTHER, "지출 $index", random.nextLong(1, 10_000_000),
                    participantIds = ids.shuffled(random).take(random.nextInt(1, count + 1)).toSet())
            }
            val result = SettlementCalculator.calculate(listOf(trip()), base.copy(expenses = expenses))
            result.costs.forEach { line ->
                assertEquals(line.amountWon, line.shares.values.sum())
                assertTrue(line.shares.keys.all { it in line.participantIds })
            }
            assertEquals(result.totalWon, result.shares.sumOf { it.amountWon })
        }
    }

    @Test fun `duplicate expense IDs and duplicate trips are rejected`() {
        val expense = Expense("same", ExpenseCategory.TOLL, "통행료", 1000)
        assertThrows(IllegalArgumentException::class.java) {
            SettlementCalculator.calculate(listOf(trip()), draft().copy(expenses = listOf(expense, expense)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            EnergyCalculator.estimate(listOf(trip(), trip()), EnergyCalculator.settings(draft()))
        }
    }

    @Test fun `empty subset cannot silently charge everybody`() {
        val expense = Expense("e", ExpenseCategory.OTHER, "커피", 1000, participantIds = emptySet())
        assertThrows(IllegalArgumentException::class.java) {
            SettlementCalculator.calculate(listOf(trip()), draft().copy(expenses = listOf(expense)))
        }
    }

    @Test fun `unknown participant and stale trip selection are rejected`() {
        val expense = Expense("e", ExpenseCategory.OTHER, "커피", 1000, participantIds = setOf("deleted"))
        assertThrows(IllegalArgumentException::class.java) {
            SettlementCalculator.calculate(listOf(trip()), draft().copy(expenses = listOf(expense)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SettlementCalculator.calculate(listOf(trip()), draft().copy(selectedTripIds = setOf("missing")))
        }
    }

    @Test fun `invalid efficiency prices names and total overflow cannot produce results`() {
        for (value in listOf("", "0", "-1", "NaN", "1001", "1.1234")) {
            assertThrows(IllegalArgumentException::class.java) { EnergyCalculator.settings(draft().copy(efficiencyInput = value)) }
        }
        for (value in listOf("", "-1", "1.2345", "100001", "100000.001", "NaN")) {
            assertThrows(IllegalArgumentException::class.java) { EnergyCalculator.settings(draft().copy(unitPriceInput = value)) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            SettlementCalculator.calculate(listOf(trip()), draft().copy(participants = listOf(Participant("a", ""))))
        }
        val huge = Expense("huge", ExpenseCategory.OTHER, "큰 금액", MAX_MONEY_WON)
        assertThrows(IllegalArgumentException::class.java) {
            SettlementCalculator.calculate(listOf(trip()), draft().copy(expenses = listOf(huge)))
        }
    }

    @Test fun `QR summary discloses mock data and exact amounts for partial expenses`() {
        val expense = Expense("coffee", ExpenseCategory.OTHER, "커피", 10_001, participantIds = setOf("p1", "p2"))
        val input = draft().copy(expenses = listOf(expense))
        val result = SettlementCalculator.calculate(listOf(trip()), input)
        val text = ResultSummary.create(result, input, isMock = true)
        assertTrue(text.contains("예시 운행으로 계산한 결과"))
        assertTrue(text.contains("추가 비용 10,001원"))
        assertTrue(text.contains("참가자 1 19,111원"))
        assertTrue(text.contains("참가자 3 14,110원"))
        assertTrue(text.contains("66,441원"))
        assertTrue(text.contains("등록 순서"))
    }

    @Test fun `remainder notice depends on each cost split even when the total divides evenly`() {
        val input = draft().copy(expenses = listOf(
            Expense("toll", ExpenseCategory.TOLL, "통행료", 1),
            Expense("parking", ExpenseCategory.PARKING, "주차비", 3),
        ))
        val result = SettlementCalculator.calculate(listOf(trip()), input)
        assertEquals(56_444L, result.totalWon)
        assertTrue(result.hasRemainderAllocation)
        assertTrue(ResultSummary.create(result, input, isMock = false).contains("남은 원"))
    }
    @Test fun `fractional unit price stays decimal until final won rounding`() {
        val input = draft().copy(unitPriceInput = "1700.25")
        val settings = EnergyCalculator.settings(input)
        assertEquals(BigDecimal("1700.25"), settings.priceWonPerUnit)
        val result = SettlementCalculator.calculate(listOf(trip()), input)
        assertEquals(56_448L, result.totalWon)
        assertEquals(result.totalWon, result.shares.sumOf { it.amountWon })
    }
}
