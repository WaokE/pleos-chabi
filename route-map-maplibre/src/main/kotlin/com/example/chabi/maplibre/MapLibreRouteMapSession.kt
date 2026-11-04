package com.example.chabi.maplibre

import android.content.Context
import android.graphics.RectF
import android.os.Bundle
import com.example.chabi.map.*
import com.example.chabi.domain.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.*
import org.maplibre.android.style.expressions.Expression.*
import org.maplibre.android.style.layers.*
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.*

internal class MapLibreRouteMapSession(context: Context, private val onRouteTap: (String) -> Unit) : RouteMapSession {
    override val view: MapView
    private var map: MapLibreMap? = null
    private val mutableLoadState = MutableStateFlow<MapLoadState>(MapLoadState.Loading)
    override val loadState = mutableLoadState.asStateFlow()
    private var failed = false
    private var currentScene: RouteMapScene? = null
    private var palette: MapAppearance? = null
    private var styleKey: Boolean? = null
    private var styleGeneration = 0
    private var ready = false
    private var destroyed = false
    private var started = false
    private var resumed = false
    private var fitPoints: List<GeoPoint>? = null
    private val density = context.resources.displayMetrics.density

    init {
        MapRuntime.initialize(context)
        view = MapView(context, MapLibreMapOptions.createFromAttributes(context)
            .textureMode(true).translucentTextureSurface(false)
            .logoEnabled(false).attributionEnabled(false).compassEnabled(false)
            .localIdeographFontFamily("sans-serif"))
        view.contentDescription = "운행 선택과 연결된 지도. 지도 위 경로를 눌러 운행을 확인할 수 있습니다."
        view.onCreate(Bundle())
        view.addOnDidFailLoadingMapListener {
            if (!destroyed) { failed = true; mutableLoadState.value = MapLoadState.Failed }
        }
        view.addOnDidFinishRenderingMapListener { fully ->
            if (fully && ready && !destroyed && !failed && currentScene?.fitPoints?.isNotEmpty() == true) mutableLoadState.value = MapLoadState.Ready
        }
        view.getMapAsync { loaded ->
            if (!destroyed) {
                map = loaded
                loaded.uiSettings.isRotateGesturesEnabled = false
                loaded.uiSettings.isTiltGesturesEnabled = false
                loaded.setPrefetchZoomDelta(0)
                loaded.addOnMapClickListener { point ->
                    if (!ready || destroyed) return@addOnMapClickListener false
                    val screen = loaded.projection.toScreenLocation(point)
                    val radius = 28 * density
                    val hits = loaded.queryRenderedFeatures(RectF(screen.x - radius, screen.y - radius, screen.x + radius, screen.y + radius),
                        "route-focus", "route-focus-unselected", "route-selected", "route-context")
                    hits.firstOrNull()?.takeIf { it.hasProperty("tripId") }?.getStringProperty("tripId")?.let(onRouteTap)
                    hits.isNotEmpty()
                }
                reloadStyle()
            }
        }
        view.addOnLayoutChangeListener { _, l, t, r, b, oldL, oldT, oldR, oldB ->
            if (r - l != oldR - oldL || b - t != oldB - oldT) { fitPoints = null; fitRoutes() }
        }
    }
    override fun update(scene: RouteMapScene, appearance: MapAppearance) {
        val colors = appearance
        val changed = currentScene != scene
        val appearanceChanged = palette != colors
        currentScene = scene; palette = colors
        if (map == null || destroyed) return
        if (styleKey != colors.dark || appearanceChanged) { reloadStyle(); return }
        if (ready && changed) { render(); fitIfChanged() }
    }
    private fun reloadStyle() {
        val sdk = map ?: return
        val colors = palette ?: return
        ready = false; failed = false; mutableLoadState.value = MapLoadState.Loading; styleKey = colors.dark
        val url = if (colors.dark) "https://tiles.openfreemap.org/styles/dark" else "https://tiles.openfreemap.org/styles/liberty"
        val generation = ++styleGeneration
        sdk.setStyle(Style.Builder().fromUri(url)) { style ->
            if (destroyed || generation != styleGeneration) return@setStyle
            val color = colors.route; val halo = colors.halo
            style.addSource(GeoJsonSource("trip-lines", FeatureCollection.fromFeatures(emptyArray<Feature>())))
            style.addSource(GeoJsonSource("trip-points", FeatureCollection.fromFeatures(emptyArray<Feature>())))
            style.addLayer(LineLayer("route-context", "trip-lines").withProperties(lineColor(colors.context), lineWidth(3f), lineOpacity(.45f), lineCap(Property.LINE_CAP_ROUND)))
            style.addLayer(LineLayer("route-halo", "trip-lines").withFilter(eq(get("selected"), literal(true)))
                .withProperties(lineColor(halo), lineWidth(12f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)))
            style.addLayer(LineLayer("route-selected", "trip-lines").withFilter(eq(get("selected"), literal(true)))
                .withProperties(lineColor(color), lineWidth(6f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)))
            style.addLayer(LineLayer("route-focus", "trip-lines").withFilter(all(eq(get("focused"), literal(true)), eq(get("selected"), literal(true))))
                .withProperties(lineColor(color), lineWidth(9f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)))
            style.addLayer(LineLayer("route-focus-unselected", "trip-lines").withFilter(all(eq(get("focused"), literal(true)), eq(get("selected"), literal(false))))
                .withProperties(lineColor(colors.unselectedFocus), lineWidth(6f), lineDasharray(arrayOf(2f, 1.5f)), lineCap(Property.LINE_CAP_ROUND)))
            style.addLayer(CircleLayer("route-endpoints", "trip-points").withProperties(circleRadius(8f), circleColor(color), circleStrokeWidth(3f), circleStrokeColor(halo)))
            style.addLayer(SymbolLayer("route-names", "trip-points").withProperties(textField(get("label")), textSize(22f),
                textColor(colors.text), textHaloColor(halo), textHaloWidth(2f), textFont(arrayOf("Noto Sans Regular")),
                textAnchor(Property.TEXT_ANCHOR_TOP), textOffset(arrayOf(0f, 1.2f))))
            ready = true; mutableLoadState.value = MapLoadState.Loading; failed = false
            render(); view.post { fitIfChanged() }
        }
    }
    override fun retry() { styleKey = null; reloadStyle() }
    private fun render() {
        if (!ready || destroyed) return
        val scene = currentScene ?: return
        val lines = scene.routes.map { route ->
            Feature.fromGeometry(LineString.fromLngLats(route.points.map { Point.fromLngLat(it.longitude, it.latitude) })).apply {
                addStringProperty("tripId", route.id); addBooleanProperty("selected", route.selected); addBooleanProperty("focused", route.focused)
            }
        }
        val markers = scene.visibleRoutes.flatMap { route ->
            listOf(route.origin to route.points.first(), route.destination to route.points.last())
        }.distinctBy { it.first to it.second }.map { (name, point) ->
            Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)).apply { addStringProperty("label", name) }
        }
        map?.style?.getSourceAs<GeoJsonSource>("trip-lines")?.setGeoJson(FeatureCollection.fromFeatures(lines))
        map?.style?.getSourceAs<GeoJsonSource>("trip-points")?.setGeoJson(FeatureCollection.fromFeatures(markers))
        if (scene.fitPoints.isEmpty()) mutableLoadState.value = MapLoadState.NoRoute
        else if (mutableLoadState.value == MapLoadState.NoRoute) mutableLoadState.value = MapLoadState.Ready
    }
    private fun fitIfChanged() {
        val points = currentScene?.fitPoints ?: return
        if (fitPoints != points) fitRoutes()
    }
    override fun fitRoutes() {
        if (!ready || destroyed || view.width == 0 || view.height == 0) return
        val points = currentScene?.fitPoints.orEmpty()
        if (points.isEmpty()) return
        val coordinates = points.distinct().map { LatLng(it.latitude, it.longitude) }
        val camera = if (coordinates.size == 1) CameraUpdateFactory.newLatLngZoom(coordinates.first(), 13.0)
        else CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(coordinates).build(),
            (52 * density).toInt(), (56 * density).toInt(), (92 * density).toInt(), (72 * density).toInt())
        map?.moveCamera(camera)
        fitPoints = currentScene?.fitPoints
    }
    override fun zoomIn() { if (ready && !destroyed) map?.animateCamera(CameraUpdateFactory.zoomIn(), 180) }
    override fun zoomOut() { if (ready && !destroyed) map?.animateCamera(CameraUpdateFactory.zoomOut(), 180) }
    override fun onStart() { if (!destroyed && !started) { view.onStart(); started = true } }
    override fun onResume() { if (!destroyed && !resumed) { view.onResume(); resumed = true } }
    override fun onPause() { if (!destroyed && resumed) { view.onPause(); resumed = false } }
    override fun onStop() { if (!destroyed && started) { view.onStop(); started = false } }
    override fun onLowMemory() { if (!destroyed) view.onLowMemory() }
    override fun close() {
        if (destroyed) return
        destroyed = true; styleGeneration++
        if (resumed) view.onPause()
        if (started) view.onStop()
        view.onDestroy(); resumed = false; started = false; map = null
    }
}
