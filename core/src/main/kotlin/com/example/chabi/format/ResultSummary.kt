package com.example.chabi.format

import com.example.chabi.domain.*

/** Compact, self-contained phone handoff. No links, accounts, location traces or server upload. */
object ResultSummary {
    /** Compact enough for 20 people in one code. */
    fun create(result: Settlement, draft: SettlementDraft, isMock: Boolean): String = buildString {
        appendLine("[차비] 정산 요약")
        if (isMock) appendLine("※ 예시 운행으로 계산한 결과예요.")
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
        if (draft.priceIsMock) appendLine("예시 단가예요. 실제 평균가가 아니에요.")
        if (result.hasRemainderAllocation) appendLine("비용을 나누고 남은 원은 등록 순서대로 1원씩 더했어요.")
        append("실제 주유·충전 금액과 다를 수 있어요. 송금은 하지 않아요.")
    }

    /** "단가 1700원/L · 시연용 기준 단가 · 2026-11-04"; the date is omitted when unknown. */
    private fun priceLine(result: Settlement, draft: SettlementDraft): String =
        "단가 ${draft.unitPriceInput}원/${result.energyType.quantityUnit} · ${draft.priceSource}" +
            draft.priceAsOf.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
}
