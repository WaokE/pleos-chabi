package com.example.chabi.platform

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DrivingAccess { CHECKING, PARKED, RESTRICTED, UNAVAILABLE }

/** AAOS platform UX only. Independent of the trip repository and the Pleos data SDK. */
class CarUxMonitor(private val context: Context) {
    private val mutable = MutableStateFlow(DrivingAccess.CHECKING)
    val access = mutable.asStateFlow()
    private val handler = Handler(Looper.getMainLooper())
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null
    private var generation = 0

    fun start() {
        stop()
        val token = generation
        mutable.value = DrivingAccess.CHECKING
        // The MVP is verified on Pleos API 34. Never allow editing if status is unknown.
        if (Build.VERSION.SDK_INT < 29) {
            mutable.value = DrivingAccess.UNAVAILABLE
            return
        }
        handler.postDelayed({
            if (token == generation && mutable.value == DrivingAccess.CHECKING) mutable.value = DrivingAccess.UNAVAILABLE
        }, 5_000)
        try {
            car = Car.createCar(context, handler, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { readyCar, ready ->
                if (token != generation) return@createCar
                if (!ready) {
                    mutable.value = DrivingAccess.UNAVAILABLE
                    return@createCar
                }
                try {
                    val ux = readyCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                    manager = ux
                    ux.registerListener { restriction ->
                        if (token == generation) mutable.value = if (restriction.isRequiresDistractionOptimization)
                            DrivingAccess.RESTRICTED else DrivingAccess.PARKED
                    }
                    mutable.value = if (ux.currentCarUxRestrictions.isRequiresDistractionOptimization)
                        DrivingAccess.RESTRICTED else DrivingAccess.PARKED
                } catch (_: Exception) { mutable.value = DrivingAccess.UNAVAILABLE }
            }
        } catch (_: Exception) { mutable.value = DrivingAccess.UNAVAILABLE
        } catch (_: LinkageError) { mutable.value = DrivingAccess.UNAVAILABLE }
    }

    fun stop() {
        ++generation
        handler.removeCallbacksAndMessages(null)
        runCatching { manager?.unregisterListener() }
        manager = null
        runCatching { car?.disconnect() }
        car = null
        mutable.value = DrivingAccess.CHECKING
    }
}
