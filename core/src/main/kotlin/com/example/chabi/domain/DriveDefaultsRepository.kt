package com.example.chabi.domain


/** Cached vehicle/pricing defaults. Replace this binding after supported data sources are verified. */
interface DriveDefaultsRepository {
    suspend fun getDefaults(): DriveDefaults
}

data class EnergyPreset(
    val id: String, val label: String, val type: EnergyType,
    val efficiency: String, val price: String,
    val priceSource: String, val asOf: String, val isMock: Boolean,
)

data class DriveDefaults(val vehicleLabel: String, val recommendedId: String, val presets: List<EnergyPreset>)
