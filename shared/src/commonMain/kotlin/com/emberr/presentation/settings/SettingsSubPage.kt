package com.emberr.presentation.settings

internal const val DATA_CATEGORY_TITLE = "Data"
internal const val SYNC_CATEGORY_TITLE = "Sync"
internal const val HELP_CATEGORY_TITLE = "Help"

enum class SettingsSubPage(val parentPage: SettingsSubPage?) {
    SELF_HOST(parentPage = null),
    EMBERR_CLOUD(parentPage = null),
    IMPORT(parentPage = null),
    ABOUT(parentPage = null),
    COLOR_THEME(parentPage = null),
    OPEN_SOURCE_LICENSES(parentPage = ABOUT),
    THIRD_PARTY_SERVICES(parentPage = ABOUT)
}
