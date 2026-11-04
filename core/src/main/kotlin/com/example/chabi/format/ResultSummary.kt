package com.example.chabi.format

import com.example.chabi.domain.*

/** Compact, self-contained phone handoff. No links, accounts, location traces or server upload. */
object ResultSummary {
    /** QR text: totals and each person's amount only, short enough for 20 people in one code. */
    fun create(result: Settlement, draft: SettlementDraft, isMock: Boolean): String = buildString {
        appendLine("[차비] 정산 요약")
        if (isMock) appendLine(SharedNotice.MOCK_TRIPS)
        appendLine("운행 ${result.tripCount}개 · ${distance(result.distanceMeters)}")
        appendLine("예상 ${result.energyType.title} ${won(result.energyEstimate.amountWon)}")
        // Read extra costs from the settlement, not the draft, so the QR always matches the computed total.
        val extraCosts = result.costs.filter { it.id != ENERGY_COST_ID }
        if (extraCosts.isNotEmpty()) appendLine("추가 비용 ${won(extraCosts.sumOf { it.amountWon })}")
        appendLine("총액 ${won(result.totalWon)} · 운전자 포함 ${result.shares.size}명")
        result.shares.forEachIndexed { index, share ->
            appendLine("${index + 1}. ${share.participant.name.trim().take(12)} ${won(share.amountWon)}")
        }
        appendLine(priceLine(result, draft))
        if (draft.priceIsMock) appendLine(SharedNotice.MOCK_PRICE)
        appendLine(SharedNotice.REMAINDER)
        append("실제 주유·충전 금액과 다를 수 있어요. 송금은 하지 않아요.")
    }

    /** Copy/share text: the full breakdown plus the unit price it was computed with. */
    fun full(result: Settlement, draft: SettlementDraft, isMock: Boolean): String = buildString {
        append(SettlementText.create(result, isMock))
        append("\n\n").append(priceLine(result, draft))
        if (draft.priceIsMock) append("\n").append(SharedNotice.MOCK_PRICE)
    }

    /** "단가 1700원/L · 시연용 기준 단가 · 2026-11-04"; the date is omitted when unknown. */
    private fun priceLine(result: Settlement, draft: SettlementDraft): String =
        "단가 ${draft.unitPriceInput}원/${result.energyType.quantityUnit} · ${draft.priceSource}" +
            draft.priceAsOf.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
}
