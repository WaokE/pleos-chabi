package com.example.chabi.ui

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chabi.map.*

/** Shared Pleos controls and lifecycle host. This file knows no concrete map SDK. */
@Composable
fun EmbeddedRouteMap(provider: RouteMapProvider, scene: RouteMapScene, modifier: Modifier,
    onFocusTrip: (String) -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val colors = LocalPleosColors.current
    val callback by rememberUpdatedState(onFocusTrip)
    val session = remember(provider, context) { provider.create(context) { callback(it) } }
    val loadState by session.loadState.collectAsStateWithLifecycle()
    val appearance = MapAppearance(colors.dark, colors.accent.toArgb(), colors.popup.toArgb(),
        colors.secondary.toArgb(), colors.text.toArgb(), colors.warning.toArgb())
    DisposableEffect(owner, session, context) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> session.onStart()
                Lifecycle.Event.ON_RESUME -> session.onResume()
                Lifecycle.Event.ON_PAUSE -> session.onPause()
                Lifecycle.Event.ON_STOP -> session.onStop()
                Lifecycle.Event.ON_DESTROY -> session.close()
                else -> Unit
            }
        }
        // API 34 devices still deliver the legacy memory callbacks.
        @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
        val memory = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() = session.onLowMemory()
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) session.onLowMemory()
            }
        }
        context.applicationContext.registerComponentCallbacks(memory)
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            context.applicationContext.unregisterComponentCallbacks(memory)
            session.close()
        }
    }
    Box(modifier.clip(RoundedCornerShape(24.dp)).background(colors.high).testTag("route-map")) {
        AndroidView(factory = { session.view }, modifier = Modifier.fillMaxSize(), update = { session.update(scene, appearance) })
        val message = when (loadState) {
            MapLoadState.Loading -> "지도를 불러오고 있어요"
            MapLoadState.Failed -> "지도를 불러오지 못했어요. 연결을 확인해 주세요."
            MapLoadState.NoRoute -> "이 운행에는 기록된 경로가 없어요"
            MapLoadState.Ready -> null
        }
        message?.let { text ->
            Surface(Modifier.align(Alignment.TopCenter).padding(12.dp), color = colors.popup, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text, style = MaterialTheme.typography.labelMedium)
                    if (loadState == MapLoadState.Failed) PleosTextButton(onClick = session::retry,
                        modifier = Modifier.testTag("retry-map")) { Text("다시 불러오기") }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PleosOutlinedButton(onClick = session::zoomIn,
                modifier = Modifier.size(80.dp).testTag("map-zoom-in"), contentPadding = PaddingValues(0.dp)) { Text("+") }
            PleosOutlinedButton(onClick = session::zoomOut,
                modifier = Modifier.size(80.dp).testTag("map-zoom-out"), contentPadding = PaddingValues(0.dp)) { Text("−") }
            PleosOutlinedButton(onClick = session::fitRoutes,
                modifier = Modifier.heightIn(min = 80.dp).testTag("map-fit")) { Text("전체 경로") }
        }
    }
}
