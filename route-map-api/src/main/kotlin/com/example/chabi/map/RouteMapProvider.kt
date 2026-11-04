package com.example.chabi.map

import android.content.Context
import android.view.View
import com.example.chabi.domain.GeoPoint
import kotlinx.coroutines.flow.StateFlow

/** Presentation-only contract. Never receives prices, participants, or a mutable settlement draft. */
interface RouteMapProvider {
    val info: MapProviderInfo
    /** Main thread. A session belongs to one visible surface, never to a ViewModel or singleton. */
    fun create(context: Context, onRouteTap: (String) -> Unit): RouteMapSession
}

data class MapProviderInfo(val name: String, val attribution: String, val links: List<MapCredit>)
data class MapCredit(val label: String, val url: String)

/** All SDK-specific View, geometry, camera, and error types stay inside the implementation. */
interface RouteMapSession : AutoCloseable {
    val view: View
    val loadState: StateFlow<MapLoadState>
    fun update(scene: RouteMapScene, appearance: MapAppearance)
    fun zoomIn()
    fun zoomOut()
    fun fitRoutes()
    fun retry()
    fun onStart()
    fun onResume()
    fun onPause()
    fun onStop()
    fun onLowMemory()
    /** Idempotent: unsubscribe/release native resources; ignore late SDK callbacks. */
    override fun close()
}

sealed interface MapLoadState {
    data object Loading : MapLoadState
    data object Ready : MapLoadState
    data object NoRoute : MapLoadState
    data object Failed : MapLoadState
}

/** ARGB values supplied by the Pleos theme; no dependency on Compose or a concrete map SDK. */
data class MapAppearance(val dark: Boolean, val route: Int, val halo: Int, val context: Int,
    val text: Int, val unselectedFocus: Int)

data class MapRoute(val id: String, val origin: String, val destination: String,
    val points: List<GeoPoint>, val selected: Boolean, val focused: Boolean)
data class RouteMapScene(val routes: List<MapRoute>, val fitPoints: List<GeoPoint>) {
    val visibleRoutes get() = routes.filter { it.selected || it.focused }
}
