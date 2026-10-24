package com.emberr.presentation.settings.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.cloud.account.CloudAccountRepository
import com.emberr.domain.cloud.account.CloudPasswordStrength
import com.emberr.domain.cloud.account.CloudPendingRecovery
import com.emberr.domain.cloud.account.CloudSignInState
import com.emberr.domain.cloud.account.estimateCloudPasswordStrength
import com.emberr.domain.cloud.api.CloudApiErrorCode
import com.emberr.domain.cloud.api.CloudApiException
import com.emberr.domain.cloud.api.CloudServerUnreachableException
import com.emberr.domain.cloud.api.CloudSession
import com.emberr.domain.cloud.api.normalizedCloudServerAddress
import com.emberr.domain.cloud.keys.IncorrectCloudPasswordException
import com.emberr.domain.cloud.keys.IncorrectRecoveryKeyException
import com.emberr.domain.cloud.keys.InvalidRecoveryWordsException
import com.emberr.domain.cloud.sync.CloudSyncEngine
import com.emberr.domain.cloud.sync.CloudSyncResult
import com.emberr.domain.util.system.eraseAllAppData
import com.emberr.domain.util.system.showNativeToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

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
    val serverAddressError: String? = null,
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
    val storageUsedBytes: Long? = null,
    val accountDetailsError: String? = null,
    val sessions: List<CloudSession>? = null,
    val sessionsError: String? = null,
    val logOutOtherDevices: Boolean = true,
    val recoveryWordsToShow: String = "",
    val clearNotesOnThisDevice: Boolean = false,
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val newCodeSentMessage: String? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
) {
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

private const val SOMETHING_WENT_WRONG_MESSAGE = "Something went wrong. Try again."

private const val SIGNED_OUT_MESSAGE = "You were signed out. Log in again."

private const val INCORRECT_PASSWORD_MESSAGE = "Incorrect password. Try again."

private fun cloudAccountErrorMessage(
    error: Exception,
    serverAddress: String,
    isSignedIn: Boolean,
    isPasswordChange: Boolean = false
): String = when (error) {
    is CloudApiException -> when (error.code) {
        CloudApiErrorCode.BAD_REQUEST -> if (isPasswordChange) SOMETHING_WENT_WRONG_MESSAGE else "Enter a valid email address."
        CloudApiErrorCode.INVALID_PASSWORD -> if (isSignedIn) INCORRECT_PASSWORD_MESSAGE else "Wrong email or password."
        CloudApiErrorCode.INCORRECT_CODE -> "That code is not right."
        CloudApiErrorCode.EXPIRED_CODE -> "That code has expired. Ask for a new one."
        CloudApiErrorCode.TOO_MANY_REQUESTS -> "Too many tries. Wait a bit and try again."
        CloudApiErrorCode.UPGRADE_REQUIRED -> "Update Emberr to use Emberr Cloud."
        CloudApiErrorCode.INVALID_TOKEN -> SIGNED_OUT_MESSAGE
        CloudApiErrorCode.NOT_FOUND -> "That device was already signed out."
        CloudApiErrorCode.EMAIL_ALREADY_USED -> "Another account already uses this email."
        else -> SOMETHING_WENT_WRONG_MESSAGE
    }
    is CloudServerUnreachableException -> "Can't reach the server at $serverAddress."
    is InvalidRecoveryWordsException ->
        "Those words are not a valid recovery key. Make sure there are 24 words, and check the spelling of each."
    is IncorrectRecoveryKeyException -> "These words belong to a different account."
    is IncorrectCloudPasswordException -> INCORRECT_PASSWORD_MESSAGE
    else -> SOMETHING_WENT_WRONG_MESSAGE
}

private fun cloudSyncResultMessage(result: CloudSyncResult): String = when (result) {
    is CloudSyncResult.Done -> when (result.notSentCount) {
        0 -> "Synced: ${result.receivedCount} received, ${result.sentCount} sent"
        else -> "Synced: ${result.receivedCount} received, ${result.sentCount} sent, ${result.notSentCount} not sent"
    }
    CloudSyncResult.NotSignedIn -> SIGNED_OUT_MESSAGE
    CloudSyncResult.Busy -> "Another sync is running. Try again in a moment."
    CloudSyncResult.UpdateRequired -> "Update Emberr to keep syncing with Emberr Cloud."
    is CloudSyncResult.Failed -> result.message
}

class CloudAccountViewModel(
    private val cloudAccountRepository: CloudAccountRepository,
    private val cloudSyncEngine: CloudSyncEngine,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _screenState = MutableStateFlow(
        CloudAccountScreenState(serverAddressInput = settingsManager.getCloudServerAddress())
    )
    val screenState: StateFlow<CloudAccountScreenState> = _screenState.asStateFlow()

    private var pendingRecovery: CloudPendingRecovery? = null
    private var passwordStrengthJob: Job? = null
    private var accountOverviewJob: Job? = null
    private var isSigningOutOnPurpose = false

    init {
        viewModelScope.launch {
            cloudAccountRepository.signInState.collect { signInState -> onSignInStateChanged(signInState) }
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
                it.copy(serverAddressError = "Enter an address that starts with http:// or https://, like http://localhost:8080.")
            }
            return
        }
        settingsManager.saveCloudServerAddress(address)
        _screenState.update { it.copy(serverAddressInput = address, serverAddressError = null) }
        showNativeToast("Server address saved")
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
        val current = _screenState.value
        if (current.isWorking || current.step !in signedInSteps) return
        returnToSignedIn()
        loadAccountOverview()
    }

    fun syncNow() {
        if (_screenState.value.isSyncing) return
        _screenState.update { it.copy(isSyncing = true, syncMessage = null) }
        viewModelScope.launch {
            val result = cloudSyncEngine.syncNow()
            _screenState.update { it.copy(isSyncing = false, syncMessage = cloudSyncResultMessage(result)) }
        }
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
            showNativeToast("Password changed successfully")
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
            showNativeToast("Email changed to ${current.email}")
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
                _screenState.update {
                    it.copy(errorMessage = "Your account was deleted, but the notes on this device couldn't be cleared.")
                }
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

    private fun onSignInStateChanged(signInState: CloudSignInState) {
        if (_screenState.value.step !in signedInSteps) return
        when (signInState) {
            is CloudSignInState.SignedIn -> _screenState.update { it.copy(signedInEmail = signInState.email) }
            CloudSignInState.SignedOut -> {
                val message = if (isSigningOutOnPurpose) _screenState.value.errorMessage else SIGNED_OUT_MESSAGE
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
            val accountDetails = cloudAccountRepository.fetchAccountDetails()
            _screenState.update { it.copy(storageUsedBytes = accountDetails.storageUsedBytes, accountDetailsError = null) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            _screenState.update { it.copy(accountDetailsError = signedInErrorMessage(error)) }
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
            _screenState.update { it.copy(sessionsError = signedInErrorMessage(error)) }
        }
    }

    private fun signedInErrorMessage(error: Exception): String =
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

    private fun showCodeSent(email: String, codeStep: CloudAccountStep) {
        _screenState.update {
            if (it.step == codeStep) {
                it.copy(code = "", newCodeSentMessage = "We sent a new code to $email.")
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
                isWorking = it.isWorking
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
                errorMessage = errorMessage
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
