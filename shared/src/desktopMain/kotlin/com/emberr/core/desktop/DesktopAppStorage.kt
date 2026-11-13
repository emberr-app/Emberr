package com.emberr.core.desktop

import java.io.File

object DesktopAppStorage {

    const val DEBUG_BUILD_PROPERTY = "emberr.debugBuild"
    private const val SETTINGS_FILE_NAME = "settings.properties"
    private const val DATABASE_FILE_NAME = "emberr_database.db"

    val isDebugBuild: Boolean = System.getProperty(DEBUG_BUILD_PROPERTY).toBoolean()

    val emberrDirectory: File = emberrDirectoryFor(File(System.getProperty("user.home")), isDebugBuild)

    val mediaDirectory: File
        get() = File(emberrDirectory, "media")

    fun holdsNoAppData(emberrDirectory: File): Boolean =
        !File(emberrDirectory, SETTINGS_FILE_NAME).exists() && !File(emberrDirectory, DATABASE_FILE_NAME).exists()

    fun emberrDirectoryFor(homeDirectory: File, isDebugBuild: Boolean): File =
        File(homeDirectory, if (isDebugBuild) ".emberr_debug" else ".emberr")
}
