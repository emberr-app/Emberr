package com.emberr.domain.util.format

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

actual fun compactNumberText(number: Long, maximumFractionDigits: Int): String {
    val format = NumberFormat.getCompactNumberInstance(Locale.getDefault(), NumberFormat.Style.SHORT)
    format.minimumFractionDigits = 0
    format.maximumFractionDigits = maximumFractionDigits
    format.roundingMode = RoundingMode.HALF_UP
    return format.format(number)
}
