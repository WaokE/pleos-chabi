package com.example.chabi.ui

import com.example.chabi.domain.GeoPoint
import com.example.chabi.domain.Trip
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class TripMapSceneTest {
    private val seoul = GeoPoint(37.566, 126.978)
    private val chuncheon = GeoPoint(37.88, 127.73)
    private val sokcho = GeoPoint(38.21, 128.59)
    private fun trip(id: String, points: List<GeoPoint>): Trip {
        val time = LocalDateTime.of(2026, 11, 1, 8, 0)
        return Trip(id, time, time.plusHours(1), "출발", "도착", 98_000, route = points)
    }
    private val a = trip("a", listOf(seoul, chuncheon))
    private val b = trip("b", listOf(chuncheon, sokcho))

    @Test fun `focusing one selected trip keeps every selected route inside camera bounds`() {
        val one = tripMapScene(listOf(a, b), setOf("a", "b"), "a")
        val two = tripMapScene(listOf(a, b), setOf("a", "b"), "b")
        assertTrue(one.fitPoints.containsAll(listOf(seoul, chuncheon, sokcho)))
        assertEquals(one.fitPoints, two.fitPoints)
        assertEquals(setOf("a", "b"), two.visibleRoutes.map { it.id }.toSet())
        assertTrue(two.routes.first { it.id == "b" }.focused)
    }
    @Test fun `unselected focus does not add a trip to settlement or move camera off selection`() {
        val scene = tripMapScene(listOf(a, b), setOf("a"), "b")
        assertEquals(a.route, scene.fitPoints)
        assertFalse(scene.routes.first { it.id == "b" }.selected)
        assertTrue(scene.routes.first { it.id == "b" }.focused)
    }
    @Test fun `empty selection uses focused route then available routes`() {
        assertEquals(b.route, tripMapScene(listOf(a, b), emptySet(), "b").fitPoints)
        assertEquals(a.route + b.route, tripMapScene(listOf(a, b), emptySet(), null).fitPoints)
    }
    @Test fun `missing or corrupt coordinates never create a connection across a gap`() {
        val missing = trip("missing", emptyList())
        val corrupt = trip("corrupt", listOf(seoul, GeoPoint(Double.NaN, 127.0), sokcho))
        val outOfRange = trip("outside", listOf(seoul, GeoPoint(95.0, 128.0)))
        val scene = tripMapScene(listOf(missing, corrupt, outOfRange), setOf("missing", "corrupt"), "missing")
        assertTrue(scene.routes.isEmpty())
        assertTrue(scene.fitPoints.isEmpty())
    }
}
