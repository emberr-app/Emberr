package com.emberr.domain.cloud.api

import com.emberr.domain.cloud.crypto.CloudEncryptedBytes
import com.emberr.domain.cloud.keys.CloudAccountKeys
import com.emberr.domain.cloud.keys.CloudItemType
import com.emberr.domain.cloud.keys.CloudPasswordKeys
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val CLIENT_VERSION_HEADER = "X-Client-Version"
private const val AUTH_TOKEN_HEADER = "X-Auth-Token"
private const val REQUEST_TIMEOUT_MS = 30_000L
private const val CONNECT_TIMEOUT_MS = 15_000L
private const val SOCKET_TIMEOUT_MS = 30_000L

internal val cloudApiJson = Json { ignoreUnknownKeys = true }

internal fun HttpClientConfig<*>.configureForEmberrCloud() {
    expectSuccess = false
    install(ContentNegotiation) {
        json(cloudApiJson)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        socketTimeoutMillis = SOCKET_TIMEOUT_MS
    }
}

class CloudApiClient(
    private val httpClient: HttpClient,
    private val serverAddress: () -> String,
    private val appVersion: String
) {

    suspend fun sendEmailCode(email: String, purpose: CloudEmailCodePurpose) {
        send(HttpMethod.Post, "auth", "email-code") {
            jsonBody(EmailCodeRequest(email = email, purpose = purpose.wireValue))
        }
    }

    suspend fun fetchPasswordParams(email: String): CloudPasswordParams {
        val response = send(HttpMethod.Get, "auth", "params") {
            parameter("email", email)
        }
        val answer = response.readAnswer<PasswordParamsResponse>()
        return response.decodeBase64Fields {
            CloudPasswordParams(
                salt = answer.salt.fromBase64(),
                memLimit = answer.memLimit,
                opsLimit = answer.opsLimit
            )
        }
    }

    suspend fun signUp(
        email: String,
        code: String,
        deviceName: String,
        loginKey: ByteArray,
        keys: CloudAccountKeys
    ): String {
        val answer = send(HttpMethod.Post, "auth", "signup") {
            jsonBody(
                SignupRequest(
                    email = email,
                    code = code,
                    deviceName = deviceName,
                    loginKey = loginKey.toBase64(),
                    keys = keys.toJson()
                )
            )
        }.readAnswer<SignupResponse>()
        return answer.token
    }

    suspend fun logIn(email: String, loginKey: ByteArray, deviceName: String): CloudSignedInDevice {
        val response = send(HttpMethod.Post, "auth", "login") {
            jsonBody(LoginRequest(email = email, loginKey = loginKey.toBase64(), deviceName = deviceName))
        }
        val answer = response.readAnswer<LoginResponse>()
        return response.decodeBase64Fields {
            CloudSignedInDevice(token = answer.token, keys = answer.keys.toCloudAccountKeys())
        }
    }

    suspend fun recover(email: String, code: String, deviceName: String): CloudRecoveryStart {
        val response = send(HttpMethod.Post, "auth", "recover") {
            jsonBody(RecoverRequest(email = email, code = code, deviceName = deviceName))
        }
        val answer = response.readAnswer<RecoverResponse>()
        return response.decodeBase64Fields {
            CloudRecoveryStart(
                encryptedToken = answer.encryptedToken.fromBase64(),
                keys = answer.keys.toCloudRecoveryKeys()
            )
        }
    }

    suspend fun listSessions(sessionToken: String): List<CloudSession> {
        val answer = send(HttpMethod.Get, "auth", "sessions", sessionToken = sessionToken)
            .readAnswer<SessionsResponse>()
        return answer.sessions.map { it.toCloudSession() }
    }

    suspend fun signOutSession(sessionToken: String, sessionId: String) {
        send(HttpMethod.Delete, "auth", "sessions", sessionId, sessionToken = sessionToken)
    }

    suspend fun fetchAccountDetails(sessionToken: String): CloudAccountDetails {
        val response = send(HttpMethod.Get, "account", sessionToken = sessionToken)
        val answer = response.readAnswer<AccountDetailsResponse>()
        return response.decodeBase64Fields {
            CloudAccountDetails(
                email = answer.email,
                storageUsedBytes = answer.storageUsedBytes,
                keys = answer.keys.toCloudAccountKeys()
            )
        }
    }

    suspend fun deleteAccount(sessionToken: String, loginKey: ByteArray, code: String) {
        send(HttpMethod.Delete, "account", sessionToken = sessionToken) {
            jsonBody(DeleteAccountRequest(loginKey = loginKey.toBase64(), code = code))
        }
    }

    suspend fun sendChangeEmailCode(sessionToken: String, newEmail: String) {
        send(HttpMethod.Post, "account", "email-code", sessionToken = sessionToken) {
            jsonBody(AccountEmailCodeRequest(purpose = "change", email = newEmail))
        }
    }

    suspend fun sendDeleteAccountCode(sessionToken: String) {
        send(HttpMethod.Post, "account", "email-code", sessionToken = sessionToken) {
            jsonBody(AccountEmailCodeRequest(purpose = "delete"))
        }
    }

    suspend fun sendPasswordChangeCode(sessionToken: String) {
        send(HttpMethod.Post, "account", "email-code", sessionToken = sessionToken) {
            jsonBody(AccountEmailCodeRequest(purpose = "password"))
        }
    }

    suspend fun changeEmail(sessionToken: String, newEmail: String, code: String) {
        send(HttpMethod.Post, "auth", "change-email", sessionToken = sessionToken) {
            jsonBody(ChangeEmailRequest(email = newEmail, code = code))
        }
    }

    suspend fun changePassword(
        sessionToken: String,
        loginKey: ByteArray,
        keys: CloudPasswordKeys,
        code: String?,
        logOutOtherDevices: Boolean = true
    ) {
        send(HttpMethod.Post, "auth", "change-password", sessionToken = sessionToken) {
            jsonBody(
                ChangePasswordRequest(
                    loginKey = loginKey.toBase64(),
                    keys = keys.toJson(),
                    logOutOtherDevices = logOutOtherDevices,
                    code = code
                )
            )
        }
    }

    suspend fun registerSpace(
        sessionToken: String,
        clientSpaceId: String,
        kind: CloudSpaceKind,
        lockedSpaceKey: CloudEncryptedBytes
    ): CloudRegisteredSpace {
        val response = send(HttpMethod.Post, "spaces", sessionToken = sessionToken) {
            jsonBody(
                RegisterSpaceRequest(
                    clientSpaceId = clientSpaceId,
                    kind = kind.wireValue,
                    encryptedKey = lockedSpaceKey.cipherText.toBase64(),
                    encryptedKeyNonce = lockedSpaceKey.nonce.toBase64()
                )
            )
        }
        val answer = response.readAnswer<RegisterSpaceResponse>()
        return response.decodeBase64Fields {
            CloudRegisteredSpace(
                spaceRef = answer.spaceRef,
                lockedSpaceKey = CloudEncryptedBytes(answer.encryptedKey.fromBase64(), answer.encryptedKeyNonce.fromBase64())
            )
        }
    }

    suspend fun fetchSyncState(sessionToken: String): List<CloudRemoteSpace> {
        val response = send(HttpMethod.Get, "sync", "state", sessionToken = sessionToken)
        val answer = response.readAnswer<SyncStateResponse>()
        return response.decodeBase64Fields { answer.spaces.map { it.toCloudRemoteSpace() } }
    }

    suspend fun fetchChanges(sessionToken: String, spaceRef: Long, since: Long): CloudChanges {
        val response = send(HttpMethod.Get, "spaces", spaceRef.toString(), "changes", sessionToken = sessionToken) {
            parameter("since", since)
        }
        val answer = response.readAnswer<ChangesResponse>()
        if (answer.resyncRequired) return CloudChanges.ResyncRequired(latest = answer.latest)
        return response.decodeBase64Fields {
            CloudChanges.Page(
                items = answer.items.map { it.toCloudRemoteItem() },
                latest = answer.latest,
                hasMore = answer.hasMore
            )
        }
    }

    suspend fun requestUploadUrl(sessionToken: String, spaceRef: Long, itemId: String, sizeBytes: Long): CloudUploadUrl {
        val answer = send(
            HttpMethod.Post,
            "spaces", spaceRef.toString(), "items", itemId, "upload-urls",
            sessionToken = sessionToken
        ) {
            jsonBody(UploadUrlRequest(sizeBytes = sizeBytes))
        }.readAnswer<UploadUrlResponse>()
        return CloudUploadUrl(
            objectKey = answer.objectKey,
            uploadUrl = answer.uploadUrl,
            expiresAtMicroseconds = answer.expiresAt
        )
    }

    suspend fun commitInlineItem(
        sessionToken: String,
        spaceRef: Long,
        itemId: String,
        baseRevision: Long,
        itemType: CloudItemType,
        payloadVersion: Int,
        parentId: String?,
        lockedKey: CloudEncryptedBytes?,
        encryptedPayload: CloudEncryptedBytes
    ): CloudWriteResult =
        writeItem(HttpMethod.Put, sessionToken, spaceRef, itemId) {
            jsonBody(
                CommitInlineItemRequest(
                    baseRevision = baseRevision,
                    itemType = itemType.name,
                    payloadVersion = payloadVersion,
                    parentId = parentId,
                    encryptedKey = lockedKey?.cipherText?.toBase64(),
                    encryptedKeyNonce = lockedKey?.nonce?.toBase64(),
                    inlinePayload = encryptedPayload.cipherText.toBase64(),
                    inlinePayloadNonce = encryptedPayload.nonce.toBase64()
                )
            )
        }

    suspend fun commitBlobItem(
        sessionToken: String,
        spaceRef: Long,
        itemId: String,
        baseRevision: Long,
        itemType: CloudItemType,
        payloadVersion: Int,
        parentId: String?,
        lockedKey: CloudEncryptedBytes,
        objectKey: String,
        blobNonce: ByteArray,
        sizeBytes: Long
    ): CloudWriteResult =
        writeItem(HttpMethod.Put, sessionToken, spaceRef, itemId) {
            jsonBody(
                CommitBlobItemRequest(
                    baseRevision = baseRevision,
                    itemType = itemType.name,
                    payloadVersion = payloadVersion,
                    parentId = parentId,
                    encryptedKey = lockedKey.cipherText.toBase64(),
                    encryptedKeyNonce = lockedKey.nonce.toBase64(),
                    objectKey = objectKey,
                    blobNonce = blobNonce.toBase64(),
                    sizeBytes = sizeBytes
                )
            )
        }

    suspend fun deleteItem(sessionToken: String, spaceRef: Long, itemId: String, baseRevision: Long): CloudWriteResult =
        writeItem(HttpMethod.Delete, sessionToken, spaceRef, itemId) {
            jsonBody(DeleteItemRequest(baseRevision = baseRevision))
        }

    private suspend fun writeItem(
        method: HttpMethod,
        sessionToken: String,
        spaceRef: Long,
        itemId: String,
        configureRequest: HttpRequestBuilder.() -> Unit
    ): CloudWriteResult {
        val response = sendAndKeepAnyStatus(
            method,
            "spaces", spaceRef.toString(), "items", itemId,
            sessionToken = sessionToken,
            configureRequest = configureRequest
        )
        if (response.status.isSuccess()) {
            return CloudWriteResult.Saved(revision = response.readAnswer<RevisionResponse>().revision)
        }
        val error = response.toCloudApiException()
        if (response.status != HttpStatusCode.Conflict || error.code != CloudApiErrorCode.CONFLICT) throw error
        val conflict = response.readAnswer<ConflictResponse>()
        return response.decodeBase64Fields { CloudWriteResult.Conflict(currentItem = conflict.item?.toCloudRemoteItem()) }
    }

    private suspend fun send(
        method: HttpMethod,
        vararg pathSegments: String,
        sessionToken: String? = null,
        configureRequest: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        val response = sendAndKeepAnyStatus(
            method,
            *pathSegments,
            sessionToken = sessionToken,
            configureRequest = configureRequest
        )
        if (!response.status.isSuccess()) throw response.toCloudApiException()
        return response
    }

    private suspend fun sendAndKeepAnyStatus(
        method: HttpMethod,
        vararg pathSegments: String,
        sessionToken: String?,
        configureRequest: HttpRequestBuilder.() -> Unit
    ): HttpResponse =
        try {
            httpClient.request {
                this.method = method
                url {
                    takeFrom(serverAddress())
                    appendPathSegments(listOf("v1") + pathSegments, encodeSlash = true)
                }
                header(CLIENT_VERSION_HEADER, appVersion)
                if (sessionToken != null) header(AUTH_TOKEN_HEADER, sessionToken)
                configureRequest()
            }
        } catch (_: IOException) {
            throw CloudServerUnreachableException()
        }

    private suspend fun HttpResponse.toCloudApiException(): CloudApiException {
        val errorCode = try {
            val serverCode = cloudApiJson.decodeFromString<ErrorResponse>(bodyAsText()).code
            CloudApiErrorCode.entries.firstOrNull { it.name == serverCode } ?: CloudApiErrorCode.UNKNOWN
        } catch (_: SerializationException) {
            CloudApiErrorCode.UNKNOWN
        }
        return CloudApiException(errorCode, status.value)
    }

    private suspend inline fun <reified Answer> HttpResponse.readAnswer(): Answer =
        try {
            cloudApiJson.decodeFromString<Answer>(bodyAsText())
        } catch (_: SerializationException) {
            throw CloudApiException(CloudApiErrorCode.UNKNOWN, status.value)
        }

    private inline fun <Decoded> HttpResponse.decodeBase64Fields(decode: () -> Decoded): Decoded =
        try {
            decode()
        } catch (_: IllegalArgumentException) {
            throw CloudApiException(CloudApiErrorCode.UNKNOWN, status.value)
        }
}

private inline fun <reified Body> HttpRequestBuilder.jsonBody(body: Body) {
    contentType(ContentType.Application.Json)
    setBody(body)
}
