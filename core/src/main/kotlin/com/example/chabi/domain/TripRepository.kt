package com.example.chabi.domain


enum class TripDataSource(val label: String) { MOCK("예시 운행 · MOCK"), VEHICLE("차량 운행 기록") }

/** Only this data contract is needed by the UI/domain; no vehicle SDK types cross it. */
interface TripRepository {
    val source: TripDataSource
    suspend fun getTrips(): List<Trip>
    suspend fun getTripDetail(id: String): Trip?
}
