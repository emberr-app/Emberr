package com.emberr.presentation.desktop.window

import com.emberr.core.desktop.windowsPowerShellPathIn
import com.emberr.core.desktop.withoutLauncherRestartMarker
import com.emberr.domain.update.appimage.RunningAppImage
import com.emberr.domain.update.detectStagedUpdateInstallation
import com.emberr.domain.util.system.appVersionName
import com.emberr.domain.util.system.isWindowsDesktop
import java.io.File

object DesktopAppRelauncher {

    private const val SECONDS_TO_WAIT_FOR_THIS_PROCESS_TO_EXIT = 2
    private const val MAX_SECONDS_TO_WAIT_FOR_WINDOWS_APP_TO_EXIT = 60
    private const val WAIT_FOR_PROCESS_VARIABLE = "EMBERR_RESTART_WAIT_FOR_PROCESS"
    private const val EXECUTABLE_VARIABLE = "EMBERR_RESTART_EXECUTABLE"

    fun startNewInstance(): Boolean {
        val stagedUpdateInstallation = detectStagedUpdateInstallation()
        if (stagedUpdateInstallation != null && stagedUpdateInstallation.hasNewerStagedUpdate(appVersionName)) {
            return stagedUpdateInstallation.installStagedUpdateAfterThisProcessExits(launchAfterInstall = true)
        }

        val command = currentProcessCommand() ?: return false
        val relaunchProcess = relaunchProcessFor(command) ?: return false
        return runCatching {
            relaunchProcess
                .withoutLauncherRestartMarker()
                .directory(File(System.getProperty("user.dir")))
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            true
        }.getOrDefault(false)
    }

    private fun currentProcessCommand(): List<String>? {
        RunningAppImage.fileOrNull()?.let { appImageFile -> return listOf(appImageFile.absolutePath) }
        val processInfo = ProcessHandle.current().info()
        val executable = processInfo.command().orElse(null) ?: return null
        val arguments = processInfo.arguments().orElse(emptyArray()).toList()
        return listOf(executable) + arguments
    }

    private fun delayedCommand(command: List<String>): List<String> = listOf(
        "/bin/sh",
        "-c",
        "sleep $SECONDS_TO_WAIT_FOR_THIS_PROCESS_TO_EXIT; exec \"$@\"",
        "emberr-restart"
    ) + command

    private fun relaunchProcessFor(command: List<String>): ProcessBuilder? {
        if (!isWindowsDesktop) return ProcessBuilder(delayedCommand(command))
        val executablePath = command.first()
        return windowsRelaunchProcess(executablePath)
    }

    private fun windowsRelaunchProcess(executablePath: String): ProcessBuilder? {
        val windowsDirectory = System.getenv("SystemRoot")?.let(::File) ?: return null
        return ProcessBuilder(
            windowsPowerShellPathIn(windowsDirectory),
            "-NoProfile",
            "-NonInteractive",
            "-WindowStyle", "Hidden",
            "-Command",
            "Wait-Process -Id \$env:$WAIT_FOR_PROCESS_VARIABLE -Timeout $MAX_SECONDS_TO_WAIT_FOR_WINDOWS_APP_TO_EXIT" +
                " -ErrorAction SilentlyContinue; Start-Process -FilePath \$env:$EXECUTABLE_VARIABLE"
        ).apply {
            environment()[WAIT_FOR_PROCESS_VARIABLE] = ProcessHandle.current().pid().toString()
            environment()[EXECUTABLE_VARIABLE] = executablePath
        }
    }
}
