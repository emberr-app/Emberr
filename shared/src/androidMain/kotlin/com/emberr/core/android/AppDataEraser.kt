package com.emberr.core.android

import android.content.Context
import androidx.core.content.edit
import com.emberr.core.security.AndroidSecretCipher
import com.emberr.data.local.prefs.SyncConstants
import com.emberr.di.SETTINGS_STORE_FILE_NAME
import com.emberr.domain.cloud.account.AndroidCloudAccountStorage
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

private const val ERASE_REQUEST_FILE_NAME = "erase-all-data-requested"
private const val SHARED_PREFERENCES_FOLDER_NAME = "shared_prefs"
private const val SHARED_PREFERENCES_FILE_EXTENSION = ".xml"
private val FOLDERS_THE_ERASE_LEAVES_ALONE = setOf("lib", "code_cache", SHARED_PREFERENCES_FOLDER_NAME)
private val SHARED_PREFERENCES_HOLDING_THE_CLOUD_ACCOUNT = setOf(
    AndroidCloudAccountStorage.SECRET_STORE_FILE_NAME,
    AndroidSecretCipher.KEYSET_STORAGE_FILE_NAME
)

fun requestAppDataEraseKeepingCloudAccountOnNextLaunch(context: Context): Boolean = runCatching {
    File(context.dataDir, ERASE_REQUEST_FILE_NAME).writeText("")
}.isSuccess

@OptIn(ExperimentalPathApi::class)
fun eraseAppDataKeepingCloudAccountIfRequested(context: Context) {
    val eraseRequestFile = File(context.dataDir, ERASE_REQUEST_FILE_NAME)
    if (!eraseRequestFile.isFile) return

    val settingsWereCleared = runCatching { clearSettingsExceptServerAddressAndOnboarding(context) }.isSuccess
    val otherSharedPreferencesWereDeleted = sharedPreferencesNames(context)
        .filter { name -> name != SETTINGS_STORE_FILE_NAME && name !in SHARED_PREFERENCES_HOLDING_THE_CLOUD_ACCOUNT }
        .map { name -> context.deleteSharedPreferences(name) }
        .all { wasDeleted -> wasDeleted }
    val foldersAndFilesToDelete = context.dataDir.listFiles().orEmpty()
        .filter { entry -> entry.name != ERASE_REQUEST_FILE_NAME && entry.name !in FOLDERS_THE_ERASE_LEAVES_ALONE } +
        context.externalCacheDirs.filterNotNull() +
        context.getExternalFilesDirs(null).filterNotNull()
    val everythingElseWasDeleted = foldersAndFilesToDelete
        .map { entry -> runCatching { entry.toPath().deleteRecursively() }.isSuccess }
        .all { wasDeleted -> wasDeleted }

    if (settingsWereCleared && otherSharedPreferencesWereDeleted && everythingElseWasDeleted) eraseRequestFile.delete()
}

private fun clearSettingsExceptServerAddressAndOnboarding(context: Context) {
    val settings = context.getSharedPreferences(SETTINGS_STORE_FILE_NAME, Context.MODE_PRIVATE)
    val cloudServerAddress = settings.getString(SyncConstants.KEY_CLOUD_SERVER_ADDRESS, null)
    val onboardingWasCompleted = settings.getBoolean(SyncConstants.KEY_ONBOARDING_COMPLETED, false)
    settings.edit(commit = true) {
        clear()
        if (cloudServerAddress != null) putString(SyncConstants.KEY_CLOUD_SERVER_ADDRESS, cloudServerAddress)
        if (onboardingWasCompleted) putBoolean(SyncConstants.KEY_ONBOARDING_COMPLETED, true)
    }
}

private fun sharedPreferencesNames(context: Context): List<String> =
    File(context.dataDir, SHARED_PREFERENCES_FOLDER_NAME).listFiles().orEmpty()
        .map { file -> file.name }
        .filter { fileName -> fileName.endsWith(SHARED_PREFERENCES_FILE_EXTENSION) }
        .map { fileName -> fileName.removeSuffix(SHARED_PREFERENCES_FILE_EXTENSION) }
