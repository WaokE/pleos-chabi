package com.example.chabi.format

import com.example.chabi.domain.*

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

fun won(value: Long): String = "${NumberFormat.getIntegerInstance(Locale.KOREA).format(value)}원"
fun distance(valueMeters: Long): String = BigDecimal.valueOf(valueMeters).movePointLeft(3)
    .stripTrailingZeros().toPlainString() + " km"
fun duration(minutes: Long): String = when {
    minutes < 60 -> "${minutes}분"
    minutes % 60 == 0L -> "${minutes / 60}시간"
    else -> "${minutes / 60}시간 ${minutes % 60}분"
}

/** Notices shared by the copy/share text and the QR summary, so both say the same thing. */
internal object SharedNotice {
    const val MOCK_TRIPS = "※ 예시 운행으로 계산한 결과예요."
    const val MOCK_PRICE = "예시 단가예요. 실제 평균가가 아니에요."
    const val REMAINDER = "나누고 남은 1원은 등록 순서대로 한 명씩 더했어요."
    const val ESTIMATE = "연비와 단가로 계산한 금액이라 실제 주유·충전 금액과 다를 수 있어요."
}

/** Full text for copy and Android share: every cost line, who shares it, and each person's amount. */
object SettlementText {
    fun create(result: Settlement, isMock: Boolean): String = buildString {
        appendLine("[차비] ${result.title.trim()}")
        if (isMock) appendLine(SharedNotice.MOCK_TRIPS)
        appendLine("운행 ${result.tripCount}개 · ${distance(result.distanceMeters)} · ${duration(result.durationMinutes)}")
        appendLine()
        result.costs.forEach { cost ->
            val label = if (cost.id == ENERGY_COST_ID) "예상 ${result.energyType.title}" else cost.title
            appendLine("$label ${won(cost.amountWon)}")
            if (cost.participantIds.size != result.shares.size) {
                val names = result.shares.filter { it.participant.id in cost.participantIds }
                    .joinToString(", ") { it.participant.name.trim() }
                appendLine("  나눠 낸 사람: $names")
            }
        }
        appendLine()
        appendLine("총액 ${won(result.totalWon)} · ${result.shares.size}명")
        result.shares.forEach { appendLine("${it.participant.name.trim()} ${won(it.amountWon)}") }
        appendLine()
        appendLine(SharedNotice.REMAINDER)
        append(SharedNotice.ESTIMATE)
    }
}
