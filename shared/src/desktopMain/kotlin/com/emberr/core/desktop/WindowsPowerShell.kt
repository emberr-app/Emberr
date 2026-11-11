package com.emberr.core.desktop

import java.io.File

fun windowsPowerShellPathIn(windowsDirectory: File): String =
    windowsDirectory.resolve("System32/WindowsPowerShell/v1.0/powershell.exe").path
