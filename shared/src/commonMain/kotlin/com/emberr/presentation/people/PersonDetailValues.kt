package com.emberr.presentation.people

import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.people.PersonBirthday
import com.emberr.domain.people.readPersonBirthday
import com.emberr.domain.people.toIsoText
import com.emberr.domain.util.system.deviceRegionCode
import com.emberr.presentation.shared.editor.links.toOpenableWebLink
import kotlinx.datetime.LocalDate

internal const val EARLIEST_BIRTH_YEAR = 1900
private const val YEAR_DIGITS = 4
private const val LEAP_YEAR_FOR_DAYS_IN_MONTH = 2000

val suggestedDetailKinds = listOf(PersonDetailKind.PHONE, PersonDetailKind.EMAIL, PersonDetailKind.BIRTHDAY)

fun labelChoices(kind: PersonDetailKind): List<String> = when (kind) {
    PersonDetailKind.PHONE -> listOf("Mobile", "Home", "Work")
    PersonDetailKind.EMAIL -> listOf("Personal", "Work")
    PersonDetailKind.ADDRESS -> listOf("Home", "Work")
    PersonDetailKind.WEBSITE -> listOf("Portfolio", "Blog")
    PersonDetailKind.SOCIAL -> listOf("Instagram", "X", "LinkedIn", "GitHub", "Mastodon")
    else -> emptyList()
}

fun valuePlaceholder(kind: PersonDetailKind): String = when (kind) {
    PersonDetailKind.PHONE -> phonePlaceholder(deviceRegionCode())
    PersonDetailKind.EMAIL -> "name@example.com"
    PersonDetailKind.BIRTHDAY -> ""
    PersonDetailKind.ADDRESS -> "Street, area, city"
    PersonDetailKind.COMPANY -> "Where they work"
    PersonDetailKind.ROLE -> "What they do"
    PersonDetailKind.WEBSITE -> "example.com"
    PersonDetailKind.SOCIAL -> "@handle"
    PersonDetailKind.RELATIONSHIP -> "Friend from college"
    PersonDetailKind.CUSTOM -> "Anything"
}

fun shownDetailValue(detail: PersonDetailBlock, today: LocalDate): String =
    if (detail.kind == PersonDetailKind.BIRTHDAY) {
        readPersonBirthday(detail.value)?.let { birthday -> birthdayText(birthday, today) } ?: detail.value
    } else {
        detail.value
    }

fun phoneCallLink(phone: String): String = "tel:${dialableNumber(phone)}"

fun phoneMessageLink(phone: String): String = "sms:${dialableNumber(phone)}"

fun emailLink(email: String): String = "mailto:${email.trim()}"

fun websiteLink(website: String): String = toOpenableWebLink(website.trim())

fun addressLink(address: String): String =
    "https://www.openstreetmap.org/search?query=${encodeForLink(address.trim())}"

fun socialLink(label: String?, handle: String): String? {
    val trimmed = handle.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    val name = trimmed.removePrefix("@")
    if (name.isEmpty()) return null
    return when (label?.trim()?.lowercase()) {
        "instagram" -> "https://instagram.com/$name"
        "x", "twitter" -> "https://x.com/$name"
        "linkedin" -> "https://www.linkedin.com/in/$name"
        "github" -> "https://github.com/$name"
        "mastodon" -> mastodonLink(name)
        else -> null
    }
}

fun openLinkFor(detail: PersonDetailBlock): String? = when (detail.kind) {
    PersonDetailKind.WEBSITE -> websiteLink(detail.value)
    PersonDetailKind.SOCIAL -> socialLink(detail.label, detail.value)
    PersonDetailKind.ADDRESS -> addressLink(detail.value)
    else -> null
}

private fun mastodonLink(userAtServer: String): String? {
    val parts = userAtServer.split("@")
    if (parts.size != 2 || parts.any { it.isEmpty() }) return null
    val (user, server) = parts
    return "https://$server/@$user"
}

private fun dialableNumber(phone: String): String = phone.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }

private fun encodeForLink(text: String): String = buildString {
    text.encodeToByteArray().forEach { byte ->
        val character = byte.toInt().toChar()
        when {
            byte >= 0 && (character.isLetterOrDigit() || character in "-_.~") -> append(character)
            character == ' ' -> append('+')
            else -> append('%').append((byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
        }
    }
}

data class BirthdayChoice(
    val month: Int? = null,
    val day: Int? = null,
    val yearText: String = ""
)

fun birthdayChoiceFrom(isoText: String): BirthdayChoice {
    val birthday = readPersonBirthday(isoText) ?: return BirthdayChoice()
    return BirthdayChoice(month = birthday.month, day = birthday.day, yearText = birthday.year?.toString().orEmpty())
}

fun daysInBirthdayMonth(month: Int?): Int {
    if (month == null) return 31
    val firstOfMonth = LocalDate(LEAP_YEAR_FOR_DAYS_IN_MONTH, month, 1)
    val firstOfNextMonth = if (month == 12) LocalDate(LEAP_YEAR_FOR_DAYS_IN_MONTH + 1, 1, 1) else LocalDate(LEAP_YEAR_FOR_DAYS_IN_MONTH, month + 1, 1)
    return (firstOfNextMonth.toEpochDays() - firstOfMonth.toEpochDays()).toInt()
}

fun BirthdayChoice.withMonth(newMonth: Int): BirthdayChoice =
    copy(month = newMonth, day = day?.coerceAtMost(daysInBirthdayMonth(newMonth)))

fun BirthdayChoice.isYearValid(thisYear: Int): Boolean {
    if (yearText.isBlank()) return true
    val year = yearText.trim().takeIf { it.length == YEAR_DIGITS }?.toIntOrNull() ?: return false
    if (year !in EARLIEST_BIRTH_YEAR..thisYear) return false
    val chosenMonth = month ?: return true
    val chosenDay = day ?: return true
    return runCatching { LocalDate(year, chosenMonth, chosenDay) }.isSuccess
}

fun BirthdayChoice.toBirthday(thisYear: Int): PersonBirthday? {
    val chosenMonth = month ?: return null
    val chosenDay = day ?: return null
    if (!isYearValid(thisYear)) return null
    return PersonBirthday(month = chosenMonth, day = chosenDay, year = yearText.trim().toIntOrNull())
}

fun BirthdayChoice.toIsoText(thisYear: Int): String? = toBirthday(thisYear)?.toIsoText()
