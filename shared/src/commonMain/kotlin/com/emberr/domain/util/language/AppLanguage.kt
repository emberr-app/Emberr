package com.emberr.domain.util.language

enum class AppLanguage(val languageTag: String, val nativeName: String?) {
    SYSTEM("", null),
    INDONESIAN("id", "Bahasa Indonesia"),
    GERMAN("de", "Deutsch"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    ITALIAN("it", "Italiano"),
    DUTCH("nl", "Nederlands"),
    POLISH("pl", "Polski"),
    PORTUGUESE_BRAZIL("pt-BR", "Português (Brasil)"),
    PORTUGUESE_PORTUGAL("pt-PT", "Português (Portugal)"),
    VIETNAMESE("vi", "Tiếng Việt"),
    TURKISH("tr", "Türkçe"),
    RUSSIAN("ru", "Русский"),
    UKRAINIAN("uk", "Українська"),
    HINDI("hi", "हिन्दी"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어"),
    CHINESE_SIMPLIFIED("zh-CN", "中文（简体）")
}

fun appLanguageForTag(languageTag: String): AppLanguage {
    if (languageTag.isBlank()) return AppLanguage.SYSTEM
    val chosenLanguages = AppLanguage.entries.filter { it != AppLanguage.SYSTEM }
    val exactMatch = chosenLanguages.firstOrNull { it.languageTag.equals(languageTag, ignoreCase = true) }
    if (exactMatch != null) return exactMatch
    val languageCode = languageTag.substringBefore('-').lowercase()
    return chosenLanguages.firstOrNull { it.languageTag.substringBefore('-') == languageCode } ?: AppLanguage.SYSTEM
}
