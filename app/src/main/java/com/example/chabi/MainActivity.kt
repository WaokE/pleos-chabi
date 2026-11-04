package com.example.chabi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chabi.platform.CarUxMonitor
import com.example.chabi.ui.ChabiApp
import com.example.chabi.ui.ChabiViewModel

class MainActivity : ComponentActivity() {
    private val uxMonitor by lazy { CarUxMonitor(applicationContext) }
    private val viewModel: ChabiViewModel by viewModels {
        val app = application as ChabiApplication
        ChabiViewModel.factory(app.tripRepository, app.draftStore, app.defaultsRepository)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val access by uxMonitor.access.collectAsStateWithLifecycle()
            val app = application as ChabiApplication
            ChabiApp(viewModel, access, uxMonitor::start, app.mapProvider, app.qrEncoder)
        }
    }
    override fun onStart() { super.onStart(); uxMonitor.start() }
    override fun onStop() { uxMonitor.stop(); super.onStop() }
}
