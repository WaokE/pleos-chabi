package com.example.chabi.ui

import com.example.chabi.map.MapRoute
import com.example.chabi.map.RouteMapScene
import com.example.chabi.domain.Trip

/** Missing/invalid GPS never becomes an invented connection between place names. */
fun tripMapScene(trips: List<Trip>, selected: Set<String>, focusedId: String?): RouteMapScene {
    val routes = trips.mapNotNull { trip ->
        val valid = trip.route.filter { it.latitude.isFinite() && it.longitude.isFinite() && it.latitude in -85.0..85.0 && it.longitude in -180.0..180.0 }
        if (valid.size < 2 || valid.size != trip.route.size) null
        else MapRoute(trip.id, trip.origin, trip.destination, valid, trip.id in selected, trip.id == focusedId)
    }
    val selectedRoutes = routes.filter { it.selected }
    val fitting = selectedRoutes.ifEmpty { routes.filter { it.focused } }.ifEmpty { routes }
    return RouteMapScene(routes, fitting.flatMap { it.points })
}
