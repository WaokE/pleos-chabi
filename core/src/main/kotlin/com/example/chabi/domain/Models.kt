package com.example.chabi.domain

import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime

/**
 * [title] names the cost ("연료비"), [efficiencyName] the efficiency figure ("연비"/"전비").
 * Both end in a vowel, so user text attaches 는/가/로 to them.
 */
enum class EnergyType(val title: String, val quantityUnit: String, val efficiencyUnit: String, val efficiencyName: String) {
    FUEL("연료비", "L", "km/L", "연비"), ELECTRIC("충전비", "kWh", "km/kWh", "전비"),
}

data class GeoPoint(val latitude: Double, val longitude: Double)

data class Trip(
    val id: String,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime,
    val origin: String,
    val destination: String,
    val distanceMeters: Long,
    val energyType: EnergyType = EnergyType.FUEL,
    val averageKmPerUnit: BigDecimal? = null,
    val consumedUnits: BigDecimal? = null,
    val route: List<GeoPoint> = emptyList(),
) {
    val durationMinutes: Long get() = Duration.between(startedAt, endedAt).toMinutes()
}

data class EnergySettings(
    val type: EnergyType,
    val kmPerUnit: BigDecimal,
    val priceWonPerUnit: BigDecimal,
    val useTripEfficiency: Boolean = false,
) {
    constructor(type: EnergyType, kmPerUnit: BigDecimal, priceWonPerUnit: Long, useTripEfficiency: Boolean = false) :
        this(type, kmPerUnit, BigDecimal.valueOf(priceWonPerUnit), useTripEfficiency)
}

data class EnergyEstimate(
    val amountWon: Long,
    val consumedUnits: BigDecimal,
    val consumptionProvidedCount: Int,
    val estimatedCount: Int,
)

data class Participant(val id: String, val name: String)

enum class ExpenseCategory(val title: String) {
    TOLL("통행료"), PARKING("주차비"), OTHER("기타 지출"), CUSTOM("직접 입력"),
}

data class Expense(
    val id: String,
    val category: ExpenseCategory,
    val title: String,
    val amountWon: Long,
    val memo: String = "",
    // null means all current participants; an empty set is invalid, never "everyone".
    val participantIds: Set<String>? = null,
)

data class CostLine(
    val id: String,
    val title: String,
    val amountWon: Long,
    val participantIds: List<String>,
    val shares: Map<String, Long>,
)

data class ParticipantShare(val participant: Participant, val amountWon: Long)

data class Settlement(
    val title: String,
    val tripCount: Int,
    val distanceMeters: Long,
    val durationMinutes: Long,
    val energyType: EnergyType,
    val energyEstimate: EnergyEstimate,
    val costs: List<CostLine>,
    val shares: List<ParticipantShare>,
    val totalWon: Long,
) {
    val hasRemainderAllocation get() = costs.any { it.amountWon % it.participantIds.size != 0L }
}

enum class DraftStage { TRIPS, REVIEW, RESULT }

/** Editable input stays textual until validation; blank/invalid input must not become zero. */
data class SettlementDraft(
    // Shared text starts with "[차비] <title>", so the default must not repeat the app name.
    val title: String = "정산 내역",
    val selectedTripIds: Set<String> = emptySet(),
    val energyType: EnergyType = EnergyType.FUEL,
    val efficiencyInput: String = "12.5",
    val unitPriceInput: String = "1700",
    val useTripEfficiency: Boolean = false,
    val energyProfileId: String = "gasoline",
    val priceSource: String = "저장된 기준 단가",
    val priceAsOf: String = "",
    val priceIsMock: Boolean = true,
    val expenses: List<Expense> = emptyList(),
    val participants: List<Participant> = listOf(
        Participant("me", "나"), Participant("guest-1", "동행 1"),
        Participant("guest-2", "동행 2"), Participant("guest-3", "동행 3"),
    ),
    val resumeStage: DraftStage? = null,
)
