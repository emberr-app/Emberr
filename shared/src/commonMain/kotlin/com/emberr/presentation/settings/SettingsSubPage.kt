package com.emberr.presentation.settings

internal const val DATA_CATEGORY_TITLE = "Data"
internal const val SYNC_CATEGORY_TITLE = "Sync"
internal const val HELP_CATEGORY_TITLE = "Help"

enum class SettingsSubPage(val categoryTitle: String, val parentPage: SettingsSubPage?) {
    SELF_HOST(categoryTitle = SYNC_CATEGORY_TITLE, parentPage = null),
    IMPORT(categoryTitle = DATA_CATEGORY_TITLE, parentPage = null),
    ABOUT(categoryTitle = HELP_CATEGORY_TITLE, parentPage = null),
    OPEN_SOURCE_LICENSES(categoryTitle = HELP_CATEGORY_TITLE, parentPage = ABOUT)
}
