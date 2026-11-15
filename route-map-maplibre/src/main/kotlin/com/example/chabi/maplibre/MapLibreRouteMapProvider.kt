package com.example.chabi.maplibre

import android.content.Context
import com.example.chabi.map.*

class MapLibreRouteMapProvider : RouteMapProvider {
    override val info = MapProviderInfo(
        attribution = "© OpenStreetMap 기여자 (ODbL) · © OpenMapTiles · OpenFreeMap",
        licenseUrl = "https://www.openstreetmap.org/copyright",
    )
    override fun create(context: Context, onRouteTap: (String) -> Unit): RouteMapSession =
        MapLibreRouteMapSession(context, onRouteTap)
}
