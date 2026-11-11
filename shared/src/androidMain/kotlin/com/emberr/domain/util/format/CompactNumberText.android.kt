package com.emberr.domain.util.format

import android.icu.math.BigDecimal
import android.icu.text.CompactDecimalFormat
import java.util.Locale

actual fun compactNumberText(number: Long, maximumFractionDigits: Int): String {
    val format = CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)
    format.setSignificantDigitsUsed(false)
    format.minimumFractionDigits = 0
    format.maximumFractionDigits = maximumFractionDigits
    format.roundingMode = BigDecimal.ROUND_HALF_UP
    return format.format(number)
}
