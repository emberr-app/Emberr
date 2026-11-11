package com.emberr.core.desktop

private const val JPACKAGE_RESTART_MARKER_VARIABLE = "_JPACKAGE_LAUNCHER"

fun ProcessBuilder.withoutLauncherRestartMarker(): ProcessBuilder = apply {
    environment().remove(JPACKAGE_RESTART_MARKER_VARIABLE)
}
