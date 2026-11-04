package com.example.chabi.data

import com.example.chabi.domain.TripRepository
import com.example.chabi.domain.TripDataSource
import com.example.chabi.domain.Trip
import com.example.chabi.domain.GeoPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDateTime

class MockTripRepository(
    private val routeLoader: () -> Map<String, List<GeoPoint>> = { emptyMap() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TripRepository {
    private val routes by lazy { routeLoader() }
    override val source = TripDataSource.MOCK
    override suspend fun getTrips(): List<Trip> = withContext(ioDispatcher) {
        sampleTrips.map { it.copy(route = routes[it.id].orEmpty()) }
    }
    override suspend fun getTripDetail(id: String): Trip? = getTrips().find { it.id == id }

    companion object {
        val weekendIds = setOf("weekend-1", "weekend-2", "weekend-3", "weekend-4")
        private fun time(value: String) = LocalDateTime.parse(value)
        private val sampleTrips = listOf(
            Trip("weekend-1", time("2026-11-01T08:10"), time("2026-11-01T09:40"),
                "서울", "춘천", 98_000, averageKmPerUnit = BigDecimal("13.2")),
            Trip("weekend-2", time("2026-11-01T10:20"), time("2026-11-01T12:15"),
                "춘천", "속초", 105_000, averageKmPerUnit = BigDecimal("12.8")),
            Trip("weekend-3", time("2026-11-02T10:10"), time("2026-11-02T10:58"),
                "속초 숙소", "속초 시내", 24_000, averageKmPerUnit = BigDecimal("9.5")),
            Trip("weekend-4", time("2026-11-02T15:20"), time("2026-11-02T18:10"),
                "속초", "서울", 188_000, averageKmPerUnit = BigDecimal("13.4")),
            Trip("daily-1", time("2026-11-03T08:30"), time("2026-11-03T09:12"),
                "집", "회사", 18_200, averageKmPerUnit = BigDecimal("11.2"), consumedUnits = BigDecimal("1.625")),
            Trip("daily-2", time("2026-11-03T18:20"), time("2026-11-03T19:05"),
                "회사", "집", 19_300, averageKmPerUnit = BigDecimal("10.8")),
        )
    }
}
