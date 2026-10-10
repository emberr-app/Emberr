package com.emberr.domain.importer

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.DEFAULT_SPACE_ID
import com.emberr.database.EmberrDatabase
import com.emberr.domain.ai.LocalAiEngine
import com.emberr.domain.ai.NoteIndexer
import com.emberr.domain.ai.external.AiSettingsRepository
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.TextBlock
import com.emberr.domain.repository.NoteRepositoryImpl
import com.emberr.domain.space.ActiveSpaceStore
import com.emberr.domain.util.media.MediaInfo
import com.emberr.domain.util.media.MediaStorageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.io.IOException
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val IMPORT_SPACE_ID = "imported-space"

class MarkdownNotesImporterTest {

    private val appDatabase = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val aiIndexDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { EmberrDatabase.Schema.create(it) }

    private val settings: SettingsManager = stubOf { methodName, _ ->
        when (methodName) {
            "getActiveSpaceId" -> DEFAULT_SPACE_ID
            "getActiveSpaceIdFlow" -> flowOf(DEFAULT_SPACE_ID)
            "isAiFeaturesDisabled" -> true
            else -> error("These tests do not expect SettingsManager.$methodName to be called")
        }
    }
    private val aiSettingsThatAreNeverRead: AiSettingsRepository = stubOf { methodName, _ ->
        error("These tests do not expect AiSettingsRepository.$methodName to be called")
    }

    private val repository = NoteRepositoryImpl(
        activeSpaceStore = ActiveSpaceStore(settings),
        noteDao = appDatabase.noteDao(),
        folderDao = appDatabase.folderDao(),
        blockDao = appDatabase.blockDao(),
        noteIndexer = NoteIndexer(EmberrDatabase(aiIndexDriver), LocalAiEngine(aiSettingsThatAreNeverRead), settings),
        calendarTaskDao = appDatabase.calendarTaskDao(),
        calendarEventExceptionDao = appDatabase.calendarEventExceptionDao(),
        imageBlockDao = appDatabase.imageBlockDao(),
        documentBlockDao = appDatabase.documentBlockDao(),
        bookmarkBlockDao = appDatabase.bookmarkBlockDao(),
        categoryDao = appDatabase.categoryDao(),
        propertyTagDao = appDatabase.propertyTagDao(),
        customPropertyDao = appDatabase.customPropertyDao(),
        selfHostDeletedNoteDao = appDatabase.selfHostDeletedNoteDao(),
        mediaReferenceDao = appDatabase.mediaReferenceDao(),
        canvasDao = appDatabase.canvasDao()
    )

    private val mediaStorage = InMemoryMediaStorage()

    @AfterTest
    fun closeDatabases() {
        appDatabase.close()
        aiIndexDriver.close()
    }

    private class InMemoryMediaStorage : MediaStorageHelper {
        val savedFileNames = mutableListOf<String>()

        override suspend fun copyUriToInternalStorage(uriString: String): MediaInfo? =
            error("The importer is not expected to copy from a uri")

        override suspend fun saveBytesToInternalStorage(bytes: ByteArray, extension: String, mimeType: String): MediaInfo {
            val fileName = "media_${savedFileNames.size + 1}.$extension"
            savedFileNames += fileName
            return MediaInfo(localFileName = fileName, originalName = fileName, mimeType = mimeType, sizeBytes = bytes.size.toLong())
        }

        override fun getAbsoluteMediaPath(fileName: String): String = fileName

        override fun listAllMediaFileNames(): List<String> = savedFileNames.toList()
    }

    private class FakeImportFile(override val relativePath: String, private val content: String?) : ImportFile {
        override suspend fun readBytes(): ByteArray = content?.encodeToByteArray() ?: throw IOException("Cannot read $relativePath")
    }

    private fun pickedFiles(vararg files: Pair<String, String?>, onClose: () -> Unit = {}) = PickedImportFiles(
        pickedNames = listOf("vault.zip"),
        files = files.map { (path, content) -> FakeImportFile(path, content) },
        skippedNames = emptyList(),
        releaseResources = onClose
    )

    private fun importer(appScope: CoroutineScope) =
        MarkdownNotesImporter(repository = repository, mediaStorageHelper = mediaStorage, appScope = appScope)

    private suspend fun importedNotes() =
        appDatabase.noteDao().getAllNotesForBackup().filter { it.spaceId == IMPORT_SPACE_ID }

    private inline fun <reified T : Any> stubOf(
        crossinline answer: (methodName: String, arguments: Array<out Any?>) -> Any?
    ): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { stub, method, arguments ->
            when (method.name) {
                "toString" -> "stub of ${T::class.simpleName}"
                "hashCode" -> System.identityHashCode(stub)
                "equals" -> stub === arguments?.firstOrNull()
                else -> answer(method.name, arguments ?: emptyArray())
            }
        } as T

    @Test
    fun foldersNotesLinksAndAttachmentsAreSavedIntoTheChosenSpace() = runTest {
        val result = importer(backgroundScope).importFiles(
            pickedFiles(
                "Vault/Work/Plan.md" to "# Plan\n\n[[Ideas]]\n\n![chart](images/chart.png)\n\n[Report](files/report.pdf)",
                "Vault/Work/images/chart.png" to "png bytes",
                "Vault/Work/files/report.pdf" to "pdf bytes",
                "Vault/Ideas.md" to "Some ideas",
                "journal.txt" to "first line"
            ),
            spaceId = IMPORT_SPACE_ID
        )

        assertEquals(
            ImportResult(
                spaceId = IMPORT_SPACE_ID,
                importedNoteCount = 3,
                createdFolderCount = 2,
                copiedAttachmentCount = 2,
                failedNames = emptyList()
            ),
            result
        )

        val folders = appDatabase.folderDao().getAllFolders(IMPORT_SPACE_ID).first()
        val vault = folders.single { it.name == "Vault" }
        val work = folders.single { it.name == "Work" }
        assertNull(vault.parentFolderId)
        assertEquals(vault.folderId, work.parentFolderId)

        val notes = importedNotes()
        val plan = notes.single { it.title == "Plan" }
        val ideas = notes.single { it.title == "Ideas" }
        val journal = notes.single { it.title == "journal" }
        assertEquals(work.folderId, plan.folderId)
        assertEquals(vault.folderId, ideas.folderId)
        assertNull(journal.folderId)

        val planBlocks = repository.getNoteContent(plan.noteId)!!.blocks
        assertEquals(ideas.noteId, (planBlocks[0] as LinkedNoteBlock).linkedNoteId)
        assertEquals("media_1.png", (planBlocks[1] as ImageBlock).localFilePath)
        val report = planBlocks[2] as DocumentBlock
        assertEquals("media_2.pdf", report.localFilePath)
        assertEquals("report.pdf", report.fileName)
        assertEquals("application/pdf", report.mimeType)
        assertEquals(listOf("media_1.png", "media_2.pdf"), mediaStorage.savedFileNames)
    }

    @Test
    fun frontMatterAndTagsBecomePropertiesSavedInTheChosenSpace() = runTest {
        importer(backgroundScope).importFiles(
            pickedFiles(
                "Plan.md" to "---\ntags: [project]\nstatus: draft\nauthor: Ann\ncreated: 2026-10-01\n---\nWorking on the #importer.",
                "Other.md" to "---\nauthor: Ben\n---\nMore text"
            ),
            spaceId = IMPORT_SPACE_ID
        )

        val plan = importedNotes().single { it.title == "Plan" }
        val properties = repository.getNoteContent(plan.noteId)!!.blocks.filterIsInstance<PropertyBlock>()
        assertEquals(listOf("Tags", "Status", "author"), properties.map { it.label })
        assertEquals(listOf("project", "importer"), properties[0].tags)
        assertEquals(listOf("draft"), properties[1].tags)
        assertEquals("Ann", properties[2].text)

        val customProperty = appDatabase.customPropertyDao().getAllProperties(IMPORT_SPACE_ID).first().single()
        assertEquals("author", customProperty.name)
        assertEquals(customProperty.propertyId, properties[2].customPropertyId)

        val savedTags = appDatabase.propertyTagDao().getAllTagsOnce(IMPORT_SPACE_ID).map { it.propertyKey to it.name }.toSet()
        assertEquals(setOf("TAGS" to "project", "TAGS" to "importer", "STATUS" to "draft"), savedTags)

        val expectedCreatedAt = LocalDateTime(2026, 10, 1, 0, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        assertEquals(expectedCreatedAt, plan.createdAt)
    }

    @Test
    fun anAttachmentUsedByTwoNotesIsCopiedOnce() = runTest {
        val result = importer(backgroundScope).importFiles(
            pickedFiles(
                "First.md" to "![](shared.png)",
                "Second.md" to "![](shared.png)",
                "shared.png" to "png bytes"
            ),
            spaceId = IMPORT_SPACE_ID
        )

        assertEquals(1, result.copiedAttachmentCount)
        assertEquals(listOf("media_1.png"), mediaStorage.savedFileNames)
    }

    @Test
    fun anAttachmentThatCannotBeReadIsReportedAndItsBlockShowsTheFileName() = runTest {
        val result = importer(backgroundScope).importFiles(
            pickedFiles("Note.md" to "![](broken.png)", "broken.png" to null),
            spaceId = IMPORT_SPACE_ID
        )

        assertEquals(listOf("broken.png"), result.failedNames)
        assertEquals(1, result.importedNoteCount)
        val note = importedNotes().single()
        assertEquals("broken.png", (repository.getNoteContent(note.noteId)!!.blocks.single() as TextBlock).text)
    }

    @Test
    fun aNoteThatCannotBeReadIsReportedAndTheOthersAreStillImported() = runTest {
        val result = importer(backgroundScope).importFiles(
            pickedFiles("Good.md" to "Fine", "Bad.md" to null),
            spaceId = IMPORT_SPACE_ID
        )

        assertEquals(1, result.importedNoteCount)
        assertEquals(listOf("Bad.md"), result.failedNames)
        assertEquals(listOf("Good"), importedNotes().map { it.title })
    }

    @Test
    fun aStartedImportFinishesWithItsResultAndClosesThePickedFiles() = runTest {
        var closed = false
        val importer = importer(backgroundScope)

        importer.startImport(pickedFiles("Note.md" to "Hello", onClose = { closed = true }), spaceId = IMPORT_SPACE_ID)
        val finished = importer.progress.first { it is ImportProgress.Finished } as ImportProgress.Finished

        assertEquals(1, finished.result.importedNoteCount)
        assertTrue(closed)
    }
}
