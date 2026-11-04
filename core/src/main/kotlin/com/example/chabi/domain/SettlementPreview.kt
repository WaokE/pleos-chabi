package com.example.chabi.domain

/** Calculation projection shared by presentation layers; map geometry never enters arithmetic. */
data class SettlementPreview(val selectedTrips: List<Trip>, val energy: EnergyEstimate?,
    val settlement: Settlement?, val energyError: String?, val settlementError: String?,
) {
    companion object {
        fun evaluate(trips: List<Trip>, draft: SettlementDraft): SettlementPreview {
            val selected = trips.filter { it.id in draft.selectedTripIds }
            val energy = runCatching { EnergyCalculator.estimate(selected, EnergyCalculator.settings(draft)) }
            val settlement = runCatching { SettlementCalculator.calculate(selected, draft) }
            return SettlementPreview(selected, energy.getOrNull(), settlement.getOrNull(),
                energy.exceptionOrNull()?.message, settlement.exceptionOrNull()?.message)
        }
    }
}
