package com.emberr.domain.util.language

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.prefs.SyncConstants
import com.emberr.di.SETTINGS_STORE_FILE_NAME
import org.koin.mp.KoinPlatform
import java.util.Locale

actual val appLanguageChangeNeedsRestart: Boolean
    get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

actual fun chosenAppLanguage(): AppLanguage {
    val koin = KoinPlatform.getKoin()
    val languageTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        koin.get<Context>().getSystemService(LocaleManager::class.java)
            ?.applicationLocales?.get(0)?.toLanguageTag().orEmpty()
    } else {
        koin.get<SettingsManager>().getAppLanguageTag()
    }
    return appLanguageForTag(languageTag)
}

actual fun chooseAppLanguage(language: AppLanguage) {
    val koin = KoinPlatform.getKoin()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        koin.get<Context>().getSystemService(LocaleManager::class.java)
            ?.applicationLocales = LocaleList.forLanguageTags(language.languageTag)
    } else {
        koin.get<SettingsManager>().saveAppLanguageTag(language.languageTag)
    }
}

fun applySavedAppLanguage(application: Application) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
    val languageTag = application.getSharedPreferences(SETTINGS_STORE_FILE_NAME, Context.MODE_PRIVATE)
        .getString(SyncConstants.KEY_APP_LANGUAGE_TAG, "").orEmpty()
    if (languageTag.isEmpty()) return

    val locale = Locale.forLanguageTag(languageTag)
    Locale.setDefault(locale)
    val resources = application.resources
    val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
    @Suppress("DEPRECATION")
    resources.updateConfiguration(configuration, resources.displayMetrics)
}
