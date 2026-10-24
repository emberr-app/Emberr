package com.emberr.domain.cloud.api

import com.emberr.domain.cloud.crypto.CloudEncryptedBytes
import com.emberr.domain.cloud.keys.CloudAccountKeys
import com.emberr.domain.cloud.keys.CloudItemType
import com.emberr.domain.cloud.keys.CloudPasswordKeys
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloudApiClientTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun clientAnswering(handler: MockRequestHandler) = CloudApiClient(
        httpClient = HttpClient(MockEngine(handler)) { configureForEmberrCloud() },
        serverAddress = { "http://localhost:8080" },
        appVersion = "1.2.1"
    )

    private fun accountKeysWithOneByteValuePerField() = CloudAccountKeys(
        salt = ByteArray(16) { 1 },
        memLimit = 67108864,
        opsLimit = 3,
        encryptedMasterKey = ByteArray(48) { 2 },
        encryptedMasterKeyNonce = ByteArray(24) { 3 },
        masterKeyEncryptedWithRecoveryKey = ByteArray(48) { 4 },
        masterKeyEncryptedWithRecoveryKeyNonce = ByteArray(24) { 5 },
        recoveryKeyEncryptedWithMasterKey = ByteArray(48) { 6 },
        recoveryKeyEncryptedWithMasterKeyNonce = ByteArray(24) { 7 },
        publicKey = ByteArray(32) { 8 },
        encryptedPrivateKey = ByteArray(48) { 9 },
        encryptedPrivateKeyNonce = ByteArray(24) { 10 }
    )

    @Test
    fun signupSendsTheServerFieldNamesInStandardPaddedBase64AndReadsTheToken() = runTest {
        var methodSent: HttpMethod? = null
        var pathSent: String? = null
        var clientVersionSent: String? = null
        var bodySent: JsonObject? = null
        val client = clientAnswering { request ->
            methodSent = request.method
            pathSent = request.url.encodedPath
            clientVersionSent = request.headers["X-Client-Version"]
            bodySent = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            respond("""{"token":"session-token"}""", HttpStatusCode.OK, jsonHeaders)
        }
        val keys = accountKeysWithOneByteValuePerField()

        val token = client.signUp(
            email = "a@example.com",
            code = "123456",
            deviceName = "Laptop",
            loginKey = ByteArray(32) { 0xFB.toByte() },
            keys = keys
        )

        assertEquals("session-token", token)
        assertEquals(HttpMethod.Post, methodSent)
        assertEquals("/v1/auth/signup", pathSent)
        assertEquals("1.2.1", clientVersionSent)
        val body = bodySent!!
        assertEquals(setOf("email", "code", "deviceName", "loginKey", "keys"), body.keys)
        assertEquals("a@example.com", body.getValue("email").jsonPrimitive.content)
        assertEquals("123456", body.getValue("code").jsonPrimitive.content)
        assertEquals("Laptop", body.getValue("deviceName").jsonPrimitive.content)
        assertEquals("+/v7+/v7+/v7+/v7+/v7+/v7+/v7+/v7+/v7+/v7+/s=", body.getValue("loginKey").jsonPrimitive.content)
        val keysSent = body.getValue("keys").jsonObject
        assertEquals("AQEBAQEBAQEBAQEBAQEBAQ==", keysSent.getValue("salt").jsonPrimitive.content)
        assertEquals(67108864L, keysSent.getValue("memLimit").jsonPrimitive.long)
        assertEquals(3L, keysSent.getValue("opsLimit").jsonPrimitive.long)
        val lockedFieldsSent = mapOf(
            "encryptedMasterKey" to keys.encryptedMasterKey,
            "encryptedMasterKeyNonce" to keys.encryptedMasterKeyNonce,
            "masterKeyEncryptedWithRecoveryKey" to keys.masterKeyEncryptedWithRecoveryKey,
            "masterKeyEncryptedWithRecoveryKeyNonce" to keys.masterKeyEncryptedWithRecoveryKeyNonce,
            "recoveryKeyEncryptedWithMasterKey" to keys.recoveryKeyEncryptedWithMasterKey,
            "recoveryKeyEncryptedWithMasterKeyNonce" to keys.recoveryKeyEncryptedWithMasterKeyNonce,
            "publicKey" to keys.publicKey,
            "encryptedPrivateKey" to keys.encryptedPrivateKey,
            "encryptedPrivateKeyNonce" to keys.encryptedPrivateKeyNonce
        )
        assertEquals(setOf("salt", "memLimit", "opsLimit") + lockedFieldsSent.keys, keysSent.keys)
        lockedFieldsSent.forEach { (fieldName, expectedBytes) ->
            val bytesSent = Base64.Default.decode(keysSent.getValue(fieldName).jsonPrimitive.content)
            assertContentEquals(expectedBytes, bytesSent, fieldName)
        }
    }

    @Test
    fun errorAnswersBecomeOurExceptionWithTheServerCodeOrUnknown() = runTest {
        val wrongCodeClient = clientAnswering {
            respond(
                """{"code":"INCORRECT_CODE","message":"the email code is wrong"}""",
                HttpStatusCode.Unauthorized,
                jsonHeaders
            )
        }
        val wrongCode = assertFailsWith<CloudApiException> {
            wrongCodeClient.changeEmail("session-token", "b@example.com", "000000")
        }
        assertEquals(CloudApiErrorCode.INCORRECT_CODE, wrongCode.code)
        assertEquals(401, wrongCode.httpStatus)

        val proxyClient = clientAnswering {
            respond(
                "<html><body>502 Bad Gateway</body></html>",
                HttpStatusCode.BadGateway,
                headersOf(HttpHeaders.ContentType, "text/html")
            )
        }
        val proxyError = assertFailsWith<CloudApiException> {
            proxyClient.fetchAccountDetails("session-token")
        }
        assertEquals(CloudApiErrorCode.UNKNOWN, proxyError.code)
        assertEquals(502, proxyError.httpStatus)
    }

    @Test
    fun paramsKeepsAPlusSignedInCallsSendTheTokenAndAMissingCodeIsLeftOut() = runTest {
        var emailTheServerReads: String? = null
        var paramsPathSent: String? = null
        val paramsClient = clientAnswering { request ->
            paramsPathSent = request.url.encodedPath
            emailTheServerReads = request.url.encodedQuery
                .split("&")
                .first { it.startsWith("email=") }
                .removePrefix("email=")
                .decodeURLQueryComponent(plusIsSpace = true)
            respond("""{"salt":"AQEBAQEBAQEBAQEBAQEBAQ==","memLimit":67108864,"opsLimit":3}""", HttpStatusCode.OK, jsonHeaders)
        }

        val params = paramsClient.fetchPasswordParams("a+b@example.com")

        assertEquals("/v1/auth/params", paramsPathSent)
        assertEquals("a+b@example.com", emailTheServerReads)
        assertContentEquals(ByteArray(16) { 1 }, params.salt)
        assertEquals(67108864L, params.memLimit)
        assertEquals(3, params.opsLimit)

        var signOutMethodSent: HttpMethod? = null
        var signOutPathSent: String? = null
        var tokenSent: String? = null
        val signedInClient = clientAnswering { request ->
            signOutMethodSent = request.method
            signOutPathSent = request.url.encodedPath
            tokenSent = request.headers["X-Auth-Token"]
            respondOk()
        }

        signedInClient.signOutSession(sessionToken = "session-token", sessionId = "AbC-_123")

        assertEquals(HttpMethod.Delete, signOutMethodSent)
        assertEquals("/v1/auth/sessions/AbC-_123", signOutPathSent)
        assertEquals("session-token", tokenSent)

        var changePasswordBodySent: JsonObject? = null
        val changePasswordClient = clientAnswering { request ->
            changePasswordBodySent = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            respondOk()
        }

        changePasswordClient.changePassword(
            sessionToken = "recovered-session-token",
            loginKey = ByteArray(32),
            keys = CloudPasswordKeys(
                salt = ByteArray(16),
                memLimit = 67108864,
                opsLimit = 3,
                encryptedMasterKey = ByteArray(48),
                encryptedMasterKeyNonce = ByteArray(24)
            ),
            code = null
        )

        assertEquals(setOf("loginKey", "keys", "logOutOtherDevices"), changePasswordBodySent!!.keys)
    }

    @Test
    fun blobCommitSendsOnlyTheBlobFieldsToTheItemPathAndReadsTheNewRevision() = runTest {
        var methodSent: HttpMethod? = null
        var pathSent: String? = null
        var tokenSent: String? = null
        var bodySent: JsonObject? = null
        val client = clientAnswering { request ->
            methodSent = request.method
            pathSent = request.url.encodedPath
            tokenSent = request.headers["X-Auth-Token"]
            bodySent = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            respond("""{"revision":12}""", HttpStatusCode.OK, jsonHeaders)
        }

        val result = client.commitBlobItem(
            sessionToken = "session-token",
            spaceRef = 7,
            itemId = "note-1",
            baseRevision = 11,
            itemType = CloudItemType.NOTE,
            payloadVersion = 1,
            parentId = "folder-1",
            lockedKey = CloudEncryptedBytes(ByteArray(48) { 2 }, ByteArray(24) { 3 }),
            objectKey = "items/7/note-1/ABC",
            blobNonce = ByteArray(24) { 4 },
            sizeBytes = 2048
        )

        assertEquals(12L, assertIs<CloudWriteResult.Saved>(result).revision)
        assertEquals(HttpMethod.Put, methodSent)
        assertEquals("/v1/spaces/7/items/note-1", pathSent)
        assertEquals("session-token", tokenSent)
        val body = bodySent!!
        assertEquals(
            setOf(
                "baseRevision", "itemType", "payloadVersion", "parentId", "encryptedKey", "encryptedKeyNonce",
                "objectKey", "blobNonce", "sizeBytes"
            ),
            body.keys
        )
        assertEquals("NOTE", body.getValue("itemType").jsonPrimitive.content)
        assertEquals(11L, body.getValue("baseRevision").jsonPrimitive.long)
        assertEquals(2048L, body.getValue("sizeBytes").jsonPrimitive.long)
        assertContentEquals(ByteArray(24) { 4 }, Base64.Default.decode(body.getValue("blobNonce").jsonPrimitive.content))
    }

    @Test
    fun aConflictAnswerBecomesAConflictResultWithTheCurrentItemOrNone() = runTest {
        val lockedKey = Base64.Default.encode(ByteArray(48) { 2 })
        val lockedKeyNonce = Base64.Default.encode(ByteArray(24) { 3 })
        val blobNonce = Base64.Default.encode(ByteArray(24) { 4 })
        val conflictWithItemClient = clientAnswering {
            respond(
                """{"code":"CONFLICT","message":"the item changed on the server","item":{""" +
                    """"itemId":"note-1","itemType":"NOTE","payloadVersion":1,"parentId":"folder-1",""" +
                    """"encryptedKey":"$lockedKey","encryptedKeyNonce":"$lockedKeyNonce",""" +
                    """"inlinePayload":null,"inlinePayloadNonce":null,"blobNonce":"$blobNonce",""" +
                    """"downloadUrl":"http://localhost:3900/emberr/items/7/note-1/ABC?X-Amz-Expires=900\u0026X-Amz-Signature=abc",""" +
                    """"revision":15,"sizeBytes":2048,"deleted":false,"updatedAt":1761300000000000}}""",
                HttpStatusCode.Conflict,
                jsonHeaders
            )
        }

        val conflict = conflictWithItemClient.commitInlineItem(
            sessionToken = "session-token",
            spaceRef = 7,
            itemId = "note-1",
            baseRevision = 11,
            itemType = CloudItemType.FOLDER,
            payloadVersion = 1,
            parentId = null,
            lockedKey = null,
            encryptedPayload = CloudEncryptedBytes(ByteArray(40) { 5 }, ByteArray(24) { 6 })
        )

        val currentItem = assertIs<CloudWriteResult.Conflict>(conflict).currentItem!!
        assertEquals("note-1", currentItem.itemId)
        assertEquals(CloudItemType.NOTE, currentItem.itemType)
        assertEquals("folder-1", currentItem.parentId)
        assertContentEquals(ByteArray(48) { 2 }, currentItem.lockedKey!!.cipherText)
        assertContentEquals(ByteArray(24) { 3 }, currentItem.lockedKey!!.nonce)
        assertNull(currentItem.inlinePayload)
        assertContentEquals(ByteArray(24) { 4 }, currentItem.blobNonce)
        assertEquals("http://localhost:3900/emberr/items/7/note-1/ABC?X-Amz-Expires=900&X-Amz-Signature=abc", currentItem.downloadUrl)
        assertEquals(15L, currentItem.revision)
        assertEquals(2048L, currentItem.sizeBytes)
        assertFalse(currentItem.deleted)
        assertEquals(1761300000000000L, currentItem.updatedAtMicroseconds)

        val conflictWithoutItemClient = clientAnswering {
            respond(
                """{"code":"CONFLICT","message":"the item changed on the server"}""",
                HttpStatusCode.Conflict,
                jsonHeaders
            )
        }

        val conflictWithoutItem = conflictWithoutItemClient.deleteItem("session-token", 7, "note-1", baseRevision = 11)

        assertNull(assertIs<CloudWriteResult.Conflict>(conflictWithoutItem).currentItem)
    }

    @Test
    fun changesReadResyncRequiredAndATombstoneWithEveryOptionalFieldNull() = runTest {
        val resyncClient = clientAnswering {
            respond("""{"resyncRequired":true,"latest":1290}""", HttpStatusCode.OK, jsonHeaders)
        }

        val resync = resyncClient.fetchChanges("session-token", spaceRef = 7, since = 3)

        assertEquals(1290L, assertIs<CloudChanges.ResyncRequired>(resync).latest)

        var querySent: String? = null
        val tombstoneClient = clientAnswering { request ->
            querySent = request.url.encodedQuery
            respond(
                """{"items":[{"itemId":"folder-1","itemType":"FOLDER","payloadVersion":1,"parentId":null,""" +
                    """"encryptedKey":null,"encryptedKeyNonce":null,"inlinePayload":null,"inlinePayloadNonce":null,""" +
                    """"blobNonce":null,"downloadUrl":null,"revision":1043,"sizeBytes":0,"deleted":true,""" +
                    """"updatedAt":1761300000000000}],"latest":1043,"hasMore":false}""",
                HttpStatusCode.OK,
                jsonHeaders
            )
        }

        val page = assertIs<CloudChanges.Page>(tombstoneClient.fetchChanges("session-token", spaceRef = 7, since = 1042))

        assertEquals("since=1042", querySent)
        assertEquals(1043L, page.latest)
        assertFalse(page.hasMore)
        val tombstone = page.items.single()
        assertEquals("folder-1", tombstone.itemId)
        assertEquals(CloudItemType.FOLDER, tombstone.itemType)
        assertTrue(tombstone.deleted)
        assertNull(tombstone.parentId)
        assertNull(tombstone.lockedKey)
        assertNull(tombstone.inlinePayload)
        assertNull(tombstone.blobNonce)
        assertNull(tombstone.downloadUrl)
    }
}
