@file:OptIn(ExperimentalUnsignedTypes::class)

package com.emberr.domain.cloud.account

import com.emberr.domain.cloud.api.AccountKeysJson
import com.emberr.domain.cloud.api.CloudApiClient
import com.emberr.domain.cloud.api.cloudApiJson
import com.emberr.domain.cloud.api.configureForEmberrCloud
import com.emberr.domain.cloud.api.toCloudAccountKeys
import com.emberr.domain.cloud.crypto.CloudCrypto
import com.emberr.domain.cloud.keys.CloudAccountKeyManager
import com.emberr.domain.cloud.keys.CloudRecoveryWords
import com.ionspin.kotlin.crypto.box.Box
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class CloudAccountRepositoryTest {

    private val cloudCrypto = CloudCrypto()
    private val accountKeyManager = CloudAccountKeyManager(cloudCrypto)

    private fun deviceUsing(server: FakeCloudServer, storage: InMemoryCloudAccountStorage) = CloudAccountRepository(
        apiClient = CloudApiClient(
            httpClient = HttpClient(MockEngine(server.handler)) { configureForEmberrCloud() },
            serverAddress = { "http://localhost:8080" },
            appVersion = "1.2.1"
        ),
        accountKeyManager = accountKeyManager,
        recoveryWords = CloudRecoveryWords(cloudCrypto),
        cloudCrypto = cloudCrypto,
        accountStorage = storage,
        deviceName = "Test device"
    )

    @Test
    fun loggingInOnASecondDeviceUnlocksTheSameKeysAsSignup() = runTest {
        val server = FakeCloudServer()
        val deviceAStorage = InMemoryCloudAccountStorage()
        val deviceBStorage = InMemoryCloudAccountStorage()
        val deviceA = deviceUsing(server, deviceAStorage)
        val deviceB = deviceUsing(server, deviceBStorage)

        val words = deviceA.signUp("a@example.com", "123456", "correct horse")
        deviceB.logIn("a@example.com", "correct horse")

        assertEquals(24, words.split(" ").size)
        assertEquals(server.loginKeySentAtSignup, server.loginKeySentAtLogin)
        assertContentEquals(deviceAStorage.storedMasterKey, deviceBStorage.storedMasterKey)
        assertContentEquals(deviceAStorage.storedPrivateKey, deviceBStorage.storedPrivateKey)
        assertEquals(FakeCloudServer.LOGIN_TOKEN, deviceBStorage.storedSessionToken)
        assertEquals(CloudSignInState.SignedIn("a@example.com"), deviceB.signInState.value)
    }

    @Test
    fun recoveryOpensTheSealedTokenAndTheNewPasswordUnlocksTheSameMasterKey() = runTest {
        val server = FakeCloudServer()
        val deviceAStorage = InMemoryCloudAccountStorage()
        val recoveringDeviceStorage = InMemoryCloudAccountStorage()
        val deviceA = deviceUsing(server, deviceAStorage)
        val recoveringDevice = deviceUsing(server, recoveringDeviceStorage)

        val words = deviceA.signUp("a@example.com", "123456", "forgotten password")
        server.recoveredTokenBytes = cloudCrypto.randomBytes(32)
        val pendingRecovery = recoveringDevice.recover("a@example.com", "111111", words)
        assertNull(recoveringDeviceStorage.storedSessionToken)
        recoveringDevice.finishRecovery(pendingRecovery, "new password")

        val expectedToken = Base64.getUrlEncoder().encodeToString(server.recoveredTokenBytes)
        assertEquals(expectedToken, recoveringDeviceStorage.storedSessionToken)
        assertEquals(expectedToken, server.tokenSentAtChangePassword)
        assertFalse(server.wasCodeSentAtChangePassword)
        val keysAfterChange = cloudApiJson.decodeFromJsonElement(AccountKeysJson.serializer(), server.accountKeys!!)
            .toCloudAccountKeys()
        val newPasswordKey = accountKeyManager.derivePasswordKey(
            "new password",
            keysAfterChange.salt,
            keysAfterChange.memLimit,
            keysAfterChange.opsLimit
        )
        val masterKeyFromNewPassword = accountKeyManager.unlockMasterKeyWithPasswordKey(newPasswordKey, keysAfterChange)
        assertContentEquals(deviceAStorage.storedMasterKey, masterKeyFromNewPassword)
        assertContentEquals(deviceAStorage.storedMasterKey, recoveringDeviceStorage.storedMasterKey)
        assertContentEquals(deviceAStorage.storedPrivateKey, recoveringDeviceStorage.storedPrivateKey)
    }
}

private class FakeCloudServer {
    var accountKeys: JsonObject? = null
    var loginKeySentAtSignup: String? = null
    var loginKeySentAtLogin: String? = null
    var tokenSentAtChangePassword: String? = null
    var wasCodeSentAtChangePassword = false
    var recoveredTokenBytes = ByteArray(0)

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    val handler: MockRequestHandler = { request ->
        val body = request.body.toByteArray().decodeToString()
        val bodyJson = if (body.isEmpty()) JsonObject(emptyMap()) else Json.parseToJsonElement(body).jsonObject
        fun bodyText(fieldName: String) = bodyJson.getValue(fieldName).jsonPrimitive.content
        fun answer(json: String) = respond(json, HttpStatusCode.OK, jsonHeaders)
        fun wrongLoginKey() = respond("""{"code":"INVALID_PASSWORD"}""", HttpStatusCode.Unauthorized, jsonHeaders)

        when (request.url.encodedPath) {
            "/v1/auth/signup" -> {
                accountKeys = bodyJson.getValue("keys").jsonObject
                loginKeySentAtSignup = bodyText("loginKey")
                answer("""{"token":"$SIGNUP_TOKEN"}""")
            }
            "/v1/auth/params" -> {
                val keys = accountKeys!!
                answer(
                    buildJsonObject {
                        put("salt", keys.getValue("salt"))
                        put("memLimit", keys.getValue("memLimit"))
                        put("opsLimit", keys.getValue("opsLimit"))
                    }.toString()
                )
            }
            "/v1/auth/login" -> {
                loginKeySentAtLogin = bodyText("loginKey")
                if (loginKeySentAtLogin != loginKeySentAtSignup) wrongLoginKey()
                else answer("""{"token":"$LOGIN_TOKEN","keys":${accountKeys}}""")
            }
            "/v1/auth/recover" -> {
                val keys = accountKeys!!
                val publicKey = Base64.getDecoder().decode(keys.getValue("publicKey").jsonPrimitive.content)
                val sealedToken = Box.seal(recoveredTokenBytes.asUByteArray(), publicKey.asUByteArray()).asByteArray()
                val recoveryKeyFieldNames = listOf(
                    "masterKeyEncryptedWithRecoveryKey",
                    "masterKeyEncryptedWithRecoveryKeyNonce",
                    "publicKey",
                    "encryptedPrivateKey",
                    "encryptedPrivateKeyNonce"
                )
                answer(
                    buildJsonObject {
                        put("encryptedToken", JsonPrimitive(Base64.getEncoder().encodeToString(sealedToken)))
                        put("keys", JsonObject(keys.filterKeys { it in recoveryKeyFieldNames }))
                    }.toString()
                )
            }
            "/v1/auth/change-password" -> {
                tokenSentAtChangePassword = request.headers["X-Auth-Token"]
                wasCodeSentAtChangePassword = "code" in bodyJson
                accountKeys = JsonObject(accountKeys!! + bodyJson.getValue("keys").jsonObject)
                answer("")
            }
            else -> respond("""{"code":"NOT_FOUND"}""", HttpStatusCode.NotFound, jsonHeaders)
        }
    }

    companion object {
        const val SIGNUP_TOKEN = "signup-token"
        const val LOGIN_TOKEN = "login-token"
    }
}

private class InMemoryCloudAccountStorage : CloudAccountStorage {
    var storedSessionToken: String? = null
    var storedMasterKey: ByteArray? = null
    var storedPrivateKey: ByteArray? = null
    var storedEmail: String? = null

    override suspend fun saveSessionToken(sessionToken: String) {
        storedSessionToken = sessionToken
    }

    override suspend fun getSessionToken(): String? = storedSessionToken

    override suspend fun saveMasterKey(masterKey: ByteArray) {
        storedMasterKey = masterKey
    }

    override suspend fun getMasterKey(): ByteArray? = storedMasterKey

    override suspend fun savePrivateKey(privateKey: ByteArray) {
        storedPrivateKey = privateKey
    }

    override suspend fun getPrivateKey(): ByteArray? = storedPrivateKey

    override suspend fun saveEmail(email: String) {
        storedEmail = email
    }

    override suspend fun getEmail(): String? = storedEmail

    override suspend fun clearAll() {
        storedSessionToken = null
        storedMasterKey = null
        storedPrivateKey = null
        storedEmail = null
    }
}
