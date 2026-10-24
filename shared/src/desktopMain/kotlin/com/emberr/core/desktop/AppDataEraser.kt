package com.emberr.core.desktop

import com.emberr.core.security.secrets.PlaintextSecretBackend
import com.emberr.core.security.secrets.SecretNamespace
import com.emberr.data.local.prefs.DesktopPreferenceStore
import com.emberr.data.local.prefs.SyncConstants
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

private const val ERASE_REQUEST_FILE_NAME = "erase-all-data-requested"
private const val KEEP_CLOUD_ACCOUNT_REQUEST = "keep-cloud-account"
private val FILES_THE_RUNNING_APP_HOLDS_OPEN = setOf("instance.lock", "instance.socket")
private val FILES_HOLDING_THE_CLOUD_ACCOUNT = setOf("settings.properties", "secrets.properties")
private val SETTINGS_KEPT_WITH_THE_CLOUD_ACCOUNT = setOf(
    SyncConstants.KEY_CLOUD_SERVER_ADDRESS,
    SyncConstants.KEY_ONBOARDING_COMPLETED
)

fun requestAppDataEraseOnNextLaunch(emberrDirectory: File, keepCloudAccount: Boolean = false): Boolean = runCatching {
    emberrDirectory.mkdirs()
    File(emberrDirectory, ERASE_REQUEST_FILE_NAME).writeText(if (keepCloudAccount) KEEP_CLOUD_ACCOUNT_REQUEST else "")
}.isSuccess

@OptIn(ExperimentalPathApi::class)
fun eraseAppDataIfRequested(emberrDirectory: File) {
    val eraseRequestFile = File(emberrDirectory, ERASE_REQUEST_FILE_NAME)
    if (!eraseRequestFile.isFile) return
    val eraseRequest = runCatching { eraseRequestFile.readText() }.getOrNull() ?: return
    val keepsCloudAccount = eraseRequest == KEEP_CLOUD_ACCOUNT_REQUEST
    val filesToKeep = if (keepsCloudAccount) FILES_HOLDING_THE_CLOUD_ACCOUNT else emptySet()

    val everythingWasDeleted = emberrDirectory.listFiles().orEmpty()
        .filter { entry ->
            entry.name != ERASE_REQUEST_FILE_NAME &&
                entry.name !in FILES_THE_RUNNING_APP_HOLDS_OPEN &&
                entry.name !in filesToKeep
        }
        .map { entry -> runCatching { entry.toPath().deleteRecursively() }.isSuccess }
        .all { wasDeleted -> wasDeleted }
    val onlyTheCloudAccountIsLeft = !keepsCloudAccount ||
        runCatching { removeEverythingButTheCloudAccount(emberrDirectory) }.isSuccess

    if (everythingWasDeleted && onlyTheCloudAccountIsLeft) eraseRequestFile.delete()
}

private fun removeEverythingButTheCloudAccount(emberrDirectory: File) {
    val settings = DesktopPreferenceStore(emberrDirectory)
    settings.keys()
        .filter { key -> key !in SETTINGS_KEPT_WITH_THE_CLOUD_ACCOUNT }
        .forEach { key -> settings.remove(key) }

    val plainTextSecrets = PlaintextSecretBackend(emberrDirectory)
    val cloudServiceName = SecretNamespace.EmberrCloud.keyringServiceName
    plainTextSecrets.storedSecrets()
        .filter { storedSecret -> storedSecret.service != cloudServiceName }
        .forEach { storedSecret -> plainTextSecrets.removeSecret(storedSecret.service, storedSecret.account) }
}
