package com.emberr.domain.credentials

import kotlinx.serialization.Serializable

const val SHORTEST_GENERATED_PASSWORD = 8
const val LONGEST_GENERATED_PASSWORD = 64
const val MOST_REQUIRED_CHARACTERS_OF_ONE_KIND = 9

private const val UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz"
private const val NUMBERS = "0123456789"
private const val SYMBOLS = "!@#\$%^&*-_+=?"
private const val AMBIGUOUS_CHARACTERS = "IOl01"

@Serializable
data class PasswordGeneratorOptions(
    val length: Int = 18,
    val usesUppercase: Boolean = true,
    val usesLowercase: Boolean = true,
    val usesNumbers: Boolean = true,
    val usesSymbols: Boolean = true,
    val minimumNumbers: Int = 1,
    val minimumSymbols: Int = 1,
    val avoidsAmbiguousCharacters: Boolean = true
) {
    val enabledCharacterSetCount: Int
        get() = listOf(usesUppercase, usesLowercase, usesNumbers, usesSymbols).count { it }

    val shortestAllowedLength: Int
        get() {
            val requiredCount = (if (usesUppercase) 1 else 0) +
                (if (usesLowercase) 1 else 0) +
                (if (usesNumbers) minimumNumbers else 0) +
                (if (usesSymbols) minimumSymbols else 0)
            return maxOf(SHORTEST_GENERATED_PASSWORD, requiredCount)
        }

    val shownLength: Int
        get() = length.coerceIn(shortestAllowedLength, LONGEST_GENERATED_PASSWORD)
}

expect fun secureRandomIndex(bound: Int): Int

fun generatePassword(
    options: PasswordGeneratorOptions,
    randomIndex: (bound: Int) -> Int = ::secureRandomIndex
): String {
    fun characters(all: String): String =
        if (options.avoidsAmbiguousCharacters) all.filterNot { it in AMBIGUOUS_CHARACTERS } else all

    val uppercase = characters(UPPERCASE_LETTERS).takeIf { options.usesUppercase }
    val lowercase = characters(LOWERCASE_LETTERS).takeIf { options.usesLowercase }
    val numbers = characters(NUMBERS).takeIf { options.usesNumbers }
    val symbols = SYMBOLS.takeIf { options.usesSymbols }
    val allAllowed = listOfNotNull(uppercase, lowercase, numbers, symbols).joinToString("").ifEmpty { characters(LOWERCASE_LETTERS) }

    fun pick(from: String): Char = from[randomIndex(from.length)]

    val passwordCharacters = buildList {
        uppercase?.let { letters -> add(pick(letters)) }
        lowercase?.let { letters -> add(pick(letters)) }
        numbers?.let { digits -> repeat(options.minimumNumbers) { add(pick(digits)) } }
        symbols?.let { marks -> repeat(options.minimumSymbols) { add(pick(marks)) } }
        while (size < options.shownLength) add(pick(allAllowed))
    }.toMutableList()

    for (index in passwordCharacters.lastIndex downTo 1) {
        val swapIndex = randomIndex(index + 1)
        val character = passwordCharacters[index]
        passwordCharacters[index] = passwordCharacters[swapIndex]
        passwordCharacters[swapIndex] = character
    }
    return passwordCharacters.joinToString("")
}
