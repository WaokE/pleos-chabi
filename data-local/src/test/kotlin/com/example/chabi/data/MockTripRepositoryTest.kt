package com.example.chabi.data

import com.example.chabi.domain.GeoPoint
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MockTripRepositoryTest {
    @Test fun `asset loader executes on injected IO dispatcher and is cached`() {
        Executors.newSingleThreadExecutor { Thread(it, "route-io") }.asCoroutineDispatcher().use { io -> runBlocking {
            var thread = ""; var reads = 0
            val route = listOf(GeoPoint(37.5, 127.0), GeoPoint(37.8, 127.5))
            val repository = MockTripRepository(routeLoader = { thread = Thread.currentThread().name; reads++; mapOf("weekend-1" to route) }, ioDispatcher = io)
            assertEquals(route, repository.getTripDetail("weekend-1")!!.route)
            repository.getTrips()
            assertTrue(thread.startsWith("route-io"))
            assertEquals(1, reads)
        } }
    }
    @Test fun `asset failure propagates without fabricated geometry and retry can recover`() = runBlocking {
        var fail = true
        val repository = MockTripRepository(routeLoader = { check(!fail); emptyMap() })
        assertTrue(runCatching { repository.getTrips() }.isFailure)
        fail = false
        assertTrue(repository.getTrips().all { it.route.isEmpty() })
    }
}
