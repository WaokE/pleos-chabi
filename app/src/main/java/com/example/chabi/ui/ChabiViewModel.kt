package com.example.chabi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chabi.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel

class ChabiViewModel(private val repository: TripRepository, private val store: DraftStore,
    private val defaultsRepository: DriveDefaultsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DriveUiState(source = repository.source))
    val state = mutableState.asStateFlow()
    private val editor = DraftEditor()
    private val saves = Channel<SettlementDraft>(Channel.UNLIMITED)
    private var restored = false
    private var hadSavedDraft = false
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            for (draft in saves) {
                try { store.save(draft) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    mutableState.value = mutableState.value.copy(notice = "입력한 내용을 저장하지 못했어요. 앱을 닫으면 최근 입력을 잃을 수 있어요.")
                }
            }
        }
        loadTrips()
    }

    fun loadTrips() {
        loadJob?.cancel()
        mutableState.value = mutableState.value.copy(loading = true, loadError = false)
        loadJob = viewModelScope.launch {
            try {
                restoreDraftOnce()
                val defaults = mutableState.value.defaults ?: defaultsRepository.getDefaults()
                require(defaults.presets.isNotEmpty() && defaults.presets.any { it.id == defaults.recommendedId })
                val trips = repository.getTrips()
                require(trips.map { it.id }.distinct().size == trips.size)
                val state = mutableState.value
                val missing = state.draft.selectedTripIds - trips.map { it.id }.toSet()
                val draft = if (!hadSavedDraft && state.defaults == null)
                    editor.withPreset(state.draft, defaults.presets.first { it.id == defaults.recommendedId }) else state.draft
                mutableState.value = derive(state.copy(draft = draft, defaults = defaults, trips = trips.sortedBy { it.startedAt }, loading = false,
                    notice = if (missing.isNotEmpty()) "저장된 운행 중 ${missing.size}개를 찾을 수 없어요. 운행을 다시 골라 주세요." else state.notice))
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { mutableState.value = mutableState.value.copy(loading = false, loadError = true) }
        }
    }
    /**
     * Reads the saved draft on the first load only, so a retry after a trip-load failure
     * never overwrites edits made since. A failed read keeps the default draft and shows a notice.
     */
    private suspend fun restoreDraftOnce() {
        if (restored) return
        try {
            val saved = store.load()
            hadSavedDraft = saved != null
            mutableState.value = mutableState.value.copy(draft = saved ?: SettlementDraft())
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(notice = "저장된 입력을 불러오지 못해서 새로 시작해요.")
        }
        restored = true
    }

    fun setStep(step: Step) {
        val state = mutableState.value
        if (step == Step.RESULT && state.settlement == null) return
        if (step == Step.REVIEW && !state.hasCompleteTripSelection) return
        val next = state.copy(step = step)
        val stage = step.draftStage
        if (stage != null && state.hasDraft && stage != state.draft.resumeStage) {
            save(next.copy(draft = state.draft.copy(resumeStage = stage)))
        } else {
            mutableState.value = next
        }
    }
    fun next() { if (mutableState.value.canContinue) setStep(when (mutableState.value.step) {
        Step.HOME -> Step.TRIPS; Step.TRIPS -> Step.REVIEW; Step.REVIEW -> Step.RESULT; Step.RESULT -> Step.RESULT
    }) }
    fun back() = setStep(when (mutableState.value.step) {
        Step.HOME -> Step.HOME; Step.TRIPS -> Step.HOME; Step.REVIEW -> Step.TRIPS; Step.RESULT -> Step.REVIEW
    })
    fun resumeDraft() = setStep(mutableState.value.resumeStep)
    fun startNew() {
        val state = mutableState.value
        val defaults = state.defaults ?: return
        val draft = editor.withPreset(SettlementDraft(), defaults.presets.first { it.id == defaults.recommendedId })
        save(derive(state.copy(draft = draft, step = Step.TRIPS, filter = TripFilter.ALL, focusedTripId = null)))
    }
    fun focusTrip(id: String) { if (mutableState.value.trips.any { it.id == id }) mutableState.value = mutableState.value.copy(focusedTripId = id) }
    fun setFilter(filter: TripFilter) { mutableState.value = mutableState.value.copy(filter = filter) }
    fun dismissNotice() { mutableState.value = mutableState.value.copy(notice = null) }
    fun toggleTrip(id: String) {
        if (mutableState.value.trips.none { it.id == id }) return
        change { copy(selectedTripIds = selectedTripIds.let { if (id in it) it - id else it + id }) }
        focusTrip(id)
    }
    fun toggleGroup(ids: Set<String>) {
        val available = mutableState.value.trips.map { it.id }.toSet()
        val valid = ids.intersect(available)
        if (valid.isEmpty()) return
        change { copy(selectedTripIds = if (selectedTripIds.containsAll(valid)) selectedTripIds - valid else selectedTripIds + valid) }
        mutableState.value.trips.firstOrNull { it.id in valid }?.let { focusTrip(it.id) }
    }
    fun clearTrips() = change { copy(selectedTripIds = emptySet()) }
    fun setEfficiency(value: String) = change { copy(efficiencyInput = value.take(12)) }
    fun restoreReferencePrice() {
        val preset = mutableState.value.defaults?.presets?.find { it.id == mutableState.value.draft.energyProfileId } ?: return
        change { copy(unitPriceInput = preset.price, priceSource = preset.priceSource, priceAsOf = preset.asOf, priceIsMock = preset.isMock) }
    }
    fun applyCostSettings(profileId: String, efficiency: String, price: String, useTrip: Boolean, referencePrice: Boolean) {
        val preset = mutableState.value.defaults?.presets?.find { it.id == profileId } ?: return
        change { editor.applyCostSettings(this, preset, efficiency, price, useTrip, referencePrice) }
    }
    fun saveExpense(expense: Expense) = change {
        copy(expenses = if (expenses.any { it.id == expense.id }) expenses.map { if (it.id == expense.id) expense else it } else expenses + expense)
    }
    fun removeExpense(id: String) = change { copy(expenses = expenses.filterNot { it.id == id }) }
    fun setExpensePeople(id: String, ids: Set<String>?) = change {
        copy(expenses = expenses.map { if (it.id == id) it.copy(participantIds = ids) else it })
    }
    fun setParticipantCount(count: Int) = change { editor.setParticipantCount(this, count) }
    fun renamePerson(id: String, name: String) = change {
        copy(participants = participants.map { if (it.id == id) it.copy(name = name.take(20)) else it })
    }
    private fun change(block: SettlementDraft.() -> SettlementDraft) {
        val state = mutableState.value
        val edited = state.draft.block()
        val draft = edited.copy(resumeStage = state.step.draftStage ?: edited.resumeStage)
        save(derive(state.copy(draft = draft)))
    }
    private fun save(state: DriveUiState) {
        mutableState.value = state
        saves.trySend(state.draft)
    }
    private fun derive(state: DriveUiState): DriveUiState {
        val preview = SettlementPreview.evaluate(state.trips, state.draft)
        return state.copy(selectedTrips = preview.selectedTrips, energy = preview.energy,
            energyError = preview.energyError, settlement = preview.settlement, settlementError = preview.settlementError)
    }
    companion object {
        fun factory(repository: TripRepository, store: DraftStore,
            defaultsRepository: DriveDefaultsRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ChabiViewModel::class.java))
                return ChabiViewModel(repository, store, defaultsRepository) as T
            }
        }
    }
}
