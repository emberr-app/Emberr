package com.emberr.domain.cloud.sync

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CloudItemEntity
import com.emberr.data.local.room.entity.DEFAULT_SPACE_ID
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.SelfHostDeletedNoteEntity
import com.emberr.domain.ai.chat.ChatSessionRepository
import com.emberr.domain.canvas.CanvasRepository
import com.emberr.domain.cloud.api.CloudApiClient
import com.emberr.domain.cloud.api.CloudBlobTransfer
import com.emberr.domain.cloud.api.configureForCloudBlobTransfer
import com.emberr.domain.cloud.api.configureForEmberrCloud
import com.emberr.domain.cloud.crypto.CloudCrypto
import com.emberr.domain.cloud.keys.CloudDailyNoteIds
import com.emberr.domain.cloud.keys.CloudItemKeyManager
import com.emberr.domain.cloud.keys.CloudItemType
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.space.ActiveSpaceStore
import com.emberr.domain.space.SpaceRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CloudNoteSyncRefusedUploadsTest {

    private val appDatabase = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val cloudCrypto = CloudCrypto()
    private val itemKeyManager = CloudItemKeyManager(cloudCrypto)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val requestsSent = mutableListOf<String>()

    private val fullStorageServer: MockRequestHandler = { request ->
        val path = request.url.encodedPath
        requestsSent += "${request.method.value} ${request.url.host}$path"
        when {
            request.url.host == "storage.test" -> respondOk()
            path.endsWith("/upload-urls") -> respond(
                """{"objectKey":"items/2/note/1","uploadUrl":"http://storage.test/piece","expiresAt":1}""",
                HttpStatusCode.OK,
                jsonHeaders
            )
            request.method == HttpMethod.Put -> respond(
                """{"code":"STORAGE_LIMIT_EXCEEDED"}""",
                HttpStatusCode.UpgradeRequired,
                jsonHeaders
            )
            request.method == HttpMethod.Delete -> respond("""{"revision":6}""", HttpStatusCode.OK, jsonHeaders)
            else -> error("The note pass should not call $path")
        }
    }

    private val noteRepositoryThatIsNeverUsed: NoteRepository = stubOf { methodName ->
        error("This test does not expect NoteRepository.$methodName to be called")
    }

    private val noteSync = CloudNoteSync(
        apiClient = CloudApiClient(
            httpClient = HttpClient(MockEngine(fullStorageServer)) { configureForEmberrCloud() },
            serverAddress = { "http://localhost:8080" },
            appVersion = "1.2.1"
        ),
        blobTransfer = CloudBlobTransfer(HttpClient(MockEngine(fullStorageServer)) { configureForCloudBlobTransfer() }),
        itemKeyManager = itemKeyManager,
        dailyNoteIds = CloudDailyNoteIds(cloudCrypto),
        cloudItemDao = appDatabase.cloudItemDao(),
        noteDao = appDatabase.noteDao(),
        blockDao = appDatabase.blockDao(),
        folderDao = appDatabase.folderDao(),
        selfHostDeletedNoteDao = appDatabase.selfHostDeletedNoteDao(),
        noteRepository = noteRepositoryThatIsNeverUsed,
        spaceRepository = SpaceRepository(
            spaceDao = appDatabase.spaceDao(),
            activeSpaceStore = ActiveSpaceStore(stubOf<SettingsManager> { methodName ->
                if (methodName == "getActiveSpaceIdFlow") flowOf(DEFAULT_SPACE_ID) else error("Unexpected $methodName")
            }),
            noteRepository = noteRepositoryThatIsNeverUsed,
            chatSessionRepository = stubOf<ChatSessionRepository> { methodName -> error("Unexpected $methodName") }
        ),
        canvasRepository = CanvasRepository(appDatabase.canvasDao(), appDatabase.noteDao())
    )

    @AfterTest
    fun closeDatabase() {
        appDatabase.close()
    }

    private inline fun <reified T : Any> stubOf(crossinline answer: (methodName: String) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { stub, method, arguments ->
            when (method.name) {
                "toString" -> "stub of ${T::class.simpleName}"
                "hashCode" -> System.identityHashCode(stub)
                "equals" -> stub === arguments?.firstOrNull()
                else -> answer(method.name)
            }
        } as T

    private fun changedNote(noteId: String) = NoteMetadataEntity(
        noteId = noteId,
        title = noteId,
        folderId = null,
        isDaily = false,
        dateString = null,
        createdAt = 100L,
        updatedAt = 200L,
        filePath = ""
    )

    @Test
    fun aFullStorageAnswerStopsLaterNoteCommitsButADeletionStillGoesUp() = runTest {
        val noteDao = appDatabase.noteDao()
        noteDao.insertOrUpdateMetadata(changedNote("first"))
        noteDao.insertOrUpdateMetadata(changedNote("second"))
        appDatabase.selfHostDeletedNoteDao().upsertTombstone(
            SelfHostDeletedNoteEntity(noteId = "gone", isDaily = false, dateString = null, deletedAt = 300L)
        )
        val liveRowOfDeletedNote = CloudItemEntity(
            spaceRef = 2L,
            itemId = "gone",
            itemType = CloudItemType.NOTE.name,
            localId = "gone",
            parentId = DEFAULT_SPACE_ID,
            encryptedKey = null,
            encryptedKeyNonce = null,
            revision = 5L,
            syncedUpdatedAt = 50L,
            isDeleted = false
        )
        val streams = listOf(
            CloudStream(
                appSpaceId = ACCOUNT_SPACE_ID,
                spaceRef = 1L,
                lastRevisionSeen = 0L,
                rowsByItemId = mutableMapOf(),
                openKeys = mutableMapOf(ACCOUNT_SPACE_ID to itemKeyManager.newItemKey())
            ),
            CloudStream(
                appSpaceId = DEFAULT_SPACE_ID,
                spaceRef = 2L,
                lastRevisionSeen = 0L,
                rowsByItemId = mutableMapOf("gone" to liveRowOfDeletedNote),
                openKeys = mutableMapOf(DEFAULT_SPACE_ID to itemKeyManager.newItemKey())
            )
        )
        val counts = SyncPassCounts()

        noteSync.pushNotes(sessionToken = "session-token", streams = streams, counts = counts)

        assertEquals(1, requestsSent.count { it.endsWith("/upload-urls") })
        assertEquals(1, requestsSent.count { it.startsWith("PUT localhost/v1/spaces/2/items/") })
        assertTrue("DELETE localhost/v1/spaces/2/items/gone" in requestsSent)
        assertEquals(CloudSyncResult.UploadsRefused(CloudUploadsRefusedReason.STORAGE_FULL), counts.passResult())
        assertEquals(0L, noteDao.getNoteById("first")?.cloudSyncedAt)
        assertEquals(0L, noteDao.getNoteById("second")?.cloudSyncedAt)
        assertTrue(appDatabase.cloudItemDao().getItemsInSpace(2L).single { it.itemId == "gone" }.isDeleted)
    }
}
