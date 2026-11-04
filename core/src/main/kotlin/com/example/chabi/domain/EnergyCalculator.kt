package com.example.chabi.domain

import java.math.BigDecimal
import java.math.RoundingMode

object EnergyCalculator {
    fun settings(draft: SettlementDraft): EnergySettings {
        val name = draft.energyType.efficiencyName
        val efficiency = draft.efficiencyInput.toBigDecimalOrNull()
        require(efficiency != null && efficiency > BigDecimal.ZERO && efficiency <= BigDecimal("1000")) {
            "${name}는 0보다 크고 1,000 이하로 입력해 주세요."
        }
        require(efficiency.scale() <= 3) { "${name}는 소수점 아래 셋째 자리까지 입력할 수 있어요." }
        val price = draft.unitPriceInput.toBigDecimalOrNull()
        require(price != null && price >= BigDecimal.ZERO && price <= BigDecimal("100000")) { "단가는 0원에서 100,000원 사이로 입력해 주세요." }
        require(price.scale() <= 3) { "단가는 소수점 아래 셋째 자리까지 입력할 수 있어요." }
        return EnergySettings(draft.energyType, efficiency, price, draft.useTripEfficiency)
    }

    fun estimate(trips: List<Trip>, settings: EnergySettings): EnergyEstimate {
        require(settings.kmPerUnit > BigDecimal.ZERO) { "${settings.type.efficiencyName}는 0보다 커야 해요." }
        require(settings.priceWonPerUnit >= BigDecimal.ZERO && settings.priceWonPerUnit <= BigDecimal("100000")) { "단가가 올바르지 않아요." }
        require(trips.map { it.id }.distinct().size == trips.size) { "같은 운행이 두 번 들어 있어요." }
        var consumption = BigDecimal.ZERO
        var providedCount = 0
        trips.forEach { trip ->
            require(trip.distanceMeters >= 0 && !trip.endedAt.isBefore(trip.startedAt)) { "운행 기록의 거리나 시간이 올바르지 않아요." }
            val measured = trip.consumedUnits?.takeIf { trip.energyType == settings.type }
            val quantity = if (measured != null) {
                require(measured >= BigDecimal.ZERO) { "운행 기록의 소비량이 올바르지 않아요." }
                providedCount++
                measured
            } else {
                val efficiency = trip.averageKmPerUnit
                    ?.takeIf { settings.useTripEfficiency && trip.energyType == settings.type }
                    ?: settings.kmPerUnit
                require(efficiency > BigDecimal.ZERO) { "운행 기록의 ${settings.type.efficiencyName}가 올바르지 않아요." }
                BigDecimal.valueOf(trip.distanceMeters).movePointLeft(3)
                    .divide(efficiency, 16, RoundingMode.HALF_UP)
            }
            consumption = consumption.add(quantity)
        }
        // Aggregate consumption first. Round once, to whole won; never truncate each trip.
        val cost = consumption.multiply(settings.priceWonPerUnit)
            .setScale(0, RoundingMode.HALF_UP)
        require(cost <= BigDecimal.valueOf(MAX_MONEY_WON)) { "계산할 수 있는 최대 금액을 넘었어요." }
        return EnergyEstimate(cost.longValueExact(), consumption, providedCount, trips.size - providedCount)
    }
}
