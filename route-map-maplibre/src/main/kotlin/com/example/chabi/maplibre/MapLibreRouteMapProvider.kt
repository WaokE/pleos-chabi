package com.example.chabi.maplibre

import android.content.Context
import com.example.chabi.map.*

class MapLibreRouteMapProvider : RouteMapProvider {
    override val info = MapProviderInfo(
        name = "OpenFreeMap · MapLibre Native",
        attribution = "© OpenStreetMap 기여자 · OpenMapTiles · OpenFreeMap",
        links = listOf(
            MapCredit("© OpenStreetMap 기여자 · ODbL", "https://www.openstreetmap.org/copyright"),
            MapCredit("© OpenMapTiles", "https://openmaptiles.org/"),
            MapCredit("OpenFreeMap", "https://openfreemap.org/"),
        ),
    )
    override fun create(context: Context, onRouteTap: (String) -> Unit): RouteMapSession =
        MapLibreRouteMapSession(context, onRouteTap)
}
