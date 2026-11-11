package com.emberr.domain.util.format

expect fun compactNumberText(number: Long, maximumFractionDigits: Int): String

fun shortNumberText(number: Long): String {
    val wholeNumberText = compactNumberText(number, maximumFractionDigits = 0)
    return if (wholeNumberText.count { it.isDigit() } == 1) compactNumberText(number, maximumFractionDigits = 1) else wholeNumberText
}
