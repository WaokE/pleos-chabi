package com.example.chabi.domain

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class DraftEditorTest {
    private val preset = EnergyPreset("gasoline", "휘발유", EnergyType.FUEL, "12.5", "1700", "예시 기준", "2026-11-04", true)
    private val editor = DraftEditor(Clock.fixed(Instant.parse("2026-11-05T00:00:00Z"), ZoneOffset.UTC))

    @Test fun `removing last participant of a partial expense preserves invalid empty subset`() {
        val draft = SettlementDraft(expenses = listOf(
            Expense("partial", ExpenseCategory.PARKING, "주차비", 1001, participantIds = setOf("guest-3")),
            Expense("all", ExpenseCategory.TOLL, "통행료", 1001),
        ))
        val next = editor.setParticipantCount(draft, 3)
        assertEquals(emptySet<String>(), next.expenses.first().participantIds)
        assertNull(next.expenses.last().participantIds)
        assertEquals(4, draft.participants.size)
    }
    @Test fun `growing participants preserves existing identities and selected expense members`() {
        var id = 0
        val editor = DraftEditor(nextId = { "new-${++id}" })
        val draft = SettlementDraft(expenses = listOf(Expense("p", ExpenseCategory.PARKING, "주차비", 1001, participantIds = setOf("me"))))
        val next = editor.setParticipantCount(draft, 6)
        assertEquals(draft.participants, next.participants.take(4))
        assertEquals(6, next.participants.map { it.id }.distinct().size)
        assertEquals(setOf("me"), next.expenses.single().participantIds)
    }
    @Test fun `manual quote records injected date and preserves provenance on unchanged values`() {
        val next = editor.applyCostSettings(SettlementDraft(), preset, "12.5", "1700.25", false, false)
        assertEquals("2026-11-05", next.priceAsOf)
        assertEquals("직접 입력", next.priceSource)
        assertFalse(next.priceIsMock)
        val unchanged = editor.applyCostSettings(next, preset, "13", "1700.25", false, false)
        assertEquals(next.priceAsOf, unchanged.priceAsOf)
        assertEquals(next.priceSource, unchanged.priceSource)
    }
    @Test fun `invalid edit is rejected atomically and reference restores provenance`() {
        val old = editor.applyCostSettings(SettlementDraft(), preset, "12.5", "1800", false, false)
        assertEquals(old, editor.applyCostSettings(old, preset, "0", "1700", false, true))
        val restored = editor.applyCostSettings(old, preset, "12.5", preset.price, false, true)
        assertEquals(preset.priceSource, restored.priceSource)
        assertEquals(preset.asOf, restored.priceAsOf)
        assertTrue(restored.priceIsMock)
    }
    @Test fun `preview keeps invalid input visible while apply rejects it`() {
        val old = SettlementDraft()
        val preview = editor.previewCostSettings(old, preset, "0", "1900", false, false)
        assertEquals("0", preview.efficiencyInput)
        assertEquals(DIRECT_PRICE_SOURCE, preview.priceSource)
        assertEquals(old, editor.applyCostSettings(old, preset, "0", "1900", false, false))
    }
    @Test fun `validation message names the efficiency the user is editing`() {
        val fuel = runCatching { EnergyCalculator.settings(SettlementDraft(efficiencyInput = "0")) }.exceptionOrNull()?.message
        val electric = runCatching { EnergyCalculator.settings(SettlementDraft(energyType = EnergyType.ELECTRIC, efficiencyInput = "0")) }
            .exceptionOrNull()?.message
        assertTrue(fuel!!.startsWith("연비는"))
        assertTrue(electric!!.startsWith("전비는"))
    }
    @Test fun `participant count is clamped to the shared limits`() {
        assertEquals(MAX_PARTICIPANTS, editor.setParticipantCount(SettlementDraft(), 99).participants.size)
        assertEquals(MIN_PARTICIPANTS, editor.setParticipantCount(SettlementDraft(), 0).participants.size)
    }
    @Test fun `participant names differing only by case or spaces are flagged like the calculator does`() {
        val people = listOf(Participant("a", "Kim"), Participant("b", " kim "), Participant("c", "Lee"))
        assertTrue(isInvalidParticipantName("Kim", people))
        assertFalse(isInvalidParticipantName("Lee", people))
        assertTrue(isInvalidParticipantName("  ", people))
    }
}
