package com.example.chabi.format

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
