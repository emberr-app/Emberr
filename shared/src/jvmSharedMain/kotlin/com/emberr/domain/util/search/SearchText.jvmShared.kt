package com.emberr.domain.util.search

import java.text.Normalizer

private val accentMarks = Regex("\\p{Mn}+")

actual fun removeAccentMarks(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(accentMarks, "")
