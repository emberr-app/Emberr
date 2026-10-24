package com.emberr.core.desktop

import com.emberr.core.security.secrets.PlaintextSecretBackend
import com.emberr.core.security.secrets.SecretNamespace
import com.emberr.data.local.prefs.DesktopPreferenceStore
import com.emberr.data.local.prefs.SyncConstants
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppDataEraserTest {

    private val workingDirectory = Files.createTempDirectory("emberr-app-data-eraser").toFile()
    private val emberrDirectory = File(workingDirectory, ".emberr")
    private val folderOutsideTheApp = File(workingDirectory, "my-synced-folder")

    @AfterTest
    fun deleteWorkingDirectory() {
        workingDirectory.deleteRecursively()
    }

    @Test
    fun withoutARequestNothingIsDeleted() {
        writeAppData()

        eraseAppDataIfRequested(emberrDirectory)

        assertTrue(File(emberrDirectory, "emberr_database.db").isFile)
        assertTrue(File(emberrDirectory, "media/photo.jpg").isFile)
    }

    @Test
    fun requestedEraseDeletesEverythingAndThenTheRequestItself() {
        writeAppData()
        assertTrue(requestAppDataEraseOnNextLaunch(emberrDirectory))

        eraseAppDataIfRequested(emberrDirectory)

        assertEquals(emptyList(), emberrDirectory.list().orEmpty().toList())
    }

    @Test
    fun theSingleInstanceLockFilesAreKept() {
        writeAppData()
        File(emberrDirectory, "instance.lock").writeText("")
        File(emberrDirectory, "instance.socket").writeText("")
        requestAppDataEraseOnNextLaunch(emberrDirectory)

        eraseAppDataIfRequested(emberrDirectory)

        assertEquals(setOf("instance.lock", "instance.socket"), emberrDirectory.list().orEmpty().toSet())
    }

    @Test
    fun aLinkedFolderIsUnlinkedButItsFilesAreKept() {
        writeAppData()
        File(folderOutsideTheApp, "note.md").apply { parentFile.mkdirs() }.writeText("keep me")
        Files.createSymbolicLink(File(emberrDirectory, "vault").toPath(), folderOutsideTheApp.toPath())
        requestAppDataEraseOnNextLaunch(emberrDirectory)

        eraseAppDataIfRequested(emberrDirectory)

        assertFalse(File(emberrDirectory, "vault").exists())
        assertEquals("keep me", File(folderOutsideTheApp, "note.md").readText())
    }

    @Test
    fun requestingAnEraseCreatesTheFolderWhenItIsMissing() {
        assertTrue(requestAppDataEraseOnNextLaunch(emberrDirectory))

        eraseAppDataIfRequested(emberrDirectory)

        assertEquals(emptyList(), emberrDirectory.list().orEmpty().toList())
    }

    @Test
    fun anEraseThatKeepsTheCloudAccountKeepsOnlyItsPlainTextSecretsTheServerAddressAndOnboarding() {
        writeAppData()
        val cloudService = SecretNamespace.EmberrCloud.keyringServiceName
        val selfHostService = SecretNamespace.SelfHostSync.keyringServiceName
        PlaintextSecretBackend(emberrDirectory).apply {
            writeSecret(cloudService, "session_token", "cloud-token")
            writeSecret(selfHostService, "server_password", "webdav-password")
        }
        DesktopPreferenceStore(emberrDirectory).apply {
            put(SyncConstants.KEY_CLOUD_SERVER_ADDRESS, "http://192.168.1.20:8080")
            put(SyncConstants.KEY_ACTIVE_SPACE_ID, "space-from-before")
            putBoolean(SyncConstants.KEY_ONBOARDING_COMPLETED, true)
        }
        assertTrue(requestAppDataEraseOnNextLaunch(emberrDirectory, keepCloudAccount = true))

        eraseAppDataIfRequested(emberrDirectory)

        assertEquals(setOf("settings.properties", "secrets.properties"), emberrDirectory.list().orEmpty().toSet())
        val secretsLeft = PlaintextSecretBackend(emberrDirectory).storedSecrets().map { it.service to it.secret }
        assertEquals(listOf(cloudService to "cloud-token"), secretsLeft)
        val settingsLeft = DesktopPreferenceStore(emberrDirectory)
        assertEquals(setOf(SyncConstants.KEY_CLOUD_SERVER_ADDRESS, SyncConstants.KEY_ONBOARDING_COMPLETED), settingsLeft.keys())
        assertEquals("http://192.168.1.20:8080", settingsLeft.get(SyncConstants.KEY_CLOUD_SERVER_ADDRESS, ""))
    }

    private fun writeAppData() {
        File(emberrDirectory, "media").mkdirs()
        File(emberrDirectory, "emberr_database.db").writeText("notes")
        File(emberrDirectory, "emberr_ai_index.db").writeText("index")
        File(emberrDirectory, "media/photo.jpg").writeText("pixels")
    }
}
