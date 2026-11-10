package com.emberr.presentation.shared.components

sealed interface PhrasePart {
    data class Words(val text: String) : PhrasePart
    data class Slot(val number: Int) : PhrasePart
}

private val slotPattern = Regex("%(\\d+)\\\$s")

fun phraseParts(phrase: String): List<PhrasePart> {
    val parts = mutableListOf<PhrasePart>()
    var wordsStart = 0
    slotPattern.findAll(phrase).forEach { match ->
        parts += PhrasePart.Words(phrase.substring(wordsStart, match.range.first).trim())
        parts += PhrasePart.Slot(match.groupValues[1].toInt())
        wordsStart = match.range.last + 1
    }
    parts += PhrasePart.Words(phrase.substring(wordsStart).trim())
    return parts.filterNot { it is PhrasePart.Words && it.text.isEmpty() }
}
