package com.example.chabi.ui

import com.example.chabi.data.*
import com.example.chabi.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChabiViewModelTest {
    private class MemoryStore(var draft: SettlementDraft? = null) : DraftStore {
        override suspend fun load() = draft
        override suspend fun save(draft: SettlementDraft) { this.draft = draft }
    }
    private fun mockTrips() = MockTripRepository(ioDispatcher = UnconfinedTestDispatcher())
    // Fixtures belong to tests, never the production ViewModel.
    private fun ChabiViewModel.addDemoExpenses() {
        listOf(
            Expense("demo-toll", ExpenseCategory.TOLL, "왕복 통행료", 24_000),
            Expense("demo-parking", ExpenseCategory.PARKING, "숙소·해변 주차", 8_000),
            Expense("demo-coffee", ExpenseCategory.OTHER, "커피", 10_001, participantIds = state.value.draft.participants.take(2).map { it.id }.toSet()),
        ).forEach(::saveExpense)
    }
    @Before fun setDispatcher() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun resetDispatcher() { Dispatchers.resetMain() }

    @Test fun `multi selection stays unique and deselection updates all totals`() {
        val vm = ChabiViewModel(mockTrips(), MemoryStore(), MockDriveDefaultsRepository())
        vm.toggleGroup(MockTripRepository.weekendIds)
        assertEquals(4, vm.state.value.selectedTrips.size)
        assertEquals(415_000L, vm.state.value.distanceMeters)
        assertEquals(56_440L, vm.state.value.energy!!.amountWon)
        vm.setFilter(TripFilter.SELECTED)
        assertEquals(415_000L, vm.state.value.distanceMeters)
        vm.toggleTrip("weekend-3")
        assertEquals(391_000L, vm.state.value.distanceMeters)
        vm.toggleTrip("weekend-3")
        assertEquals(415_000L, vm.state.value.distanceMeters)
        vm.toggleGroup(MockTripRepository.weekendIds)
        assertTrue(vm.state.value.selectedTrips.isEmpty())
        assertNull(vm.state.value.settlement)
    }

    @Test fun `demo expense addition is idempotent and edits replace the same expense`() {
        val vm = ChabiViewModel(mockTrips(), MemoryStore(), MockDriveDefaultsRepository())
        repeat(3) { vm.addDemoExpenses() }
        assertEquals(3, vm.state.value.draft.expenses.size)
        assertEquals(42_001L, vm.state.value.additionalWon)
        val coffee = vm.state.value.draft.expenses.last()
        vm.saveExpense(coffee.copy(amountWon = 5000))
        assertEquals(3, vm.state.value.draft.expenses.size)
        assertEquals(37_000L, vm.state.value.additionalWon)
    }

    @Test fun `removing last cost participant leaves an invalid empty subset`() {
        val vm = ChabiViewModel(mockTrips(), MemoryStore(), MockDriveDefaultsRepository())
        vm.toggleGroup(MockTripRepository.weekendIds)
        val last = vm.state.value.draft.participants.last().id
        vm.saveExpense(Expense("coffee", ExpenseCategory.OTHER, "커피", 1001, participantIds = setOf(last)))
        vm.setParticipantCount(3)
        assertEquals(emptySet<String>(), vm.state.value.draft.expenses.single().participantIds)
        assertNull(vm.state.value.settlement)
        vm.setExpensePeople("coffee", setOf("me"))
        assertNotNull(vm.state.value.settlement)
    }

    @Test fun `3 4 and 5 participant flows reconcile after changing counts`() {
        val vm = ChabiViewModel(mockTrips(), MemoryStore(), MockDriveDefaultsRepository())
        vm.toggleGroup(MockTripRepository.weekendIds)
        vm.addDemoExpenses()
        for (count in listOf(3, 4, 5)) {
            vm.setParticipantCount(count)
            val result = vm.state.value.settlement!!
            assertEquals(count, result.shares.size)
            assertEquals(98_441L, result.totalWon)
            assertEquals(result.totalWon, result.shares.sumOf { it.amountWon })
        }
    }

    @Test fun `local draft restores selected trips energy expenses and names`() {
        val store = MemoryStore()
        val first = ChabiViewModel(mockTrips(), store, MockDriveDefaultsRepository())
        first.toggleGroup(MockTripRepository.weekendIds)
        first.setEfficiency("15")
        first.renamePerson("me", "운전자")
        first.addDemoExpenses()
        val restored = ChabiViewModel(mockTrips(), store, MockDriveDefaultsRepository()).state.value
        assertEquals(415_000L, restored.distanceMeters)
        assertEquals("15", restored.draft.efficiencyInput)
        assertEquals("운전자", restored.draft.participants.first().name)
        assertEquals(3, restored.draft.expenses.size)
        assertNotNull(restored.settlement)
    }

    @Test fun `unavailable saved trip blocks results rather than silently dropping cost`() {
        val store = MemoryStore(SettlementDraft(
            selectedTripIds = setOf("weekend-1", "unavailable"), resumeStage = DraftStage.RESULT,
        ))
        val vm = ChabiViewModel(mockTrips(), store, MockDriveDefaultsRepository())
        vm.resumeDraft()
        val state = vm.state.value
        assertEquals(Step.TRIPS, state.step)
        assertFalse(state.canContinue)
        assertNull(state.settlement)
        assertNotNull(state.notice)
    }

    @Test fun `repository failure is shown without losing the existing draft`() {
        val failing = object : TripRepository {
            override val source = TripDataSource.VEHICLE
            override suspend fun getTrips(): List<Trip> = error("offline")
            override suspend fun getTripDetail(id: String): Trip? = null
        }
        val input = SettlementDraft(title = "저장된 여행")
        val state = ChabiViewModel(failing, MemoryStore(input), MockDriveDefaultsRepository()).state.value
        assertTrue(state.loadError)
        assertEquals("저장된 여행", state.draft.title)
        assertEquals(TripDataSource.VEHICLE, state.source)
    }
    @Test fun `short flow requires explicit selection confirmation and valid result`() {
        val vm = ChabiViewModel(mockTrips(), MemoryStore(), MockDriveDefaultsRepository())
        assertEquals(Step.HOME, vm.state.value.step)
        vm.startNew()
        vm.next()
        assertEquals(Step.TRIPS, vm.state.value.step)
        vm.toggleGroup(MockTripRepository.weekendIds)
        assertEquals(Step.TRIPS, vm.state.value.step)
        vm.next()
        assertEquals(Step.REVIEW, vm.state.value.step)
        assertEquals(14_110L, vm.state.value.settlement!!.shares.first().amountWon)
        vm.setEfficiency("0")
        vm.next()
        assertEquals(Step.REVIEW, vm.state.value.step)
        vm.setEfficiency("12.5")
        vm.next()
        assertEquals(Step.RESULT, vm.state.value.step)
        vm.back()
        assertEquals(Step.REVIEW, vm.state.value.step)
        assertEquals(415_000L, vm.state.value.distanceMeters)
    }

    @Test fun `resume returns to the last step from home and after restarting the app`() {
        val store = MemoryStore()
        fun reopen() = ChabiViewModel(mockTrips(), store, MockDriveDefaultsRepository())
        val first = reopen()
        first.startNew()
        first.toggleTrip("weekend-1")
        first.back()
        first.resumeDraft()
        assertEquals(Step.TRIPS, first.state.value.step)

        val atSelection = reopen()
        atSelection.resumeDraft()
        assertEquals(Step.TRIPS, atSelection.state.value.step)
        assertEquals(98_000L, atSelection.state.value.distanceMeters)
        atSelection.next()

        val atReview = reopen()
        atReview.resumeDraft()
        assertEquals(Step.REVIEW, atReview.state.value.step)
        atReview.next()

        val atResult = reopen()
        atResult.resumeDraft()
        assertEquals(Step.RESULT, atResult.state.value.step)
        atResult.back()
        atResult.back()
        atResult.back()
        assertEquals(Step.HOME, atResult.state.value.step)
        atResult.resumeDraft()
        assertEquals(Step.TRIPS, atResult.state.value.step)
    }

    @Test fun `new settlement resets extras and people while resume preserves all legacy inputs`() {
        val original = SettlementDraft(selectedTripIds = MockTripRepository.weekendIds,
            expenses = listOf(Expense("legacy", ExpenseCategory.OTHER, "커피", 1001, participantIds = setOf("me"))))
        val store = MemoryStore(original)
        val vm = ChabiViewModel(mockTrips(), store, MockDriveDefaultsRepository())
        vm.resumeDraft()
        assertEquals(Step.REVIEW, vm.state.value.step)
        assertEquals(original.copy(resumeStage = DraftStage.REVIEW), vm.state.value.draft)
        vm.startNew()
        assertTrue(vm.state.value.draft.selectedTripIds.isEmpty())
        assertTrue(vm.state.value.draft.expenses.isEmpty())
        assertEquals(4, vm.state.value.draft.participants.size)
        assertFalse(vm.state.value.hasDraft)
        assertEquals("시연용 기준 단가", vm.state.value.draft.priceSource)
        assertEquals(vm.state.value.draft, store.draft)
    }

    @Test fun `manual quote basis survives resume and result instead of refreshing defaults`() {
        val store = MemoryStore()
        val vm = ChabiViewModel(mockTrips(), store, MockDriveDefaultsRepository())
        vm.startNew()
        vm.toggleGroup(MockTripRepository.weekendIds)
        vm.next()
        vm.applyCostSettings("gasoline", "12.5", "1800", false, false)
        assertEquals("직접 입력", vm.state.value.draft.priceSource)
        assertFalse(vm.state.value.draft.priceIsMock)
        val restored = ChabiViewModel(mockTrips(), store, MockDriveDefaultsRepository())
        restored.resumeDraft()
        restored.next()
        assertEquals(Step.RESULT, restored.state.value.step)
        assertEquals("1800", restored.state.value.draft.unitPriceInput)
        assertEquals(59_760L, restored.state.value.settlement!!.totalWon)
        restored.restoreReferencePrice()
        assertEquals("1700", restored.state.value.draft.unitPriceInput)
        assertTrue(restored.state.value.draft.priceIsMock)
    }

    @Test fun `electric preset uses electric units and invalid settings are never committed`() {
        val vm = ChabiViewModel(mockTrips(), MemoryStore(), MockDriveDefaultsRepository())
        vm.startNew(); vm.toggleGroup(MockTripRepository.weekendIds)
        vm.applyCostSettings("electric", "5", "300", false, true)
        assertEquals(EnergyType.ELECTRIC, vm.state.value.draft.energyType)
        assertEquals(24_900L, vm.state.value.settlement!!.totalWon)
        val valid = vm.state.value.draft
        vm.applyCostSettings("electric", "0", "300", false, true)
        assertEquals(valid, vm.state.value.draft)
    }

    @Test fun `replacement repositories supply vehicle records and energy defaults without mock assumptions`() {
        val time = java.time.LocalDateTime.of(2026, 11, 4, 8, 0)
        val trip = Trip("vehicle-trip-42", time, time.plusHours(1), "출발지", "도착지", 10_000,
            energyType = EnergyType.ELECTRIC, consumedUnits = java.math.BigDecimal("2.5"))
        val trips = object : TripRepository {
            override val source = TripDataSource.VEHICLE
            override suspend fun getTrips() = listOf(trip)
            override suspend fun getTripDetail(id: String) = trip.takeIf { it.id == id }
        }
        val defaults = object : DriveDefaultsRepository {
            override suspend fun getDefaults() = DriveDefaults("연결된 차량", "vehicle-electric", listOf(
                EnergyPreset("vehicle-electric", "전기", EnergyType.ELECTRIC, "4", "301.2", "테스트 공급원", "2026-11-04", false)))
        }
        val vm = ChabiViewModel(trips, MemoryStore(), defaults)
        vm.startNew(); vm.toggleTrip(trip.id); vm.next()
        val state = vm.state.value
        assertEquals(TripDataSource.VEHICLE, state.source)
        assertEquals("vehicle-electric", state.draft.energyProfileId)
        assertFalse(state.draft.priceIsMock)
        assertEquals(753L, state.settlement!!.totalWon)
        assertEquals(753L, state.settlement.shares.sumOf { it.amountWon })
    }

    @Test fun `route geometry changes do not alter recorded distance or settlement`() {
        val trip = run {
            val time = java.time.LocalDateTime.of(2026, 11, 4, 8, 0)
            Trip("same-trip", time, time.plusHours(1), "출발지", "도착지", 98_000)
        }
        fun state(points: List<GeoPoint>): DriveUiState {
            val repository = object : TripRepository {
                override val source = TripDataSource.VEHICLE
                override suspend fun getTrips() = listOf(trip.copy(route = points))
                override suspend fun getTripDetail(id: String) = getTrips().find { it.id == id }
            }
            val vm = ChabiViewModel(repository, MemoryStore(), MockDriveDefaultsRepository())
            vm.startNew(); vm.toggleTrip(trip.id)
            return vm.state.value
        }
        val missing = state(emptyList())
        val routed = state(listOf(GeoPoint(37.566, 126.978), GeoPoint(38.21, 128.59)))
        assertEquals(missing.distanceMeters, routed.distanceMeters)
        assertEquals(missing.settlement, routed.settlement)
    }

    @Test fun `slow storage load keeps loading state and restores input before evaluating`() {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val input = SettlementDraft(selectedTripIds = setOf("weekend-1"), efficiencyInput = "15")
        val storage = object : DraftStore {
            override suspend fun load(): SettlementDraft { gate.await(); return input }
            override suspend fun save(draft: SettlementDraft) = Unit
        }
        val vm = ChabiViewModel(mockTrips(), storage, MockDriveDefaultsRepository())
        assertTrue(vm.state.value.loading)
        gate.complete(Unit)
        assertFalse(vm.state.value.loading)
        assertEquals(input, vm.state.value.draft)
        assertEquals(98_000L, vm.state.value.distanceMeters)
    }
    @Test fun `slow writes keep UI responsive and persist drafts in order`() {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val writes = mutableListOf<String>()
        val storage = object : DraftStore {
            override suspend fun load(): SettlementDraft? = null
            override suspend fun save(draft: SettlementDraft) { gate.await(); writes += draft.efficiencyInput }
        }
        val vm = ChabiViewModel(mockTrips(), storage, MockDriveDefaultsRepository())
        vm.toggleTrip("weekend-1"); vm.setEfficiency("15"); vm.setEfficiency("16")
        assertEquals("16", vm.state.value.draft.efficiencyInput)
        assertTrue(writes.isEmpty())
        gate.complete(Unit)
        assertEquals(listOf("12.5", "15", "16"), writes)
    }
    @Test fun `write failure retains edited draft and reports persistence failure`() {
        val storage = object : DraftStore {
            override suspend fun load(): SettlementDraft? = null
            override suspend fun save(draft: SettlementDraft): Unit = error("disk full")
        }
        val vm = ChabiViewModel(mockTrips(), storage, MockDriveDefaultsRepository())
        vm.toggleTrip("weekend-1")
        assertEquals(setOf("weekend-1"), vm.state.value.draft.selectedTripIds)
        assertNotNull(vm.state.value.settlement)
        assertTrue(vm.state.value.notice!!.contains("저장하지 못했어요"))
    }

}
