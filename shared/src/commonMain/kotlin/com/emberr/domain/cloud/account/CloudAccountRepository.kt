package com.emberr.domain.cloud.account

import com.emberr.domain.cloud.api.CloudAccountDetails
import com.emberr.domain.cloud.api.CloudApiClient
import com.emberr.domain.cloud.api.CloudApiErrorCode
import com.emberr.domain.cloud.api.CloudApiException
import com.emberr.domain.cloud.api.CloudEmailCodePurpose
import com.emberr.domain.cloud.api.CloudServerUnreachableException
import com.emberr.domain.cloud.api.CloudSession
import com.emberr.domain.cloud.crypto.CloudCrypto
import com.emberr.domain.cloud.keys.CloudAccountKeyManager
import com.emberr.domain.cloud.keys.CloudRecoveryWords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64

class CloudAccountRepository(
    private val apiClient: CloudApiClient,
    private val accountKeyManager: CloudAccountKeyManager,
    private val recoveryWords: CloudRecoveryWords,
    private val cloudCrypto: CloudCrypto,
    private val accountStorage: CloudAccountStorage,
    private val deviceName: String,
    private val forgetCloudSyncRecords: suspend () -> Unit = {}
) {

    private val mutableSignInState = MutableStateFlow<CloudSignInState>(CloudSignInState.SignedOut)
    val signInState: StateFlow<CloudSignInState> = mutableSignInState.asStateFlow()

    suspend fun loadSignedInState() {
        val sessionToken = accountStorage.getSessionToken()
        val email = accountStorage.getEmail()
        mutableSignInState.value = if (sessionToken != null && email != null) {
            CloudSignInState.SignedIn(email)
        } else {
            CloudSignInState.SignedOut
        }
    }

    suspend fun sendSignupCode(email: String) {
        apiClient.sendEmailCode(email, CloudEmailCodePurpose.SIGNUP)
    }

    suspend fun signUp(email: String, code: String, password: String): String {
        val signupKeys = withContext(Dispatchers.Default) { accountKeyManager.createKeysForSignup(password) }
        val words = recoveryWords.wordsFromRecoveryKey(signupKeys.recoveryKey)
        val sessionToken = apiClient.signUp(email, code, deviceName, signupKeys.loginKey, signupKeys.accountKeys)
        saveSignedInAccount(email, sessionToken, signupKeys.masterKey, signupKeys.privateKey)
        return words
    }

    suspend fun logIn(email: String, password: String) {
        val passwordParams = apiClient.fetchPasswordParams(email)
        val passwordKey = derivePasswordKeyOffMainThread(
            password,
            passwordParams.salt,
            passwordParams.memLimit,
            passwordParams.opsLimit
        )
        val loginKey = accountKeyManager.deriveLoginKey(passwordKey)
        val signedInDevice = apiClient.logIn(email, loginKey, deviceName)
        val accountKeys = signedInDevice.keys
        val masterKey = accountKeyManager.unlockMasterKeyWithPasswordKey(passwordKey, accountKeys)
        val privateKey = accountKeyManager.unlockPrivateKey(
            masterKey,
            accountKeys.encryptedPrivateKey,
            accountKeys.encryptedPrivateKeyNonce
        )
        saveSignedInAccount(email, signedInDevice.token, masterKey, privateKey)
    }

    suspend fun sendRecoveryCode(email: String) {
        apiClient.sendEmailCode(email, CloudEmailCodePurpose.RECOVER)
    }

    suspend fun recover(email: String, code: String, words: String): CloudPendingRecovery {
        val recoveryKey = recoveryWords.recoveryKeyFromWords(words)
        val recoveryStart = apiClient.recover(email, code, deviceName)
        val recoveryKeys = recoveryStart.keys
        val masterKey = accountKeyManager.unlockMasterKeyWithRecoveryKey(recoveryKey, recoveryKeys)
        val privateKey = accountKeyManager.unlockPrivateKey(
            masterKey,
            recoveryKeys.encryptedPrivateKey,
            recoveryKeys.encryptedPrivateKeyNonce
        )
        val sessionTokenBytes = cloudCrypto.openSealedBox(recoveryStart.encryptedToken, recoveryKeys.publicKey, privateKey)
        return CloudPendingRecovery(
            email = email,
            sessionToken = Base64.UrlSafe.encode(sessionTokenBytes),
            masterKey = masterKey,
            privateKey = privateKey
        )
    }

    suspend fun finishRecovery(
        pendingRecovery: CloudPendingRecovery,
        newPassword: String,
        logOutOtherDevices: Boolean = true
    ) {
        val newPasswordKeys = withContext(Dispatchers.Default) {
            accountKeyManager.createKeysForNewPassword(newPassword, pendingRecovery.masterKey)
        }
        apiClient.changePassword(
            pendingRecovery.sessionToken,
            newPasswordKeys.loginKey,
            newPasswordKeys.passwordKeys,
            code = null,
            logOutOtherDevices = logOutOtherDevices
        )
        saveSignedInAccount(
            pendingRecovery.email,
            pendingRecovery.sessionToken,
            pendingRecovery.masterKey,
            pendingRecovery.privateKey
        )
    }

    suspend fun sendPasswordChangeCode() {
        withStoredSessionToken { sessionToken -> apiClient.sendPasswordChangeCode(sessionToken) }
    }

    suspend fun changePassword(newPassword: String, code: String, logOutOtherDevices: Boolean = true) {
        withStoredSessionToken { sessionToken ->
            val masterKey = checkNotNull(accountStorage.getMasterKey()) { MISSING_ACCOUNT_MESSAGE }
            val newPasswordKeys = withContext(Dispatchers.Default) {
                accountKeyManager.createKeysForNewPassword(newPassword, masterKey)
            }
            apiClient.changePassword(
                sessionToken,
                newPasswordKeys.loginKey,
                newPasswordKeys.passwordKeys,
                code = code,
                logOutOtherDevices = logOutOtherDevices
            )
        }
    }

    suspend fun sendChangeEmailCode(newEmail: String) {
        withStoredSessionToken { sessionToken -> apiClient.sendChangeEmailCode(sessionToken, newEmail) }
    }

    suspend fun changeEmail(newEmail: String, code: String) {
        withStoredSessionToken { sessionToken -> apiClient.changeEmail(sessionToken, newEmail, code) }
        saveEmail(newEmail)
    }

    suspend fun listSessions(): List<CloudSession> =
        withStoredSessionToken { sessionToken -> apiClient.listSessions(sessionToken) }

    suspend fun signOutSession(session: CloudSession) {
        withStoredSessionToken { sessionToken -> apiClient.signOutSession(sessionToken, session.id) }
        if (session.isCurrent) clearLocalAccount()
    }

    suspend fun signOutThisDevice() {
        try {
            endCurrentSessionOnServer()
        } catch (_: CloudApiException) {
        } catch (_: CloudServerUnreachableException) {
        }
        clearLocalAccount()
    }

    suspend fun fetchAccountDetails(): CloudAccountDetails {
        val accountDetails = withStoredSessionToken { sessionToken -> apiClient.fetchAccountDetails(sessionToken) }
        if (accountDetails.email != accountStorage.getEmail()) saveEmail(accountDetails.email)
        return accountDetails
    }

    suspend fun readRecoveryWords(): String {
        val masterKey = checkNotNull(accountStorage.getMasterKey()) { MISSING_ACCOUNT_MESSAGE }
        val accountKeys = withStoredSessionToken { sessionToken -> apiClient.fetchAccountDetails(sessionToken).keys }
        return recoveryWords.wordsFromRecoveryKey(accountKeyManager.readRecoveryKey(masterKey, accountKeys))
    }

    suspend fun readRecoveryWordsWithPassword(password: String): String {
        val accountKeys = withStoredSessionToken { sessionToken -> apiClient.fetchAccountDetails(sessionToken).keys }
        val passwordKey = derivePasswordKeyOffMainThread(
            password,
            accountKeys.salt,
            accountKeys.memLimit,
            accountKeys.opsLimit
        )
        val masterKey = accountKeyManager.unlockMasterKeyWithPasswordKey(passwordKey, accountKeys)
        return recoveryWords.wordsFromRecoveryKey(accountKeyManager.readRecoveryKey(masterKey, accountKeys))
    }

    suspend fun sendDeleteAccountCode() {
        withStoredSessionToken { sessionToken -> apiClient.sendDeleteAccountCode(sessionToken) }
    }

    suspend fun deleteAccount(password: String, code: String) {
        withStoredSessionToken { sessionToken ->
            val accountKeys = apiClient.fetchAccountDetails(sessionToken).keys
            val passwordKey = derivePasswordKeyOffMainThread(
                password,
                accountKeys.salt,
                accountKeys.memLimit,
                accountKeys.opsLimit
            )
            apiClient.deleteAccount(sessionToken, accountKeyManager.deriveLoginKey(passwordKey), code)
        }
        clearLocalAccount()
    }

    private suspend fun endCurrentSessionOnServer() {
        val sessionToken = accountStorage.getSessionToken() ?: return
        val currentSession = apiClient.listSessions(sessionToken).firstOrNull { it.isCurrent } ?: return
        apiClient.signOutSession(sessionToken, currentSession.id)
    }

    private suspend fun <Answer> withStoredSessionToken(call: suspend (String) -> Answer): Answer {
        val sessionToken = checkNotNull(accountStorage.getSessionToken()) { MISSING_ACCOUNT_MESSAGE }
        return try {
            call(sessionToken)
        } catch (error: CloudApiException) {
            if (error.code == CloudApiErrorCode.INVALID_TOKEN) clearLocalAccount()
            throw error
        }
    }

    private suspend fun derivePasswordKeyOffMainThread(
        password: String,
        salt: ByteArray,
        memLimit: Long,
        opsLimit: Int
    ): ByteArray = withContext(Dispatchers.Default) {
        accountKeyManager.derivePasswordKey(password, salt, memLimit, opsLimit)
    }

    private suspend fun saveSignedInAccount(
        email: String,
        sessionToken: String,
        masterKey: ByteArray,
        privateKey: ByteArray
    ) {
        accountStorage.saveMasterKey(masterKey)
        accountStorage.savePrivateKey(privateKey)
        accountStorage.saveEmail(email)
        accountStorage.saveSessionToken(sessionToken)
        mutableSignInState.value = CloudSignInState.SignedIn(email)
    }

    private suspend fun saveEmail(email: String) {
        accountStorage.saveEmail(email)
        mutableSignInState.value = CloudSignInState.SignedIn(email)
    }

    suspend fun clearLocalAccount() {
        accountStorage.clearAll()
        mutableSignInState.value = CloudSignInState.SignedOut
        forgetCloudSyncRecords()
    }

    private companion object {
        const val MISSING_ACCOUNT_MESSAGE = "This device has no saved Emberr Cloud account"
    }
}
