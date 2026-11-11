package com.emberr.domain.util.search

expect fun removeAccentMarks(text: String): String

private val latinLettersThatDoNotSplitIntoAccents = mapOf(
    'ł' to 'l', 'Ł' to 'L',
    'ø' to 'o', 'Ø' to 'O',
    'đ' to 'd', 'Đ' to 'D',
    'ı' to 'i'
)

private val latinLetterWithoutAccent: Map<Char, Char> by lazy {
    (('À'..'ɏ') + ('Ḁ'..'ỿ'))
        .associateWith { letter -> removeAccentMarks(letter.toString()).singleOrNull() ?: letter } +
        latinLettersThatDoNotSplitIntoAccents
}

private val latinLettersBySearchLetter: Map<Char, Set<Char>> by lazy {
    (('A'..'Z') + ('a'..'z') + latinLetterWithoutAccent.keys)
        .groupBy(::searchLetterOf)
        .mapValues { (_, letters) -> letters.toSet() }
}

fun searchLetterOf(char: Char): Char = (latinLetterWithoutAccent[char] ?: char).lowercaseChar()

fun searchFormOf(text: String): String = buildString(text.length) { text.forEach { append(searchLetterOf(it)) } }

fun String.containsForSearch(query: String): Boolean = searchFormOf(this).contains(searchFormOf(query))

fun searchGlobPattern(query: String): String = buildString {
    append('*')
    for (char in query) {
        val matchingLetters = latinLettersBySearchLetter[searchLetterOf(char)].orEmpty() +
            setOf(char, char.lowercaseChar(), char.uppercaseChar())
        when {
            matchingLetters.size > 1 -> append('[').append(matchingLetters.joinToString("")).append(']')
            char == '*' || char == '?' || char == '[' -> append('[').append(char).append(']')
            else -> append(char)
        }
    }
    append('*')
}
