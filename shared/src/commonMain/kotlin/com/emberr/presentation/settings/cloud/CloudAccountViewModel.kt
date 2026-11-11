package com.emberr.presentation.settings.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.cloud.account.CloudAccountRepository
import com.emberr.domain.cloud.account.CloudPasswordStrength
import com.emberr.domain.cloud.account.CloudPendingRecovery
import com.emberr.domain.cloud.account.CloudSignInState
import com.emberr.domain.cloud.account.estimateCloudPasswordStrength
import com.emberr.domain.cloud.api.CloudAccountDetails
import com.emberr.domain.cloud.api.CloudApiErrorCode
import com.emberr.domain.cloud.api.CloudApiException
import com.emberr.domain.cloud.api.CloudServerUnreachableException
import com.emberr.domain.cloud.api.CloudSession
import com.emberr.domain.cloud.api.normalizedCloudServerAddress
import com.emberr.domain.cloud.keys.IncorrectCloudPasswordException
import com.emberr.domain.cloud.keys.IncorrectRecoveryKeyException
import com.emberr.domain.cloud.keys.InvalidRecoveryWordsException
import com.emberr.domain.cloud.sync.CloudMediaPassDetails
import com.emberr.domain.cloud.sync.CloudSyncEngine
import com.emberr.domain.cloud.sync.CloudSyncResult
import com.emberr.domain.cloud.sync.CloudSyncScheduler
import com.emberr.domain.cloud.sync.CloudSyncStatus
import com.emberr.domain.selfhost.sync.SelfHostConnectionState
import com.emberr.domain.selfhost.sync.SelfHostSyncNetwork
import com.emberr.domain.selfhost.sync.SelfHostSyncProgress
import com.emberr.domain.sync.SyncNetworkRule
import com.emberr.domain.sync.SyncPairingState
import com.emberr.domain.sync.waitingForNetworkPhrase
import com.emberr.domain.util.system.eraseAllAppData
import com.emberr.domain.util.system.showNativeToast
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.cloud_account_deleted_notes_not_cleared
import emberr.shared.generated.resources.cloud_disconnect_self_host_first
import emberr.shared.generated.resources.cloud_email_changed
import emberr.shared.generated.resources.cloud_error_device_already_signed_out
import emberr.shared.generated.resources.cloud_error_email_already_used
import emberr.shared.generated.resources.cloud_error_expired_code
import emberr.shared.generated.resources.cloud_error_incorrect_code
import emberr.shared.generated.resources.cloud_error_incorrect_password
import emberr.shared.generated.resources.cloud_error_invalid_email
import emberr.shared.generated.resources.cloud_error_invalid_recovery_words
import emberr.shared.generated.resources.cloud_error_recovery_words_other_account
import emberr.shared.generated.resources.cloud_error_server_unreachable
import emberr.shared.generated.resources.cloud_error_signed_out
import emberr.shared.generated.resources.cloud_error_something_went_wrong
import emberr.shared.generated.resources.cloud_error_too_many_tries
import emberr.shared.generated.resources.cloud_error_update_required
import emberr.shared.generated.resources.cloud_error_wrong_email_or_password
import emberr.shared.generated.resources.cloud_new_code_sent
import emberr.shared.generated.resources.cloud_password_changed
import emberr.shared.generated.resources.cloud_server_address_invalid
import emberr.shared.generated.resources.cloud_server_address_saved
import emberr.shared.generated.resources.cloud_sync_busy
import emberr.shared.generated.resources.cloud_sync_other_method_on
import emberr.shared.generated.resources.cloud_sync_update_required
import emberr.shared.generated.resources.cloud_synced
import emberr.shared.generated.resources.cloud_synced_with_not_sent
import emberr.shared.generated.resources.cloud_unpair_lan_sync_first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

enum class CloudAccountStep {
    LOADING,
    WELCOME,
    SIGN_UP_EMAIL,
    SIGN_UP_DETAILS,
    SIGN_UP_RECOVERY_WORDS,
    LOG_IN,
    RECOVERY_EMAIL,
    RECOVERY_WORDS,
    RECOVERY_NEW_PASSWORD,
    SIGNED_IN,
    CHANGE_PASSWORD,
    CHANGE_PASSWORD_CODE,
    CHANGE_EMAIL,
    CHANGE_EMAIL_CODE,
    RECOVERY_WORDS_PASSWORD,
    SHOW_RECOVERY_WORDS,
    DELETE_ACCOUNT
}

data class CloudAccountScreenState(
    val step: CloudAccountStep = CloudAccountStep.LOADING,
    val serverAddressInput: String = "",
    val serverAddressError: StringResource? = null,
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val passwordAgain: String = "",
    val passwordStrength: CloudPasswordStrength = CloudPasswordStrength.WEAK,
    val passwordStrengthCheckedFor: String = "",
    val recoveryWordsInput: String = "",
    val newRecoveryWords: String = "",
    val hasSavedRecoveryWords: Boolean = false,
    val signedInEmail: String = "",
    val accountDetails: CloudAccountDetails? = null,
    val accountDetailsError: String? = null,
    val sessions: List<CloudSession>? = null,
    val sessionsError: String? = null,
    val logOutOtherDevices: Boolean = true,
    val recoveryWordsToShow: String = "",
    val clearNotesOnThisDevice: Boolean = false,
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val newCodeSentMessage: String? = null,
    val syncStatus: CloudSyncStatus = CloudSyncStatus(),
    val syncNetwork: SelfHostSyncNetwork = SelfHostSyncNetwork.WIFI_AND_MOBILE_DATA,
    val mediaSyncProgress: SelfHostSyncProgress? = null,
    val mediaPassDetails: CloudMediaPassDetails? = null,
    val otherSyncMethodMessage: String? = null
) {
    val syncProblemMessage: CloudText?
        get() = syncStatus.lastProblem?.let { problem -> cloudSyncResultMessage(problem, syncNetwork, accountDetails) }

    val planText: CloudText?
        get() = accountDetails?.subscription?.let { subscription ->
            cloudPlanText(subscription, Clock.System.now(), TimeZone.currentSystemDefault())
        }

    val canStartSignIn: Boolean
        get() = !isWorking && otherSyncMethodMessage == null

    val isServerAddressShown: Boolean
        get() = step !in setOf(CloudAccountStep.LOADING, CloudAccountStep.SIGN_UP_RECOVERY_WORDS) && step !in signedInSteps

    val canGoBack: Boolean
        get() = previousStepOf(step) != null

    val canSendCode: Boolean
        get() = email.isNotBlank() && !isWorking

    val isPasswordStrengthKnown: Boolean
        get() = password.isNotEmpty() && passwordStrengthCheckedFor == password

    val doPasswordsMatch: Boolean
        get() = password.isNotEmpty() && password == passwordAgain

    val canCreateAccount: Boolean
        get() = code.length == EMAIL_CODE_LENGTH && isNewPasswordAccepted && !isWorking

    val canLogIn: Boolean
        get() = email.isNotBlank() && password.isNotEmpty() && !isWorking

    val canCheckRecoveryWords: Boolean
        get() = code.length == EMAIL_CODE_LENGTH && recoveryWordsInput.isNotBlank() && !isWorking

    val canSetNewPassword: Boolean
        get() = isNewPasswordAccepted && !isWorking

    val canFinishSignUp: Boolean
        get() = hasSavedRecoveryWords && !isWorking

    val canChangePassword: Boolean
        get() = code.length == EMAIL_CODE_LENGTH && isNewPasswordAccepted && !isWorking

    val canChangeEmail: Boolean
        get() = code.length == EMAIL_CODE_LENGTH && !isWorking

    val canCheckPassword: Boolean
        get() = password.isNotEmpty() && !isWorking

    val canDeleteAccount: Boolean
        get() = code.length == EMAIL_CODE_LENGTH && password.isNotEmpty() && !isWorking

    private val isNewPasswordAccepted: Boolean
        get() = doPasswordsMatch && isPasswordStrengthKnown && passwordStrength != CloudPasswordStrength.WEAK
}

private const val EMAIL_CODE_LENGTH = 6

private val stepsThatPickANewPassword = setOf(
    CloudAccountStep.SIGN_UP_DETAILS,
    CloudAccountStep.RECOVERY_NEW_PASSWORD,
    CloudAccountStep.CHANGE_PASSWORD_CODE
)

private val stepsThatSendAPasswordChange = setOf(
    CloudAccountStep.RECOVERY_NEW_PASSWORD,
    CloudAccountStep.CHANGE_PASSWORD_CODE
)

private val signedInSteps = setOf(
    CloudAccountStep.SIGNED_IN,
    CloudAccountStep.CHANGE_PASSWORD,
    CloudAccountStep.CHANGE_PASSWORD_CODE,
    CloudAccountStep.CHANGE_EMAIL,
    CloudAccountStep.CHANGE_EMAIL_CODE,
    CloudAccountStep.RECOVERY_WORDS_PASSWORD,
    CloudAccountStep.SHOW_RECOVERY_WORDS,
    CloudAccountStep.DELETE_ACCOUNT
)

private fun previousStepOf(step: CloudAccountStep): CloudAccountStep? = when (step) {
    CloudAccountStep.SIGN_UP_EMAIL -> CloudAccountStep.WELCOME
    CloudAccountStep.SIGN_UP_DETAILS -> CloudAccountStep.SIGN_UP_EMAIL
    CloudAccountStep.LOG_IN -> CloudAccountStep.WELCOME
    CloudAccountStep.RECOVERY_EMAIL -> CloudAccountStep.LOG_IN
    CloudAccountStep.RECOVERY_WORDS -> CloudAccountStep.RECOVERY_EMAIL
    CloudAccountStep.RECOVERY_NEW_PASSWORD -> CloudAccountStep.RECOVERY_WORDS
    CloudAccountStep.CHANGE_PASSWORD,
    CloudAccountStep.CHANGE_EMAIL,
    CloudAccountStep.RECOVERY_WORDS_PASSWORD,
    CloudAccountStep.SHOW_RECOVERY_WORDS,
    CloudAccountStep.DELETE_ACCOUNT -> CloudAccountStep.SIGNED_IN
    CloudAccountStep.CHANGE_PASSWORD_CODE -> CloudAccountStep.CHANGE_PASSWORD
    CloudAccountStep.CHANGE_EMAIL_CODE -> CloudAccountStep.CHANGE_EMAIL
    CloudAccountStep.LOADING,
    CloudAccountStep.WELCOME,
    CloudAccountStep.SIGN_UP_RECOVERY_WORDS,
    CloudAccountStep.SIGNED_IN -> null
}

private suspend fun cloudAccountErrorMessage(
    error: Exception,
    serverAddress: String,
    isSignedIn: Boolean,
    isPasswordChange: Boolean = false
): String = when (error) {
    is CloudApiException -> when (error.code) {
        CloudApiErrorCode.BAD_REQUEST ->
            getString(if (isPasswordChange) Res.string.cloud_error_something_went_wrong else Res.string.cloud_error_invalid_email)
        CloudApiErrorCode.INVALID_PASSWORD ->
            getString(if (isSignedIn) Res.string.cloud_error_incorrect_password else Res.string.cloud_error_wrong_email_or_password)
        CloudApiErrorCode.INCORRECT_CODE -> getString(Res.string.cloud_error_incorrect_code)
        CloudApiErrorCode.EXPIRED_CODE -> getString(Res.string.cloud_error_expired_code)
        CloudApiErrorCode.TOO_MANY_REQUESTS -> getString(Res.string.cloud_error_too_many_tries)
        CloudApiErrorCode.UPGRADE_REQUIRED -> getString(Res.string.cloud_error_update_required)
        CloudApiErrorCode.INVALID_TOKEN -> getString(Res.string.cloud_error_signed_out)
        CloudApiErrorCode.NOT_FOUND -> getString(Res.string.cloud_error_device_already_signed_out)
        CloudApiErrorCode.EMAIL_ALREADY_USED -> getString(Res.string.cloud_error_email_already_used)
        else -> getString(Res.string.cloud_error_something_went_wrong)
    }
    is CloudServerUnreachableException -> getString(Res.string.cloud_error_server_unreachable, serverAddress)
    is InvalidRecoveryWordsException -> getString(Res.string.cloud_error_invalid_recovery_words)
    is IncorrectRecoveryKeyException -> getString(Res.string.cloud_error_recovery_words_other_account)
    is IncorrectCloudPasswordException -> getString(Res.string.cloud_error_incorrect_password)
    else -> getString(Res.string.cloud_error_something_went_wrong)
}

private fun cloudSyncResultMessage(
    result: CloudSyncResult,
    syncNetwork: SelfHostSyncNetwork,
    accountDetails: CloudAccountDetails?
): CloudText = when (result) {
    is CloudSyncResult.Done -> when (result.notSentCount) {
        0 -> CloudText.Phrase(Res.string.cloud_synced, listOf(result.receivedCount, result.sentCount))
        else -> CloudText.Phrase(
            Res.string.cloud_synced_with_not_sent,
            listOf(result.receivedCount, result.sentCount, result.notSentCount)
        )
    }
    is CloudSyncResult.UploadsRefused -> uploadsRefusedMessage(result.reason, accountDetails)
    is CloudSyncResult.CloudCopyRemoved -> cloudCopyRemovedMessage(result.removedAtMicroseconds, TimeZone.currentSystemDefault())
    CloudSyncResult.NotSignedIn -> CloudText.Phrase(Res.string.cloud_error_signed_out)
    CloudSyncResult.Busy -> CloudText.Phrase(Res.string.cloud_sync_busy)
    CloudSyncResult.UpdateRequired -> CloudText.Phrase(Res.string.cloud_sync_update_required)
    CloudSyncResult.AnotherSyncMethodIsOn -> CloudText.Phrase(Res.string.cloud_sync_other_method_on)
    CloudSyncResult.WaitingForAllowedNetwork -> CloudText.Phrase(waitingForNetworkPhrase(syncNetwork))
    is CloudSyncResult.Failed -> CloudText.Plain(result.message)
}

private fun aSyncFinished(previous: CloudSyncStatus, current: CloudSyncStatus): Boolean {
    val notePassFinished = previous.isSyncing && !current.isSyncing
    val mediaPassFinished = current.lastMediaPass != null &&
        current.lastMediaPass.finishedAtMillis != previous.lastMediaPass?.finishedAtMillis
    return notePassFinished || mediaPassFinished
}

class CloudAccountViewModel(
    private val cloudAccountRepository: CloudAccountRepository,
    private val cloudSyncEngine: CloudSyncEngine,
    private val cloudSyncScheduler: CloudSyncScheduler,
    private val selfHostConnectionState: SelfHostConnectionState,
    private val syncPairingState: SyncPairingState,
    private val settingsManager: SettingsManager,
    private val syncNetworkRule: SyncNetworkRule
) : ViewModel() {

    private val _screenState = MutableStateFlow(
        CloudAccountScreenState(serverAddressInput = settingsManager.getCloudServerAddress())
    )
    val screenState: StateFlow<CloudAccountScreenState> = _screenState.asStateFlow()

    private var pendingRecovery: CloudPendingRecovery? = null
    private var passwordStrengthJob: Job? = null
    private var accountOverviewJob: Job? = null
    private var isSigningOutOnPurpose = false
    private var isScreenShown = false

    init {
        viewModelScope.launch {
            cloudAccountRepository.signInState.collect { signInState -> onSignInStateChanged(signInState) }
        }
        viewModelScope.launch {
            var previousSyncStatus: CloudSyncStatus? = null
            cloudSyncEngine.status.collect { syncStatus ->
                _screenState.update { it.copy(syncStatus = syncStatus, syncNetwork = syncNetworkRule.chosenNetwork()) }
                val finishedSince = previousSyncStatus?.let { previous -> aSyncFinished(previous, syncStatus) } == true
                if (isScreenShown && finishedSince) launch { cloudAccountRepository.refreshAccountDetails() }
                previousSyncStatus = syncStatus
            }
        }
        viewModelScope.launch {
            cloudAccountRepository.accountDetails.collect { details -> _screenState.update { it.copy(accountDetails = details) } }
        }
        viewModelScope.launch {
            cloudSyncEngine.mediaSyncProgress.collect { progress -> _screenState.update { it.copy(mediaSyncProgress = progress) } }
        }
        viewModelScope.launch {
            cloudSyncEngine.mediaPassDetails.collect { details -> _screenState.update { it.copy(mediaPassDetails = details) } }
        }
        viewModelScope.launch {
            combine(selfHostConnectionState.isConnected, syncPairingState.isPaired) { isSelfHostConnected, isPaired ->
                when {
                    isSelfHostConnected -> getString(Res.string.cloud_disconnect_self_host_first)
                    isPaired -> getString(Res.string.cloud_unpair_lan_sync_first)
                    else -> null
                }
            }.collect { message -> _screenState.update { it.copy(otherSyncMethodMessage = message) } }
        }
        runAccountTask {
            try {
                cloudAccountRepository.loadSignedInState()
            } finally {
                showSignInState()
            }
        }
    }

    fun onServerAddressChanged(input: String) {
        _screenState.update { it.copy(serverAddressInput = input, serverAddressError = null) }
    }

    fun saveServerAddress() {
        if (_screenState.value.isWorking) return
        val address = normalizedCloudServerAddress(_screenState.value.serverAddressInput)
        if (address == null) {
            _screenState.update {
                it.copy(serverAddressError = Res.string.cloud_server_address_invalid)
            }
            return
        }
        settingsManager.saveCloudServerAddress(address)
        _screenState.update { it.copy(serverAddressInput = address, serverAddressError = null) }
        viewModelScope.launch { showNativeToast(getString(Res.string.cloud_server_address_saved)) }
    }

    fun onEmailChanged(email: String) {
        _screenState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onCodeChanged(input: String) {
        _screenState.update { it.copy(code = input.filter(Char::isDigit).take(EMAIL_CODE_LENGTH), errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _screenState.update { it.copy(password = password, errorMessage = null) }
        if (_screenState.value.step in stepsThatPickANewPassword) checkPasswordStrength(password)
    }

    fun onPasswordAgainChanged(passwordAgain: String) {
        _screenState.update { it.copy(passwordAgain = passwordAgain, errorMessage = null) }
    }

    fun onRecoveryWordsChanged(words: String) {
        _screenState.update { it.copy(recoveryWordsInput = words, errorMessage = null) }
    }

    fun onSavedRecoveryWordsChanged(hasSaved: Boolean) {
        _screenState.update { it.copy(hasSavedRecoveryWords = hasSaved) }
    }

    fun onLogOutOtherDevicesChanged(logOutOtherDevices: Boolean) {
        _screenState.update { it.copy(logOutOtherDevices = logOutOtherDevices) }
    }

    fun onClearNotesOnThisDeviceChanged(clearNotes: Boolean) {
        _screenState.update { it.copy(clearNotesOnThisDevice = clearNotes) }
    }

    fun startSignUp() = openStep(CloudAccountStep.SIGN_UP_EMAIL)

    fun startLogIn() = openStep(CloudAccountStep.LOG_IN)

    fun startRecovery() = openStep(CloudAccountStep.RECOVERY_EMAIL)

    fun startChangePassword() = openSignedInStep(CloudAccountStep.CHANGE_PASSWORD)

    fun startChangeEmail() = openSignedInStep(CloudAccountStep.CHANGE_EMAIL)

    fun askPasswordForRecoveryWords() = openSignedInStep(CloudAccountStep.RECOVERY_WORDS_PASSWORD)

    fun goBack() {
        val current = _screenState.value
        if (current.isWorking) return
        val previousStep = previousStepOf(current.step) ?: return
        if (previousStep == CloudAccountStep.SIGNED_IN) {
            returnToSignedIn()
            return
        }
        if (current.step == CloudAccountStep.RECOVERY_NEW_PASSWORD) pendingRecovery = null
        _screenState.update {
            it.copy(
                step = previousStep,
                code = "",
                password = if (previousStep == CloudAccountStep.LOG_IN) it.password else "",
                passwordAgain = "",
                recoveryWordsInput = "",
                errorMessage = null,
                newCodeSentMessage = null
            )
        }
    }

    fun sendSignupCode() {
        if (!_screenState.value.canSendCode) return
        runAccountTask {
            val email = _screenState.value.email.trim()
            cloudAccountRepository.sendSignupCode(email)
            showCodeSent(email, codeStep = CloudAccountStep.SIGN_UP_DETAILS)
        }
    }

    fun createAccount() {
        if (!_screenState.value.canCreateAccount) return
        runAccountTask {
            val current = _screenState.value
            val words = cloudAccountRepository.signUp(current.email, current.code, current.password)
            _screenState.update {
                it.copy(
                    step = CloudAccountStep.SIGN_UP_RECOVERY_WORDS,
                    code = "",
                    password = "",
                    passwordAgain = "",
                    newRecoveryWords = words,
                    hasSavedRecoveryWords = false
                )
            }
        }
    }

    fun finishSignUp() {
        val current = _screenState.value
        if (!current.canFinishSignUp) return
        showSignedIn(current.email)
    }

    fun logIn() {
        if (!_screenState.value.canLogIn) return
        runAccountTask {
            val current = _screenState.value
            val email = current.email.trim()
            cloudAccountRepository.logIn(email, current.password)
            showSignedIn(email)
        }
    }

    fun sendRecoveryCode() {
        if (!_screenState.value.canSendCode) return
        runAccountTask {
            val email = _screenState.value.email.trim()
            cloudAccountRepository.sendRecoveryCode(email)
            showCodeSent(email, codeStep = CloudAccountStep.RECOVERY_WORDS)
        }
    }

    fun checkRecoveryWords() {
        if (!_screenState.value.canCheckRecoveryWords) return
        runAccountTask {
            val current = _screenState.value
            pendingRecovery = cloudAccountRepository.recover(current.email, current.code, current.recoveryWordsInput)
            _screenState.update {
                it.copy(
                    step = CloudAccountStep.RECOVERY_NEW_PASSWORD,
                    code = "",
                    recoveryWordsInput = "",
                    password = "",
                    passwordAgain = "",
                    newCodeSentMessage = null
                )
            }
        }
    }

    fun setNewPassword(logOutOtherDevices: Boolean) {
        if (!_screenState.value.canSetNewPassword) return
        val recovery = pendingRecovery ?: return
        runAccountTask {
            cloudAccountRepository.finishRecovery(recovery, _screenState.value.password, logOutOtherDevices)
            pendingRecovery = null
            showSignedIn(recovery.email)
        }
    }

    fun onScreenShown() {
        isScreenShown = true
        val current = _screenState.value
        if (current.isWorking || current.step !in signedInSteps) return
        returnToSignedIn()
        loadAccountOverview()
    }

    fun onScreenHidden() {
        isScreenShown = false
    }

    fun syncNow() {
        if (_screenState.value.syncStatus.isSyncing) return
        cloudSyncScheduler.syncNow()
    }

    fun signOutThisDevice() {
        runAccountTask {
            signOutOnPurpose { cloudAccountRepository.signOutThisDevice() }
        }
    }

    fun signOutSession(session: CloudSession) {
        if (session.isCurrent) {
            signOutThisDevice()
            return
        }
        runAccountTask {
            try {
                cloudAccountRepository.signOutSession(session)
            } finally {
                loadSessions()
            }
        }
    }

    fun sendPasswordChangeCode() {
        runAccountTask {
            cloudAccountRepository.sendPasswordChangeCode()
            showCodeSent(_screenState.value.signedInEmail, codeStep = CloudAccountStep.CHANGE_PASSWORD_CODE)
        }
    }

    fun changePassword() {
        if (!_screenState.value.canChangePassword) return
        runAccountTask {
            val current = _screenState.value
            cloudAccountRepository.changePassword(current.password, current.code, current.logOutOtherDevices)
            showNativeToast(getString(Res.string.cloud_password_changed))
            returnToSignedIn()
            if (current.logOutOtherDevices) loadSessions()
        }
    }

    fun sendChangeEmailCode() {
        if (!_screenState.value.canSendCode) return
        runAccountTask {
            val newEmail = _screenState.value.email.trim()
            cloudAccountRepository.sendChangeEmailCode(newEmail)
            showCodeSent(newEmail, codeStep = CloudAccountStep.CHANGE_EMAIL_CODE)
        }
    }

    fun changeEmail() {
        if (!_screenState.value.canChangeEmail) return
        runAccountTask {
            val current = _screenState.value
            cloudAccountRepository.changeEmail(current.email, current.code)
            showNativeToast(getString(Res.string.cloud_email_changed, current.email))
            returnToSignedIn()
        }
    }

    fun showRecoveryWords() {
        runAccountTask {
            showRecoveryWordsStep(cloudAccountRepository.readRecoveryWords())
        }
    }

    fun showRecoveryWordsAfterPasswordCheck() {
        if (!_screenState.value.canCheckPassword) return
        runAccountTask {
            showRecoveryWordsStep(cloudAccountRepository.readRecoveryWordsWithPassword(_screenState.value.password))
        }
    }

    fun sendDeleteAccountCode() {
        runAccountTask {
            cloudAccountRepository.sendDeleteAccountCode()
            showCodeSent(_screenState.value.signedInEmail, codeStep = CloudAccountStep.DELETE_ACCOUNT)
        }
    }

    fun deleteAccount() {
        if (!_screenState.value.canDeleteAccount) return
        runAccountTask {
            val current = _screenState.value
            signOutOnPurpose { cloudAccountRepository.deleteAccount(current.password, current.code) }
            if (current.clearNotesOnThisDevice && !eraseAllAppData()) {
                val message = getString(Res.string.cloud_account_deleted_notes_not_cleared)
                _screenState.update { it.copy(errorMessage = message) }
            }
        }
    }

    private fun openSignedInStep(step: CloudAccountStep) {
        if (_screenState.value.isWorking) return
        _screenState.update {
            it.copy(
                step = step,
                email = "",
                code = "",
                password = "",
                passwordAgain = "",
                logOutOtherDevices = true,
                clearNotesOnThisDevice = false,
                errorMessage = null,
                newCodeSentMessage = null
            )
        }
    }

    private fun returnToSignedIn() {
        _screenState.update {
            it.copy(
                step = CloudAccountStep.SIGNED_IN,
                email = "",
                code = "",
                password = "",
                passwordAgain = "",
                logOutOtherDevices = true,
                recoveryWordsToShow = "",
                clearNotesOnThisDevice = false,
                errorMessage = null,
                newCodeSentMessage = null
            )
        }
    }

    private fun showRecoveryWordsStep(words: String) {
        _screenState.update {
            it.copy(step = CloudAccountStep.SHOW_RECOVERY_WORDS, password = "", recoveryWordsToShow = words)
        }
    }

    private suspend fun signOutOnPurpose(signOut: suspend () -> Unit) {
        isSigningOutOnPurpose = true
        try {
            signOut()
        } catch (error: Exception) {
            if (cloudAccountRepository.signInState.value is CloudSignInState.SignedIn) isSigningOutOnPurpose = false
            throw error
        }
    }

    private suspend fun onSignInStateChanged(signInState: CloudSignInState) {
        if (_screenState.value.step !in signedInSteps) return
        when (signInState) {
            is CloudSignInState.SignedIn -> _screenState.update { it.copy(signedInEmail = signInState.email) }
            CloudSignInState.SignedOut -> {
                val message = if (isSigningOutOnPurpose) _screenState.value.errorMessage else getString(Res.string.cloud_error_signed_out)
                isSigningOutOnPurpose = false
                showSignedOut(errorMessage = message)
            }
        }
    }

    private fun loadAccountOverview() {
        accountOverviewJob?.cancel()
        accountOverviewJob = viewModelScope.launch {
            loadAccountDetails()
            loadSessions()
        }
    }

    private suspend fun loadAccountDetails() {
        if (cloudAccountRepository.signInState.value !is CloudSignInState.SignedIn) return
        try {
            cloudAccountRepository.fetchAccountDetails()
            _screenState.update { it.copy(accountDetailsError = null) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            val message = signedInErrorMessage(error)
            _screenState.update { it.copy(accountDetailsError = message) }
        }
    }

    private suspend fun loadSessions() {
        if (cloudAccountRepository.signInState.value !is CloudSignInState.SignedIn) return
        try {
            val sessions = cloudAccountRepository.listSessions().sortedByDescending { it.lastUsedAtMicroseconds }
            _screenState.update { it.copy(sessions = sessions, sessionsError = null) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            val message = signedInErrorMessage(error)
            _screenState.update { it.copy(sessionsError = message) }
        }
    }

    private suspend fun signedInErrorMessage(error: Exception): String =
        cloudAccountErrorMessage(error, settingsManager.getCloudServerAddress(), isSignedIn = true)

    private fun openStep(step: CloudAccountStep) {
        if (_screenState.value.isWorking) return
        _screenState.update {
            it.copy(
                step = step,
                code = "",
                password = "",
                passwordAgain = "",
                recoveryWordsInput = "",
                errorMessage = null,
                newCodeSentMessage = null
            )
        }
    }

    private suspend fun showCodeSent(email: String, codeStep: CloudAccountStep) {
        val newCodeSentMessage = getString(Res.string.cloud_new_code_sent, email)
        _screenState.update {
            if (it.step == codeStep) {
                it.copy(code = "", newCodeSentMessage = newCodeSentMessage)
            } else {
                it.copy(step = codeStep, email = email, code = "", newCodeSentMessage = null)
            }
        }
    }

    private fun showSignedIn(email: String) {
        isSigningOutOnPurpose = false
        _screenState.update {
            CloudAccountScreenState(
                step = CloudAccountStep.SIGNED_IN,
                serverAddressInput = it.serverAddressInput,
                signedInEmail = email,
                isWorking = it.isWorking,
                syncStatus = it.syncStatus,
                accountDetails = it.accountDetails,
                mediaSyncProgress = it.mediaSyncProgress,
                mediaPassDetails = it.mediaPassDetails,
                otherSyncMethodMessage = it.otherSyncMethodMessage
            )
        }
        loadAccountOverview()
    }

    private fun showSignedOut(errorMessage: String?) {
        accountOverviewJob?.cancel()
        _screenState.update {
            CloudAccountScreenState(
                step = CloudAccountStep.WELCOME,
                serverAddressInput = it.serverAddressInput,
                isWorking = it.isWorking,
                errorMessage = errorMessage,
                syncStatus = it.syncStatus,
                accountDetails = it.accountDetails,
                mediaSyncProgress = it.mediaSyncProgress,
                mediaPassDetails = it.mediaPassDetails,
                otherSyncMethodMessage = it.otherSyncMethodMessage
            )
        }
    }

    private fun showSignInState() {
        when (val signInState = cloudAccountRepository.signInState.value) {
            is CloudSignInState.SignedIn -> showSignedIn(signInState.email)
            CloudSignInState.SignedOut -> showSignedOut(errorMessage = null)
        }
    }

    private fun checkPasswordStrength(password: String) {
        passwordStrengthJob?.cancel()
        passwordStrengthJob = viewModelScope.launch {
            val strength = withContext(Dispatchers.Default) { estimateCloudPasswordStrength(password) }
            _screenState.update {
                if (it.password == password) it.copy(passwordStrength = strength, passwordStrengthCheckedFor = password) else it
            }
        }
    }

    private fun runAccountTask(task: suspend () -> Unit) {
        if (_screenState.value.isWorking) return
        val isSignedInTask = _screenState.value.step in signedInSteps
        val isPasswordChangeTask = _screenState.value.step in stepsThatSendAPasswordChange
        _screenState.update { it.copy(isWorking = true, errorMessage = null, newCodeSentMessage = null) }
        viewModelScope.launch {
            try {
                task()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                val message = cloudAccountErrorMessage(
                    error,
                    settingsManager.getCloudServerAddress(),
                    isSignedInTask,
                    isPasswordChangeTask
                )
                _screenState.update { it.copy(errorMessage = message) }
            } finally {
                _screenState.update { it.copy(isWorking = false) }
            }
        }
    }
}
