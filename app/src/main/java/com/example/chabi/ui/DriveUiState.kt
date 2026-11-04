package com.example.chabi.ui

import com.example.chabi.domain.*

enum class Step(val shortTitle: String) {
    HOME("홈"), TRIPS("운행 선택"), REVIEW("비용·인원"), RESULT("정산 결과"),
}
enum class TripFilter(val title: String) { ALL("전체 운행"), SELECTED("선택한 운행") }

data class DriveUiState(
    val source: TripDataSource,
    val draft: SettlementDraft = SettlementDraft(),
    val trips: List<Trip> = emptyList(), val selectedTrips: List<Trip> = emptyList(),
    val step: Step = Step.HOME, val filter: TripFilter = TripFilter.ALL,
    val defaults: DriveDefaults? = null, val focusedTripId: String? = null,
    val loading: Boolean = true, val loadError: Boolean = false, val notice: String? = null,
    val energy: EnergyEstimate? = null, val energyError: String? = null,
    val settlement: Settlement? = null, val settlementError: String? = null,
) {
    val distanceMeters get() = selectedTrips.sumOf { it.distanceMeters }
    val durationMinutes get() = selectedTrips.sumOf { it.durationMinutes }
    val additionalWon get() = draft.expenses.sumOf { it.amountWon }
    val hasDraft get() = draft.selectedTripIds.isNotEmpty() || draft.expenses.isNotEmpty()
    val previewTrip get() = trips.find { it.id == focusedTripId } ?: selectedTrips.firstOrNull() ?: trips.firstOrNull()
    val canContinue get() = !loading && !loadError && when (step) {
        Step.HOME -> defaults != null
        Step.TRIPS -> selectedTrips.isNotEmpty() && selectedTrips.size == draft.selectedTripIds.size
        Step.REVIEW -> settlement != null
        Step.RESULT -> false
    }
}
