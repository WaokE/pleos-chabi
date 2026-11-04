package com.example.chabi

import android.app.Application
import com.example.chabi.handoff.QrEncoder
import com.example.chabi.zxing.ZxingQrEncoder
import com.example.chabi.data.MockDriveDefaultsRepository
import com.example.chabi.data.LocalDraftStore
import com.example.chabi.data.MockTripRepository
import com.example.chabi.data.MockRouteFixtures
import com.example.chabi.domain.*
import com.example.chabi.map.RouteMapProvider
import com.example.chabi.maplibre.MapLibreRouteMapProvider

class ChabiApplication : Application() {
    // Only this source binding changes when a supported vehicle API becomes available.
    val tripRepository: TripRepository by lazy { MockTripRepository(routeLoader = { MockRouteFixtures.read(assets.open("mock_routes.geojson")) }) }
    val qrEncoder: QrEncoder by lazy { ZxingQrEncoder() }
    val mapProvider: RouteMapProvider by lazy { MapLibreRouteMapProvider() }
    val defaultsRepository: DriveDefaultsRepository by lazy { MockDriveDefaultsRepository() }
    val draftStore: DraftStore by lazy { LocalDraftStore(this) }
}
