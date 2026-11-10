package com.emberr.domain.util.language

import com.emberr.domain.util.time.startingWithCapital
import java.util.Locale

actual fun languageNameInCurrentLanguage(languageTag: String): String =
    Locale.forLanguageTag(languageTag).getDisplayName(Locale.getDefault()).startingWithCapital()
