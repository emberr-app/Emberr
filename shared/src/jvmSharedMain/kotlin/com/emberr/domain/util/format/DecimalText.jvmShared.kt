package com.emberr.domain.util.format

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

actual fun decimalText(number: Double, minimumFractionDigits: Int, maximumFractionDigits: Int): String {
    val format = NumberFormat.getNumberInstance(Locale.getDefault())
    format.isGroupingUsed = false
    format.minimumFractionDigits = minimumFractionDigits
    format.maximumFractionDigits = maximumFractionDigits
    format.roundingMode = RoundingMode.HALF_UP
    return format.format(number)
}
