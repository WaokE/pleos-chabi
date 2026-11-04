package com.example.chabi.domain

interface DraftStore {
    suspend fun load(): SettlementDraft?
    suspend fun save(draft: SettlementDraft)
}
