package com.emberr.domain.util.language

expect val appLanguageChangeNeedsRestart: Boolean

expect fun chosenAppLanguage(): AppLanguage

expect fun chooseAppLanguage(language: AppLanguage)
