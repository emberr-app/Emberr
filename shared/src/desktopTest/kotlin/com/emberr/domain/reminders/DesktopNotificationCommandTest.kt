package com.emberr.domain.reminders

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopNotificationCommandTest {

    private val trickyTitle = "Zoltan said \"hi\" \\ 'ok' 🎉"
    private val trickyMessage = "It's \"Quincy's\" birthday \\o/ 🎂"

    @Test
    fun quotesBackslashesAndEmojiNeverBecomePartOfTheScriptText() {
        val macCommand = macNotificationCommand(trickyTitle, trickyMessage)
        assertEquals(listOf(trickyTitle, trickyMessage), macCommand.takeLast(2))
        assertFalse(macCommand.dropLast(2).any { part -> "Zoltan" in part || "Quincy" in part })

        val windowsProcess = windowsNotificationProcess(trickyTitle, trickyMessage, iconUri = "file:///C:/icon.png")
        val encodedScript = windowsProcess.command().last()
        val decodedScript = String(Base64.getDecoder().decode(encodedScript), Charsets.UTF_16LE)
        assertEquals("-EncodedCommand", windowsProcess.command()[windowsProcess.command().size - 2])
        assertFalse("Zoltan" in decodedScript || "Quincy" in decodedScript)
        assertEquals(trickyTitle, windowsProcess.environment()["EMBERR_NOTIFICATION_TITLE"])
        assertEquals(trickyMessage, windowsProcess.environment()["EMBERR_NOTIFICATION_MESSAGE"])

        val scriptWithEmoji = "Write-Output '🎉 \"quoted\" \\'"
        assertEquals(scriptWithEmoji, String(Base64.getDecoder().decode(powerShellEncodedCommand(scriptWithEmoji)), Charsets.UTF_16LE))
    }
}
