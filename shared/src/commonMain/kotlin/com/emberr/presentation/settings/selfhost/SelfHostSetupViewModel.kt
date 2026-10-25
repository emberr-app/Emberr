package com.emberr.presentation.settings.selfhost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.selfhost.sync.ForegroundSyncPoller
import com.emberr.domain.selfhost.crypto.IncorrectPassphraseException
import com.emberr.domain.selfhost.crypto.KeyDerivationManager
import com.emberr.domain.selfhost.crypto.SecureSyncKeyStorage
import com.emberr.domain.selfhost.crypto.VaultKeyLock
import com.emberr.domain.selfhost.webdav.SelfHostServerCredentials
import com.emberr.domain.selfhost.sync.SELF_HOST_UPDATE_REQUIRED_MESSAGE
import com.emberr.domain.selfhost.sync.SelfHostConnectionState
import com.emberr.domain.selfhost.sync.SelfHostSyncEngine
import com.emberr.domain.selfhost.sync.SelfHostSyncLog
import com.emberr.domain.selfhost.sync.SelfHostSyncNetwork
import com.emberr.domain.selfhost.sync.SelfHostSyncProgress
import com.emberr.domain.selfhost.sync.SelfHostSyncScheduler
import com.emberr.domain.selfhost.sync.SelfHostUpdateRequiredSignal
import com.emberr.domain.selfhost.webdav.WebDavConfigurationException
import com.emberr.domain.selfhost.webdav.WebDavConflictException
import com.emberr.domain.selfhost.webdav.WebDavConnectionTestResult
import com.emberr.domain.selfhost.webdav.WebDavSyncClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.emberr.domain.util.time.formatTimeOfDay

enum class ConnectionTestStatus { NOT_TESTED, TESTING, VERIFIED, FAILED }

enum class SetupPhase { FORM, FINALIZING, ERROR }

enum class ManualSyncStatus { IDLE, SYNCING, CANCELLING }

enum class VaultMode { UNKNOWN, CREATE_VAULT, RESTORE_VAULT }

data class SelfHostSetupFormState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val passphrase: String = "",
    val existingPassphraseInput: String = "",
    val hasAcknowledgedRisk: Boolean = false,
    val connectionTestStatus: ConnectionTestStatus = ConnectionTestStatus.NOT_TESTED,
    val connectionTestMessage: String? = null,
    val configurationWarningMessage: String? = null,
    val vaultMode: VaultMode = VaultMode.UNKNOWN,
    val setupPhase: SetupPhase = SetupPhase.FORM,
    val errorMessage: String? = null
) {
    val canTestConnection: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank() &&
            connectionTestStatus != ConnectionTestStatus.TESTING

    val canFinishSetup: Boolean
        get() {
            if (connectionTestStatus != ConnectionTestStatus.VERIFIED || setupPhase == SetupPhase.FINALIZING) return false
            return when (vaultMode) {
                VaultMode.CREATE_VAULT -> hasAcknowledgedRisk
                VaultMode.RESTORE_VAULT -> existingPassphraseInput.length == 16
                VaultMode.UNKNOWN -> false
            }
        }
}

data class SelfHostConnectedState(
    val serverUrl: String,
    val syncStatus: ManualSyncStatus = ManualSyncStatus.IDLE,
    val lastSyncedAtMillis: Long? = null,
    val isDisconnecting: Boolean = false,
    val syncError: String? = null,
    val syncNetwork: SelfHostSyncNetwork = SelfHostSyncNetwork.WIFI_AND_MOBILE_DATA,
    val noteSyncProgress: SelfHostSyncProgress? = null,
    val imageSyncProgress: SelfHostSyncProgress? = null,
    val hasSavedRecoveryPassphrase: Boolean = false,
    val shownRecoveryPassphrase: String? = null,
    val passphraseToSaveInput: String = "",
    val isSavingPassphrase: Boolean = false,
    val savePassphraseError: String? = null
) {
    val canSavePassphrase: Boolean
        get() = passphraseToSaveInput.length == 16 && !isSavingPassphrase
}

sealed class SelfHostScreenState {
    data object Checking : SelfHostScreenState()
    data class Unconfigured(val form: SelfHostSetupFormState) : SelfHostScreenState()
    data class Connected(val connectedState: SelfHostConnectedState) : SelfHostScreenState()
}

private const val WAIT_BEFORE_SHOWING_SYNC_FINISHED_MS = 1_000L

private const val WRONG_PASSPHRASE_MESSAGE = "That passphrase doesn't unlock this vault."

private fun waitingForNetworkMessage(syncNetwork: SelfHostSyncNetwork): String = when (syncNetwork) {
    SelfHostSyncNetwork.WIFI_ONLY -> "Sync is set to Wi-Fi only. It will run when you connect to Wi-Fi."
    SelfHostSyncNetwork.MOBILE_DATA_ONLY -> "Sync is set to mobile data only. It will run when you are off Wi-Fi."
    SelfHostSyncNetwork.WIFI_AND_MOBILE_DATA -> "Sync will run when you are back online."
}

fun formatLastSynced(epochMillis: Long?, usesTwentyFourHourClock: Boolean): String {
    if (epochMillis == null || epochMillis == 0L) return "Never synced"
    val local = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    val month = local.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "Last synced: $month ${local.day}, ${formatTimeOfDay(local.hour, local.minute, usesTwentyFourHourClock)}"
}

class SelfHostSetupViewModel(
    private val webDavSyncClient: WebDavSyncClient,
    private val secureSyncKeyStorage: SecureSyncKeyStorage,
    private val keyDerivationManager: KeyDerivationManager,
    private val vaultKeyLock: VaultKeyLock,
    private val selfHostSyncEngine: SelfHostSyncEngine,
    private val selfHostSyncScheduler: SelfHostSyncScheduler,
    private val settingsManager: SettingsManager,
    private val foregroundSyncPoller: ForegroundSyncPoller,
    private val selfHostConnectionState: SelfHostConnectionState
) : ViewModel() {

    private val _screenState = MutableStateFlow<SelfHostScreenState>(SelfHostScreenState.Checking)
    val screenState: StateFlow<SelfHostScreenState> = _screenState.asStateFlow()

    val notesWaitingToSyncCount: StateFlow<Int> = selfHostSyncEngine.notesWaitingToSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            refreshConnectionState()
        }
        viewModelScope.launch {
            combine(
                selfHostSyncScheduler.isSyncActive,
                selfHostSyncEngine.noteSyncProgress,
                selfHostSyncEngine.imageSyncProgress
            ) { isManualSyncActive, noteProgress, imageProgress ->
                isManualSyncActive || noteProgress != null || imageProgress != null
            }
                .debounce { isRunning -> if (isRunning) 0L else WAIT_BEFORE_SHOWING_SYNC_FINISHED_MS }
                .distinctUntilChanged()
                .collect { active ->
                    SelfHostSyncLog.d("ViewModel: isSyncActive changed to $active")
                    updateConnected {
                        it.copy(
                            syncStatus = if (active) ManualSyncStatus.SYNCING else ManualSyncStatus.IDLE,
                            lastSyncedAtMillis = if (active) {
                                it.lastSyncedAtMillis
                            } else {
                                settingsManager.getSelfHostLastSyncTimestamp().takeIf { ts -> ts > 0L }
                            }
                        )
                    }
                }
        }
        viewModelScope.launch {
            combine(selfHostSyncEngine.noteSyncProgress, selfHostSyncEngine.imageSyncProgress) { noteProgress, imageProgress ->
                noteProgress to imageProgress
            }.collect { (noteProgress, imageProgress) ->
                updateConnected { it.copy(noteSyncProgress = noteProgress, imageSyncProgress = imageProgress) }
            }
        }
        viewModelScope.launch {
            selfHostSyncScheduler.syncError.collect { error ->
                SelfHostSyncLog.d("ViewModel: syncError changed to $error")
                updateConnected { it.copy(syncError = error) }
            }
        }
        viewModelScope.launch {
            SelfHostUpdateRequiredSignal.isUpdateRequired.collect { isUpdateRequired ->
                if (isUpdateRequired) updateConnected { it.copy(syncError = SELF_HOST_UPDATE_REQUIRED_MESSAGE) }
            }
        }
    }

    private fun refreshConnectionState() {
        selfHostConnectionState.refresh()
        val credentials = secureSyncKeyStorage.getServerCredentials()
        val encryptionKey = secureSyncKeyStorage.getEncryptionKey()

        _screenState.value = if (credentials != null && encryptionKey != null) {
            SelfHostSyncLog.d("ViewModel: vault already configured, re-arming background sync schedules for this session")
            selfHostSyncScheduler.scheduleBackgroundSync()
            foregroundSyncPoller.start()

            SelfHostScreenState.Connected(
                SelfHostConnectedState(
                    serverUrl = credentials.serverUrl,
                    lastSyncedAtMillis = settingsManager.getSelfHostLastSyncTimestamp().takeIf { it > 0L },
                    syncNetwork = savedSyncNetwork(),
                    hasSavedRecoveryPassphrase = secureSyncKeyStorage.getRecoveryPassphrase() != null
                )
            )
        } else {
            SelfHostScreenState.Unconfigured(freshFormState())
        }
    }

    private fun savedSyncNetwork(): SelfHostSyncNetwork =
        SelfHostSyncNetwork.fromStoredName(settingsManager.getSelfHostSyncNetwork())

    private fun freshFormState(): SelfHostSetupFormState =
        SelfHostSetupFormState(passphrase = keyDerivationManager.generatePassphrase())

    private fun updateForm(transform: (SelfHostSetupFormState) -> SelfHostSetupFormState) {
        _screenState.update { current ->
            if (current is SelfHostScreenState.Unconfigured) current.copy(form = transform(current.form)) else current
        }
    }

    private fun updateConnected(transform: (SelfHostConnectedState) -> SelfHostConnectedState) {
        _screenState.update { current ->
            if (current is SelfHostScreenState.Connected) {
                current.copy(connectedState = transform(current.connectedState))
            } else {
                current
            }
        }
    }

    fun onServerUrlChanged(value: String) {
        updateForm {
            it.copy(
                serverUrl = value,
                connectionTestStatus = ConnectionTestStatus.NOT_TESTED,
                connectionTestMessage = null,
                configurationWarningMessage = null,
                errorMessage = null
            )
        }
    }

    fun onUsernameChanged(value: String) {
        updateForm {
            it.copy(
                username = value,
                connectionTestStatus = ConnectionTestStatus.NOT_TESTED,
                connectionTestMessage = null,
                configurationWarningMessage = null,
                errorMessage = null
            )
        }
    }

    fun onPasswordChanged(value: String) {
        updateForm {
            it.copy(
                password = value,
                connectionTestStatus = ConnectionTestStatus.NOT_TESTED,
                connectionTestMessage = null,
                configurationWarningMessage = null,
                errorMessage = null
            )
        }
    }

    fun onAcknowledgeRiskChanged(acknowledged: Boolean) {
        updateForm { it.copy(hasAcknowledgedRisk = acknowledged) }
    }

    fun dismissConfigurationWarning() {
        updateForm { it.copy(configurationWarningMessage = null) }
    }

    fun regeneratePassphrase() {
        updateForm { it.copy(passphrase = keyDerivationManager.generatePassphrase()) }
    }

    fun onExistingPassphraseChanged(value: String) {
        updateForm { it.copy(existingPassphraseInput = value, errorMessage = null) }
    }

    fun testConnection() {
        val form = (_screenState.value as? SelfHostScreenState.Unconfigured)?.form ?: return
        if (!form.canTestConnection) return

        viewModelScope.launch {
            updateForm {
                it.copy(
                    connectionTestStatus = ConnectionTestStatus.TESTING,
                    connectionTestMessage = null,
                    errorMessage = null,
                    vaultMode = VaultMode.UNKNOWN
                )
            }

            val credentials = SelfHostServerCredentials(
                serverUrl = form.serverUrl.trim(),
                username = form.username.trim(),
                password = form.password
            )
            val result = webDavSyncClient.testConnection(credentials)

            if (result !is WebDavConnectionTestResult.Success) {
                updateForm { current ->
                    when (result) {
                        is WebDavConnectionTestResult.InvalidCredentials -> current.copy(
                            connectionTestStatus = ConnectionTestStatus.FAILED,
                            connectionTestMessage = "Invalid username or password"
                        )

                        is WebDavConnectionTestResult.ServerError -> current.copy(
                            connectionTestStatus = ConnectionTestStatus.FAILED,
                            connectionTestMessage = "Server returned an error (${result.statusCode})"
                        )

                        is WebDavConnectionTestResult.NetworkFailure -> current.copy(
                            connectionTestStatus = ConnectionTestStatus.FAILED,
                            connectionTestMessage = result.cause.message ?: "Could not reach the server"
                        )

                        is WebDavConnectionTestResult.InvalidConfiguration -> current.copy(
                            connectionTestStatus = ConnectionTestStatus.FAILED,
                            configurationWarningMessage = result.message
                        )

                        WebDavConnectionTestResult.Success -> current
                    }
                }
                return@launch
            }

            val vaultExists = try {
                webDavSyncClient.checkVaultExists(credentials)
            } catch (cause: Exception) {
                updateForm {
                    it.copy(
                        connectionTestStatus = ConnectionTestStatus.FAILED,
                        connectionTestMessage = "Could not check for an existing vault: ${cause.message ?: "unknown error"}"
                    )
                }
                return@launch
            }

            updateForm {
                it.copy(
                    connectionTestStatus = ConnectionTestStatus.VERIFIED,
                    connectionTestMessage = if (vaultExists) {
                        "Existing vault found on this server"
                    } else {
                        "No existing vault, this will create a new one"
                    },
                    vaultMode = if (vaultExists) VaultMode.RESTORE_VAULT else VaultMode.CREATE_VAULT
                )
            }
        }
    }

    fun completeSetup() {
        val form = (_screenState.value as? SelfHostScreenState.Unconfigured)?.form ?: return
        if (!form.canFinishSetup) return

        viewModelScope.launch {
            updateForm { it.copy(setupPhase = SetupPhase.FINALIZING, errorMessage = null) }

            val credentials = SelfHostServerCredentials(
                serverUrl = form.serverUrl.trim(),
                username = form.username.trim(),
                password = form.password
            )

            try {
                secureSyncKeyStorage.saveServerCredentials(credentials)
                webDavSyncClient.ensureRemoteLayoutExists()

                val isRestoring = form.vaultMode == VaultMode.RESTORE_VAULT
                val passphrase = if (isRestoring) form.existingPassphraseInput else form.passphrase
                val passphraseChars = passphrase.toCharArray()
                val vaultKey = try {
                    if (isRestoring) {
                        val vaultFileBytes = webDavSyncClient.downloadVaultFile()
                            ?: throw WebDavConfigurationException("No vault file was found on the server to restore from")
                        withContext(Dispatchers.Default) { vaultKeyLock.unlockVault(vaultFileBytes, passphraseChars) }
                    } else {
                        if (webDavSyncClient.checkVaultExists(credentials)) {
                            throw WebDavConflictException(
                                "A vault already exists on this server. Restore it instead of creating a new one."
                            )
                        }
                        if (webDavSyncClient.checkManifestExists(credentials)) {
                            throw WebDavConfigurationException(
                                "This server folder has sync data from an older or damaged vault. " +
                                    "Delete the emberr_sync folder on your server, then set up again."
                            )
                        }
                        val newVault = withContext(Dispatchers.Default) { vaultKeyLock.createVault(passphraseChars) }
                        webDavSyncClient.uploadVaultFile(newVault.vaultFileBytes, failIfExists = true)
                        newVault.vaultKey
                    }
                } finally {
                    passphraseChars.fill(Char(0))
                }
                secureSyncKeyStorage.saveEncryptionKey(vaultKey)
                secureSyncKeyStorage.saveRecoveryPassphrase(passphrase)
                selfHostSyncEngine.forgetServerSyncProgress()
            } catch (cause: WebDavConflictException) {
                secureSyncKeyStorage.clearAll()
                updateForm {
                    it.copy(
                        setupPhase = SetupPhase.FORM,
                        vaultMode = VaultMode.RESTORE_VAULT,
                        connectionTestMessage = "Existing vault found on this server",
                        errorMessage = "A vault already exists on this server. Enter its recovery passphrase below to restore it."
                    )
                }
                return@launch
            } catch (cause: Exception) {
                secureSyncKeyStorage.clearAll()
                updateForm {
                    it.copy(
                        setupPhase = SetupPhase.ERROR,
                        errorMessage = cause.message ?: "Could not set up secure sync"
                    )
                }
                return@launch
            }

            selfHostConnectionState.refresh()
            selfHostSyncEngine.resumeAfterPause()

            _screenState.value = SelfHostScreenState.Connected(
                SelfHostConnectedState(
                    serverUrl = credentials.serverUrl,
                    syncNetwork = savedSyncNetwork(),
                    hasSavedRecoveryPassphrase = true
                )
            )
            syncNow()
            selfHostSyncScheduler.scheduleBackgroundSync()
            foregroundSyncPoller.start()
        }
    }

    fun syncNow() {
        SelfHostSyncLog.d("ViewModel: syncNow() invoked")
        if (_screenState.value !is SelfHostScreenState.Connected) return
        if (currentSyncStatus() != ManualSyncStatus.IDLE) {
            SelfHostSyncLog.d("ViewModel: syncNow() ignored, a sync is already running")
            return
        }
        if (!selfHostSyncEngine.isOnAllowedNetwork()) {
            updateConnected { it.copy(syncError = waitingForNetworkMessage(it.syncNetwork)) }
            return
        }
        selfHostSyncScheduler.syncNow()
    }

    fun cancelSync() {
        SelfHostSyncLog.d("ViewModel: cancelSync() invoked")
        if (currentSyncStatus() != ManualSyncStatus.SYNCING) return
        updateConnected { it.copy(syncStatus = ManualSyncStatus.CANCELLING) }
        selfHostSyncScheduler.cancelSync()
    }

    private fun currentSyncStatus(): ManualSyncStatus? =
        (_screenState.value as? SelfHostScreenState.Connected)?.connectedState?.syncStatus

    fun onSyncNetworkSelected(syncNetwork: SelfHostSyncNetwork) {
        settingsManager.saveSelfHostSyncNetwork(syncNetwork.name)
        updateConnected { it.copy(syncNetwork = syncNetwork, syncError = null) }
        selfHostSyncScheduler.scheduleBackgroundSync()
    }

    fun showRecoveryPassphrase() {
        val passphrase = secureSyncKeyStorage.getRecoveryPassphrase() ?: return
        updateConnected { it.copy(shownRecoveryPassphrase = passphrase) }
    }

    fun hideRecoveryPassphrase() {
        updateConnected { it.copy(shownRecoveryPassphrase = null) }
    }

    fun onPassphraseToSaveChanged(value: String) {
        updateConnected { it.copy(passphraseToSaveInput = value, savePassphraseError = null) }
    }

    fun savePassphraseOnThisDevice() {
        val connectedState = (_screenState.value as? SelfHostScreenState.Connected)?.connectedState ?: return
        if (!connectedState.canSavePassphrase) return
        val passphrase = connectedState.passphraseToSaveInput

        viewModelScope.launch {
            updateConnected { it.copy(isSavingPassphrase = true, savePassphraseError = null) }

            val errorMessage = try {
                val vaultFileBytes = webDavSyncClient.downloadVaultFile()
                if (vaultFileBytes == null) {
                    "No vault was found on the server."
                } else if (unlocksThisDevicesVault(vaultFileBytes, passphrase)) {
                    secureSyncKeyStorage.saveRecoveryPassphrase(passphrase)
                    null
                } else {
                    WRONG_PASSPHRASE_MESSAGE
                }
            } catch (cause: IncorrectPassphraseException) {
                WRONG_PASSPHRASE_MESSAGE
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                "Could not check the passphrase: ${cause.message ?: "unknown error"}"
            }

            updateConnected {
                if (errorMessage == null) {
                    it.copy(isSavingPassphrase = false, hasSavedRecoveryPassphrase = true, passphraseToSaveInput = "")
                } else {
                    it.copy(isSavingPassphrase = false, savePassphraseError = errorMessage)
                }
            }
        }
    }

    private suspend fun unlocksThisDevicesVault(vaultFileBytes: ByteArray, passphrase: String): Boolean {
        val savedVaultKey = secureSyncKeyStorage.getEncryptionKey() ?: return false
        val passphraseChars = passphrase.toCharArray()
        val unlockedVaultKey = try {
            withContext(Dispatchers.Default) { vaultKeyLock.unlockVault(vaultFileBytes, passphraseChars) }
        } finally {
            passphraseChars.fill(Char(0))
        }
        return unlockedVaultKey.contentEquals(savedVaultKey)
    }

    fun disconnectVault() {
        if (_screenState.value !is SelfHostScreenState.Connected) return

        viewModelScope.launch {
            updateConnected { it.copy(isDisconnecting = true) }

            foregroundSyncPoller.stop()
            try {
                selfHostSyncScheduler.cancelAll()
            } catch (cause: Exception) {
                SelfHostSyncLog.e("ViewModel: failed to cancel scheduled sync jobs during disconnect", cause)
            }

            secureSyncKeyStorage.clearAll()
            selfHostConnectionState.refresh()

            _screenState.value = SelfHostScreenState.Unconfigured(freshFormState())
        }
    }
}
