package com.emberr.domain.people

import com.emberr.domain.model.PersonDetailKind
import kotlinx.datetime.LocalDate

data class BirthdayOnDate(
    val personNoteId: String,
    val personName: String,
    val date: LocalDate,
    val turningAge: Int?
)

fun birthdaysInRange(people: List<PersonWithDetails>, firstDate: LocalDate, lastDate: LocalDate): List<BirthdayOnDate> {
    if (lastDate < firstDate) return emptyList()
    return people
        .filter { person -> person.person.trashedAt == null }
        .flatMap { person ->
            val birthday = person.details
                .filter { detail -> !detail.isDeleted && detail.kind == PersonDetailKind.BIRTHDAY }
                .firstNotNullOfOrNull { detail -> readPersonBirthday(detail.value) }
                ?: return@flatMap emptyList()
            (firstDate.year..lastDate.year).mapNotNull { year ->
                val birthYear = birthday.year
                if (birthYear != null && year < birthYear) return@mapNotNull null
                val date = birthday.dateInYear(year)
                if (date < firstDate || date > lastDate) return@mapNotNull null
                BirthdayOnDate(
                    personNoteId = person.person.noteId,
                    personName = person.person.title,
                    date = date,
                    turningAge = birthYear?.let { year - it }?.takeIf { it > 0 }
                )
            }
        }
}

private fun PersonBirthday.dateInYear(year: Int): LocalDate {
    val isLeapDayInCommonYear = month == 2 && day == 29 && !isLeapYear(year)
    return LocalDate(year, month, if (isLeapDayInCommonYear) 28 else day)
}

private fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
