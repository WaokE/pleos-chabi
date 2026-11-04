package com.example.chabi.data

import android.content.Context
import com.example.chabi.domain.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** One local draft, no account or network. Monetary JSON values remain integers. */
class LocalDraftStore(context: Context, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : DraftStore {
    private val preferences = context.getSharedPreferences("chabi_draft", Context.MODE_PRIVATE)
    private val writeLock = Mutex()
    override suspend fun load(): SettlementDraft? = withContext(ioDispatcher) {
        val raw = preferences.getString("draft", null) ?: return@withContext null
        val value = JSONObject(raw)
        require(value.getInt("version") == 1)
        SettlementDraft(
            title = value.getString("title"),
            selectedTripIds = value.getJSONArray("trips").strings().toSet(),
            energyType = EnergyType.valueOf(value.getString("energyType")),
            efficiencyInput = value.getString("efficiency"), unitPriceInput = value.getString("price"),
            useTripEfficiency = value.getBoolean("useTripEfficiency"),
            energyProfileId = value.optString("energyProfileId", if (value.getString("energyType") == "ELECTRIC") "electric" else "gasoline"),
            priceSource = value.optString("priceSource", "저장된 기준 단가"),
            priceAsOf = value.optString("priceAsOf", ""), priceIsMock = value.optBoolean("priceIsMock", true),
            participants = value.getJSONArray("participants").objects().map { Participant(it.getString("id"), it.getString("name")) },
            expenses = value.getJSONArray("expenses").objects().map {
                Expense(it.getString("id"), ExpenseCategory.valueOf(it.getString("category")),
                    it.getString("title"), it.getLong("amount"), it.optString("memo"),
                    if (it.isNull("people")) null else it.getJSONArray("people").strings().toSet())
            },
        )
    }
    override suspend fun save(draft: SettlementDraft): Unit = withContext(ioDispatcher) { writeLock.withLock {
        val value = JSONObject().apply {
            put("version", 1); put("title", draft.title)
            put("trips", JSONArray(draft.selectedTripIds.toList()))
            put("energyType", draft.energyType.name); put("efficiency", draft.efficiencyInput)
            put("price", draft.unitPriceInput); put("useTripEfficiency", draft.useTripEfficiency)
            put("energyProfileId", draft.energyProfileId); put("priceSource", draft.priceSource)
            put("priceAsOf", draft.priceAsOf); put("priceIsMock", draft.priceIsMock)
            put("participants", JSONArray(draft.participants.map { JSONObject().put("id", it.id).put("name", it.name) }))
            put("expenses", JSONArray(draft.expenses.map {
                JSONObject().put("id", it.id).put("category", it.category.name).put("title", it.title)
                    .put("amount", it.amountWon).put("memo", it.memo)
                    .put("people", it.participantIds?.let { ids -> JSONArray(ids.toList()) } ?: JSONObject.NULL)
            }))
        }
        check(preferences.edit().putString("draft", value.toString()).commit()) { "Draft persistence failed" }
    } }
    private fun JSONArray.strings() = (0 until length()).map { getString(it) }
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
}
