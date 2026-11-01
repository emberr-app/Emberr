package com.emberr.domain.people

import kotlinx.datetime.LocalDate

private const val YEAR_FOR_CHECKING_A_DAY_WITHOUT_YEAR = 2000
private val birthdayWithYear = Regex("""(\d{4})-(\d{2})-(\d{2})""")
private val birthdayWithoutYear = Regex("""--(\d{2})-(\d{2})""")

data class PersonBirthday(
    val month: Int,
    val day: Int,
    val year: Int?
)

fun readPersonBirthday(text: String): PersonBirthday? {
    val trimmed = text.trim()
    val birthday = birthdayWithYear.matchEntire(trimmed)?.destructured?.let { (year, month, day) ->
        PersonBirthday(month = month.toInt(), day = day.toInt(), year = year.toInt())
    } ?: birthdayWithoutYear.matchEntire(trimmed)?.destructured?.let { (month, day) ->
        PersonBirthday(month = month.toInt(), day = day.toInt(), year = null)
    } ?: return null
    val isRealDate = runCatching {
        LocalDate(birthday.year ?: YEAR_FOR_CHECKING_A_DAY_WITHOUT_YEAR, birthday.month, birthday.day)
    }.isSuccess
    return birthday.takeIf { isRealDate }
}
