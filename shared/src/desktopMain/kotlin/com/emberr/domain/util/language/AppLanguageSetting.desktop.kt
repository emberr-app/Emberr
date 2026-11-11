package com.emberr.domain.util.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.emberr.data.local.prefs.SettingsManager
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import java.util.Locale

actual val appLanguageChangeNeedsRestart: Boolean = false

private val systemLanguageLocale: Locale = Locale.getDefault()

var appliedAppLanguageTag: String by mutableStateOf("")
    private set

actual fun chosenAppLanguage(): AppLanguage =
    appLanguageForTag(KoinPlatform.getKoin().get<SettingsManager>().getAppLanguageTag())

actual fun chooseAppLanguage(language: AppLanguage) {
    KoinPlatform.getKoin().get<SettingsManager>().saveAppLanguageTag(language.languageTag)
    applyAppLanguageTag(language.languageTag)
}

fun applySavedAppLanguage() {
    applyAppLanguageTag(KoinPlatform.getKoin().get<SettingsManager>().getAppLanguageTag())
}

private fun applyAppLanguageTag(languageTag: String) {
    val systemFormatLocale = Locale.getDefault(Locale.Category.FORMAT)
    Locale.setDefault(if (languageTag.isEmpty()) systemLanguageLocale else Locale.forLanguageTag(languageTag))
    Locale.setDefault(Locale.Category.FORMAT, systemFormatLocale)
    appliedAppLanguageTag = languageTag
}

@Composable
fun stringInAppLanguage(resource: StringResource): String =
    key(appliedAppLanguageTag) { stringResource(resource) }
