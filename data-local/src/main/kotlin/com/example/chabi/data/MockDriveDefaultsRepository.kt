package com.example.chabi.data

import com.example.chabi.domain.*

class MockDriveDefaultsRepository : DriveDefaultsRepository {
    override suspend fun getDefaults() = DriveDefaults("예시 차량", "gasoline", listOf(
        EnergyPreset("gasoline", "휘발유", EnergyType.FUEL, "12.5", "1700", "시연용 기준 단가", "2026-11-04", true),
        EnergyPreset("diesel", "경유", EnergyType.FUEL, "15", "1550", "시연용 기준 단가", "2026-11-04", true),
        EnergyPreset("lpg", "LPG", EnergyType.FUEL, "9", "1000", "시연용 기준 단가", "2026-11-04", true),
        EnergyPreset("electric", "전기", EnergyType.ELECTRIC, "5", "300", "시연용 충전 단가", "2026-11-04", true),
    ))
}
