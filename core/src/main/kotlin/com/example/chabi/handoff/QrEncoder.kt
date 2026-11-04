package com.example.chabi.handoff

fun interface QrEncoder {
    /** Main-safe. Implementations own CPU dispatching and preserve cancellation. */
    suspend fun encode(text: String): QrCode
}

/** Square module matrix including its quiet zone, independent of any encoding library. */
class QrCode(val size: Int, isDark: (x: Int, y: Int) -> Boolean) {
    init { require(size in 1..1024) }
    private val modules = BooleanArray(size * size) { isDark(it % size, it / size) }
    operator fun get(x: Int, y: Int): Boolean = modules[y * size + x]
}
