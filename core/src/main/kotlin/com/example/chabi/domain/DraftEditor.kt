package com.example.chabi.domain

import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/** Price source recorded when the user types a unit price instead of using a preset. */
const val DIRECT_PRICE_SOURCE: String = "직접 입력"

/** Stateless draft business rules. Clock/IDs are explicit seams for deterministic tests. */
class DraftEditor(private val clock: Clock = Clock.systemDefaultZone(), private val nextId: () -> String = { UUID.randomUUID().toString() }) {
    fun withPreset(draft: SettlementDraft, preset: EnergyPreset) = draft.copy(energyType = preset.type,
        energyProfileId = preset.id, efficiencyInput = preset.efficiency, unitPriceInput = preset.price,
        priceSource = preset.priceSource, priceAsOf = preset.asOf, priceIsMock = preset.isMock)

    /**
     * Draft that would result from the cost-basis form, without validating the numbers.
     * The form uses it for its preview and validation; [applyCostSettings] uses it to commit.
     */
    fun previewCostSettings(old: SettlementDraft, preset: EnergyPreset, efficiency: String, price: String,
        useTrip: Boolean, referencePrice: Boolean,
    ): SettlementDraft {
        val unchanged = price == old.unitPriceInput && preset.id == old.energyProfileId
        val base = if (referencePrice) withPreset(old, preset) else old.copy(energyType = preset.type,
            energyProfileId = preset.id, priceSource = if (unchanged) old.priceSource else DIRECT_PRICE_SOURCE,
            priceAsOf = if (unchanged) old.priceAsOf else LocalDate.now(clock).toString(),
            priceIsMock = if (unchanged) old.priceIsMock else false)
        return base.copy(efficiencyInput = efficiency, unitPriceInput = price, useTripEfficiency = useTrip)
    }

    /** Applies the form only when its numbers are valid; otherwise keeps [old] unchanged. */
    fun applyCostSettings(old: SettlementDraft, preset: EnergyPreset, efficiency: String, price: String,
        useTrip: Boolean, referencePrice: Boolean,
    ): SettlementDraft {
        val next = previewCostSettings(old, preset, efficiency, price, useTrip, referencePrice)
        return if (runCatching { EnergyCalculator.settings(next) }.isSuccess) next else old
    }

    fun setParticipantCount(draft: SettlementDraft, count: Int): SettlementDraft {
        val target = count.coerceIn(MIN_PARTICIPANTS, MAX_PARTICIPANTS)
        var people = draft.participants.take(target)
        while (people.size < target) {
            val name = generateSequence(1) { it + 1 }.map { "동행 $it" }.first { name -> people.none { it.name == name } }
            people = people + Participant(nextId(), name)
        }
        val valid = people.map { it.id }.toSet()
        return draft.copy(participants = people, expenses = draft.expenses.map { cost ->
            cost.copy(participantIds = cost.participantIds?.intersect(valid))
        })
    }
}
