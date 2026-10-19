package com.emberr.presentation.settings.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.cloud.account.CloudAccountRepository
import com.emberr.domain.cloud.account.CloudPasswordStrength
import com.emberr.domain.cloud.account.CloudPendingLogin
import com.emberr.domain.cloud.account.CloudPendingRecovery
import com.emberr.domain.cloud.account.CloudSignInState
import com.emberr.domain.cloud.account.estimateCloudPasswordStrength
import com.emberr.domain.cloud.api.CloudApiErrorCode
import com.emberr.domain.cloud.api.CloudApiException
import com.emberr.domain.cloud.api.CloudServerUnreachableException
import com.emberr.domain.cloud.api.normalizedCloudServerAddress
import com.emberr.domain.cloud.keys.IncorrectRecoveryKeyException
import com.emberr.domain.cloud.keys.InvalidRecoveryWordsException
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
    LOG_IN_CODE,
    RECOVERY_EMAIL,
    RECOVERY_WORDS,
    RECOVERY_NEW_PASSWORD,
    SIGNED_IN
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
    val newRecoveryWords: List<String> = emptyList(),
    val hasSavedRecoveryWords: Boolean = false,
    val signedInEmail: String = "",
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val newCodeSentMessage: String? = null
) {
    val isServerAddressShown: Boolean
        get() = step !in setOf(CloudAccountStep.LOADING, CloudAccountStep.SIGN_UP_RECOVERY_WORDS, CloudAccountStep.SIGNED_IN)

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

    val canVerifyLoginCode: Boolean
        get() = code.length == EMAIL_CODE_LENGTH && !isWorking

    val canCheckRecoveryWords: Boolean
        get() = code.length == EMAIL_CODE_LENGTH && recoveryWordsInput.isNotBlank() && !isWorking

    val canSetNewPassword: Boolean
        get() = isNewPasswordAccepted && !isWorking

    val canFinishSignUp: Boolean
        get() = hasSavedRecoveryWords && !isWorking

    private val isNewPasswordAccepted: Boolean
        get() = doPasswordsMatch && isPasswordStrengthKnown && passwordStrength != CloudPasswordStrength.WEAK
}

private const val EMAIL_CODE_LENGTH = 6

private val stepsThatPickANewPassword = setOf(CloudAccountStep.SIGN_UP_DETAILS, CloudAccountStep.RECOVERY_NEW_PASSWORD)

private fun previousStepOf(step: CloudAccountStep): CloudAccountStep? = when (step) {
    CloudAccountStep.SIGN_UP_EMAIL -> CloudAccountStep.WELCOME
    CloudAccountStep.SIGN_UP_DETAILS -> CloudAccountStep.SIGN_UP_EMAIL
    CloudAccountStep.LOG_IN -> CloudAccountStep.WELCOME
    CloudAccountStep.LOG_IN_CODE -> CloudAccountStep.LOG_IN
    CloudAccountStep.RECOVERY_EMAIL -> CloudAccountStep.LOG_IN
    CloudAccountStep.RECOVERY_WORDS -> CloudAccountStep.RECOVERY_EMAIL
    CloudAccountStep.RECOVERY_NEW_PASSWORD -> CloudAccountStep.RECOVERY_WORDS
    CloudAccountStep.LOADING,
    CloudAccountStep.WELCOME,
    CloudAccountStep.SIGN_UP_RECOVERY_WORDS,
    CloudAccountStep.SIGNED_IN -> null
}

private const val SOMETHING_WENT_WRONG_MESSAGE = "Something went wrong. Try again."

private fun cloudAccountErrorMessage(error: Exception, serverAddress: String): String = when (error) {
    is CloudApiException -> when (error.code) {
        CloudApiErrorCode.BAD_REQUEST -> "Enter a valid email address."
        CloudApiErrorCode.INVALID_PASSWORD -> "Wrong email or password."
        CloudApiErrorCode.INCORRECT_CODE -> "That code is not right."
        CloudApiErrorCode.EXPIRED_CODE -> "That code has expired. Ask for a new one."
        CloudApiErrorCode.TOO_MANY_REQUESTS -> "Too many tries. Wait a bit and try again."
        CloudApiErrorCode.UPGRADE_REQUIRED -> "Update Emberr to use Emberr Cloud."
        else -> SOMETHING_WENT_WRONG_MESSAGE
    }
    is CloudServerUnreachableException -> "Can't reach the server at $serverAddress."
    is InvalidRecoveryWordsException ->
        "Those words are not a valid recovery key. Make sure there are 24 words, and check the spelling of each."
    is IncorrectRecoveryKeyException -> "These words belong to a different account."
    else -> SOMETHING_WENT_WRONG_MESSAGE
}

class CloudAccountViewModel(
    private val cloudAccountRepository: CloudAccountRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _screenState = MutableStateFlow(
        CloudAccountScreenState(serverAddressInput = settingsManager.getCloudServerAddress())
    )
    val screenState: StateFlow<CloudAccountScreenState> = _screenState.asStateFlow()

    private var pendingLogin: CloudPendingLogin? = null
    private var pendingRecovery: CloudPendingRecovery? = null
    private var passwordStrengthJob: Job? = null

    init {
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

    fun startSignUp() = openStep(CloudAccountStep.SIGN_UP_EMAIL)

    fun startLogIn() = openStep(CloudAccountStep.LOG_IN)

    fun startRecovery() = openStep(CloudAccountStep.RECOVERY_EMAIL)

    fun goBack() {
        val current = _screenState.value
        if (current.isWorking) return
        val previousStep = previousStepOf(current.step) ?: return
        if (current.step == CloudAccountStep.LOG_IN_CODE) pendingLogin = null
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
                    newRecoveryWords = words.split(" "),
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
            pendingLogin = cloudAccountRepository.logIn(email, current.password)
            showCodeSent(email, codeStep = CloudAccountStep.LOG_IN_CODE)
        }
    }

    fun verifyLoginCode() {
        if (!_screenState.value.canVerifyLoginCode) return
        val login = pendingLogin ?: return
        runAccountTask {
            cloudAccountRepository.finishLogin(login, _screenState.value.code)
            pendingLogin = null
            showSignedIn(login.email)
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

    fun signOutThisDevice() {
        runAccountTask {
            cloudAccountRepository.signOutThisDevice()
            showSignInState()
        }
    }

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
        _screenState.update {
            CloudAccountScreenState(
                step = CloudAccountStep.SIGNED_IN,
                serverAddressInput = it.serverAddressInput,
                signedInEmail = email,
                isWorking = it.isWorking
            )
        }
    }

    private fun showSignInState() {
        when (val signInState = cloudAccountRepository.signInState.value) {
            is CloudSignInState.SignedIn -> showSignedIn(signInState.email)
            CloudSignInState.SignedOut -> _screenState.update {
                CloudAccountScreenState(
                    step = CloudAccountStep.WELCOME,
                    serverAddressInput = it.serverAddressInput,
                    isWorking = it.isWorking
                )
            }
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
        _screenState.update { it.copy(isWorking = true, errorMessage = null, newCodeSentMessage = null) }
        viewModelScope.launch {
            try {
                task()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                val message = cloudAccountErrorMessage(error, settingsManager.getCloudServerAddress())
                _screenState.update { it.copy(errorMessage = message) }
            } finally {
                _screenState.update { it.copy(isWorking = false) }
            }
        }
    }
}
