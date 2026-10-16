package com.emberr.domain.util.time

import android.icu.util.Calendar
import kotlinx.datetime.DayOfWeek
import java.util.Locale

fun androidSystemFirstDayOfWeek(): DayOfWeek {
    val sundayBasedDayNumber = Calendar.getInstance(Locale.getDefault(Locale.Category.FORMAT)).firstDayOfWeek
    return DayOfWeek.entries[(sundayBasedDayNumber + 5) % 7]
}
