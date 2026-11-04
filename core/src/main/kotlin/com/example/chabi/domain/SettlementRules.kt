package com.example.chabi.domain

/** Upper bound for any settlement total, in whole won. */
const val MAX_MONEY_WON: Long = 999_999_999_999L

/** Upper bound for one extra expense entered in the UI, in whole won. */
const val MAX_EXPENSE_WON: Long = 999_999_999L

/** Participant count range, driver included. */
const val MIN_PARTICIPANTS: Int = 1
const val MAX_PARTICIPANTS: Int = 20

/** Cost line ID reserved for the energy (fuel/charging) cost; extra expenses may not use it. */
const val ENERGY_COST_ID: String = "energy"

/**
 * Identity used to detect ambiguous participant names.
 * Names that differ only by surrounding whitespace or letter case count as duplicates.
 */
fun participantNameKey(name: String): String = name.trim().lowercase()

/** True when [name] is blank or collides with another participant's name under [participantNameKey]. */
fun isInvalidParticipantName(name: String, participants: List<Participant>): Boolean =
    name.isBlank() || participants.count { participantNameKey(it.name) == participantNameKey(name) } > 1
