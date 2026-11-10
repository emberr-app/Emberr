package com.emberr.domain.util.language

import com.emberr.data.local.prefs.SettingsManager
import org.koin.mp.KoinPlatform
import java.util.Locale

actual val appLanguageChangeNeedsRestart: Boolean = true

actual fun chosenAppLanguage(): AppLanguage =
    appLanguageForTag(KoinPlatform.getKoin().get<SettingsManager>().getAppLanguageTag())

actual fun chooseAppLanguage(language: AppLanguage) {
    KoinPlatform.getKoin().get<SettingsManager>().saveAppLanguageTag(language.languageTag)
}

fun applySavedAppLanguage() {
    val languageTag = KoinPlatform.getKoin().get<SettingsManager>().getAppLanguageTag()
    if (languageTag.isEmpty()) return
    val systemFormatLocale = Locale.getDefault(Locale.Category.FORMAT)
    Locale.setDefault(Locale.forLanguageTag(languageTag))
    Locale.setDefault(Locale.Category.FORMAT, systemFormatLocale)
}
