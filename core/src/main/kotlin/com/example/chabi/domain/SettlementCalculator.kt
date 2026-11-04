package com.example.chabi.domain

object SettlementCalculator {
    /** Deterministic won allocation: registration order receives the remainder, one won each. */
    fun splitEvenly(amountWon: Long, participantIds: List<String>): Map<String, Long> {
        require(amountWon in 0..MAX_MONEY_WON) { "금액이 올바르지 않아요." }
        require(participantIds.isNotEmpty()) { "나눠 낼 사람을 한 명 이상 골라 주세요." }
        require(participantIds.distinct().size == participantIds.size) { "같은 사람이 두 번 들어 있어요." }
        val base = amountWon / participantIds.size
        val remainder = amountWon % participantIds.size
        return participantIds.mapIndexed { index, id ->
            id to base + if (index < remainder) 1L else 0L
        }.toMap()
    }

    fun calculate(trips: List<Trip>, draft: SettlementDraft): Settlement {
        require(trips.isNotEmpty()) { "정산할 운행을 하나 이상 골라 주세요." }
        require(trips.map { it.id }.distinct().size == trips.size) { "같은 운행이 두 번 들어 있어요." }
        require(draft.selectedTripIds == trips.map { it.id }.toSet()) { "고른 운행 중 찾을 수 없는 운행이 있어요. 운행을 다시 골라 주세요." }
        val participants = draft.participants
        require(participants.size in MIN_PARTICIPANTS..MAX_PARTICIPANTS) {
            "인원은 ${MIN_PARTICIPANTS}명에서 ${MAX_PARTICIPANTS}명 사이로 정해 주세요."
        }
        val ids = participants.map { it.id }
        require(ids.distinct().size == ids.size) { "같은 사람이 두 번 들어 있어요." }
        require(participants.all { it.name.isNotBlank() }) { "비어 있는 이름을 채워 주세요." }
        require(participants.map { participantNameKey(it.name) }.distinct().size == participants.size) {
            "이름이 같은 사람이 있어요. 서로 다르게 입력해 주세요."
        }
        require(draft.title.isNotBlank()) { "정산 이름을 입력해 주세요." }
        require(draft.expenses.map { it.id }.distinct().size == draft.expenses.size) { "같은 비용이 두 번 들어 있어요." }
        val estimate = EnergyCalculator.estimate(trips, EnergyCalculator.settings(draft))
        val energy = CostLine(ENERGY_COST_ID, "예상 ${draft.energyType.title}", estimate.amountWon,
            ids, splitEvenly(estimate.amountWon, ids))
        val extraCosts = draft.expenses.map { expense ->
            require(expense.title.isNotBlank()) { "비용 이름을 입력해 주세요." }
            require(expense.id != ENERGY_COST_ID) { "이 비용은 저장할 수 없어요. 지우고 다시 추가해 주세요." }
            require(expense.amountWon in 1..MAX_MONEY_WON) { "‘${expense.title}’ 금액은 1원 이상 입력해 주세요." }
            val chosen = expense.participantIds
            // Titles are user text, so attach particles to the fixed word "비용", never to the title.
            require(chosen == null || chosen.isNotEmpty()) { "‘${expense.title}’ 비용을 나눠 낼 사람을 한 명 이상 골라 주세요." }
            require(chosen == null || ids.containsAll(chosen)) { "‘${expense.title}’ 비용을 나눠 낼 사람 중 빠진 사람이 있어요. 다시 골라 주세요." }
            // Filter in registration order, not Set iteration order.
            val included = ids.filter { chosen == null || it in chosen }
            CostLine("expense:${expense.id}", expense.title, expense.amountWon,
                included, splitEvenly(expense.amountWon, included))
        }
        val costs = listOf(energy) + extraCosts
        val total = costs.fold(0L) { sum, cost -> Math.addExact(sum, cost.amountWon) }
        require(total <= MAX_MONEY_WON) { "총액이 계산할 수 있는 최대 금액을 넘었어요." }
        val shares = participants.map { person ->
            ParticipantShare(person, costs.sumOf { it.shares[person.id] ?: 0L })
        }
        check(shares.sumOf { it.amountWon } == total) { "나눈 금액의 합이 총액과 달라요. 다시 계산해 주세요." }
        return Settlement(
            draft.title.trim(), trips.size, trips.sumOf { it.distanceMeters },
            trips.sumOf { it.durationMinutes }, draft.energyType, estimate, costs, shares, total,
        )
    }
}
