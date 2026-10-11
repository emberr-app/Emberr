package com.emberr.domain.backup.manual

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.DEFAULT_SPACE_ID
import com.emberr.data.local.room.entity.NoteBlockEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.database.EmberrDatabase
import com.emberr.domain.ai.LocalAiEngine
import com.emberr.domain.ai.NoteIndexer
import com.emberr.domain.ai.external.AiSettingsRepository
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteBlockSerializer
import com.emberr.domain.model.TextBlock
import com.emberr.domain.reminders.ReminderRescheduler
import com.emberr.domain.reminders.ReminderScheduler
import com.emberr.domain.repository.NoteRepositoryImpl
import com.emberr.domain.space.ActiveSpaceStore
import com.emberr.domain.util.media.MediaInfo
import com.emberr.domain.util.media.MediaStorageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.Source
import java.io.IOException
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EmberrBackupImporterTest {

    private val sharedNoteId = "11111111-2222-4333-8444-555555555555"

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
    private val reminderScheduler: ReminderScheduler = stubOf { _, _ -> Unit }

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

    private val mediaOnDevice = mutableSetOf<String>()
    private val deletedMediaFileNames = mutableListOf<String>()

    private val mediaStorage = object : MediaStorageHelper {
        override suspend fun copyUriToInternalStorage(uriString: String): MediaInfo? = error("Not expected")
        override suspend fun saveBytesToInternalStorage(bytes: ByteArray, extension: String, mimeType: String): MediaInfo? =
            error("Not expected")
        override suspend fun saveStreamToInternalStorage(
            openSource: () -> Source,
            extension: String,
            mimeType: String,
            onBytesCopied: (copiedBytes: Long) -> Unit
        ): MediaInfo? = error("Not expected")
        override fun getAbsoluteMediaPath(fileName: String): String = fileName
        override fun listAllMediaFileNames(): List<String> = mediaOnDevice.toList()
        override suspend fun deleteMediaFile(fileName: String) {
            mediaOnDevice -= fileName
            deletedMediaFileNames += fileName
        }
    }

    private val blockJson = Json { encodeDefaults = true }

    @AfterTest
    fun closeDatabases() {
        appDatabase.close()
        aiIndexDriver.close()
    }

    private inner class FakeBackupFile(
        override val backupData: EmberrBackupData,
        private val mediaInBackup: Set<String> = emptySet(),
        private val mediaThatFailsToCopy: String? = null
    ) : EmberrBackupFile {
        override val pickedName = "backup.emberr"
        var isClosed = false

        override suspend fun copyMediaFileIfMissing(fileName: String): BackupMediaCopyOutcome = when {
            fileName == mediaThatFailsToCopy -> throw IOException("The disk is full")
            fileName in mediaOnDevice -> BackupMediaCopyOutcome.ALREADY_ON_DEVICE
            fileName in mediaInBackup -> {
                mediaOnDevice += fileName
                BackupMediaCopyOutcome.COPIED
            }
            else -> BackupMediaCopyOutcome.NOT_IN_BACKUP
        }

        override fun close() {
            isClosed = true
        }
    }

    private fun importer(appScope: CoroutineScope) = EmberrBackupImporter(
        appDatabase = appDatabase,
        repository = repository,
        mediaStorageHelper = mediaStorage,
        reminderRescheduler = ReminderRescheduler(
            calendarTaskDao = appDatabase.calendarTaskDao(),
            calendarEventExceptionDao = appDatabase.calendarEventExceptionDao(),
            noteDao = appDatabase.noteDao(),
            reminderScheduler = reminderScheduler
        ),
        appScope = appScope
    )

    private fun personalSpace() = SpaceEntity(spaceId = DEFAULT_SPACE_ID, displayName = "Personal", createdAt = 1L, updatedAt = 1L)

    private fun note(noteId: String, title: String) = NoteMetadataEntity(
        noteId = noteId,
        title = title,
        folderId = null,
        isDaily = false,
        dateString = null,
        createdAt = 1L,
        updatedAt = 1L,
        filePath = ""
    )

    private fun stored(noteId: String, block: NoteBlock, displayOrder: Int = 0) = NoteBlockEntity(
        blockId = block.id,
        noteId = noteId,
        displayOrder = displayOrder,
        blockDataJson = blockJson.encodeToString(NoteBlockSerializer, block),
        updatedAt = 1L,
        isDeleted = false
    )

    private fun backupWithImages(vararg imageFileNames: String) = EmberrBackupData(
        exportTimestamp = 1L,
        spaces = listOf(personalSpace()),
        notes = listOf(note(sharedNoteId, "Trip")),
        blocks = imageFileNames.mapIndexed { index, fileName ->
            stored(sharedNoteId, ImageBlock(id = "image-$index", localFilePath = "/media/$fileName"), displayOrder = index)
        }
    )

    private suspend fun putLocalNoteWithTheSameIdOnThisDevice() {
        appDatabase.backupDao().insertSpaces(listOf(personalSpace()))
        appDatabase.backupDao().insertNotes(listOf(note(sharedNoteId, "Local note")))
        appDatabase.backupDao().insertBlocks(listOf(stored(sharedNoteId, TextBlock(id = "local-text", text = "mine"))))
    }

    @Test
    fun importedSpacesAreAddedNextToTheExistingOnesWithoutTouchingThem() = runTest {
        putLocalNoteWithTheSameIdOnThisDevice()

        val result = importer(backgroundScope).importSpaces(
            FakeBackupFile(backupWithImages("photo.jpg"), mediaInBackup = setOf("photo.jpg")),
            setOf(DEFAULT_SPACE_ID)
        )

        val spaces = appDatabase.spaceDao().getAllSpacesOnce()
        assertEquals(listOf("Personal", "Personal (Imported)"), spaces.map { it.displayName }.sorted())
        val importedSpaceId = spaces.single { it.displayName == "Personal (Imported)" }.spaceId
        val notes = appDatabase.noteDao().getAllNotesForBackup()
        assertEquals("Local note", notes.single { it.noteId == sharedNoteId }.title)
        assertEquals(DEFAULT_SPACE_ID, notes.single { it.noteId == sharedNoteId }.spaceId)
        val importedNote = notes.single { it.title == "Trip" }
        assertEquals(importedSpaceId, importedNote.spaceId)
        assertEquals(listOf("local-text"), appDatabase.blockDao().getAllBlocksForNoteIncludingDeleted(sharedNoteId).map { it.blockId })
        assertEquals(1, appDatabase.blockDao().getAllBlocksForNoteIncludingDeleted(importedNote.noteId).size)
        assertEquals(
            listOf(importedNote.noteId),
            appDatabase.imageBlockDao().getAllImagesAcrossSpacesFlow().first().map { it.noteId }
        )
        assertEquals(EmberrImportResult(listOf("Personal (Imported)"), missingMediaCount = 0), result)
    }

    @Test
    fun importingTheSameBackupTwiceAddsTwoSeparateCopies() = runTest {
        val importer = importer(backgroundScope)
        val backup = backupWithImages("photo.jpg")

        importer.importSpaces(FakeBackupFile(backup, mediaInBackup = setOf("photo.jpg")), setOf(DEFAULT_SPACE_ID))
        importer.importSpaces(FakeBackupFile(backup, mediaInBackup = setOf("photo.jpg")), setOf(DEFAULT_SPACE_ID))

        assertEquals(2, appDatabase.spaceDao().getAllSpacesOnce().size)
        assertEquals(2, appDatabase.noteDao().getAllNotesForBackup().map { it.noteId }.toSet().size)
        assertTrue(appDatabase.noteDao().getAllNotesForBackup().none { it.noteId == sharedNoteId })
    }

    @Test
    fun onlyMediaThatIsMissingOnThisDeviceIsCopied() = runTest {
        mediaOnDevice += "already-here.jpg"

        val result = importer(backgroundScope).importSpaces(
            FakeBackupFile(backupWithImages("already-here.jpg", "new.jpg", "lost.jpg"), mediaInBackup = setOf("already-here.jpg", "new.jpg")),
            setOf(DEFAULT_SPACE_ID)
        )

        assertEquals(setOf("already-here.jpg", "new.jpg"), mediaOnDevice)
        assertEquals(1, result.missingMediaCount)
    }

    @Test
    fun whenCopyingFailsNothingIsSavedAndOnlyNewlyCopiedMediaIsRemoved() = runTest {
        mediaOnDevice += "already-here.jpg"

        assertFailsWith<IOException> {
            importer(backgroundScope).importSpaces(
                FakeBackupFile(
                    backupWithImages("already-here.jpg", "1-copied.jpg", "2-broken.jpg"),
                    mediaInBackup = setOf("1-copied.jpg", "2-broken.jpg"),
                    mediaThatFailsToCopy = "2-broken.jpg"
                ),
                setOf(DEFAULT_SPACE_ID)
            )
        }

        assertEquals(emptyList(), appDatabase.spaceDao().getAllSpacesOnce())
        assertEquals(emptyList(), appDatabase.noteDao().getAllNotesForBackup())
        assertEquals(listOf("1-copied.jpg"), deletedMediaFileNames)
        assertEquals(setOf("already-here.jpg"), mediaOnDevice)
    }

    @Test
    fun startImportFinishesAndClosesTheBackupFile() = runTest {
        val importer = importer(backgroundScope)
        val backupFile = FakeBackupFile(backupWithImages("photo.jpg"), mediaInBackup = setOf("photo.jpg"))

        importer.startImport(backupFile, setOf(DEFAULT_SPACE_ID))
        val finished = importer.progress.first { it is EmberrImportProgress.Finished || it is EmberrImportProgress.Failed }

        assertEquals(
            EmberrImportProgress.Finished(EmberrImportResult(listOf("Personal (Imported)"), missingMediaCount = 0)),
            finished
        )
        assertTrue(backupFile.isClosed)
    }

    @Test
    fun startImportReportsAFailureAndStillClosesTheBackupFile() = runTest {
        val importer = importer(backgroundScope)
        val backupFile = FakeBackupFile(backupWithImages("broken.jpg"), mediaInBackup = setOf("broken.jpg"), mediaThatFailsToCopy = "broken.jpg")

        importer.startImport(backupFile, setOf(DEFAULT_SPACE_ID))
        val finished = importer.progress.first { it is EmberrImportProgress.Finished || it is EmberrImportProgress.Failed }

        assertEquals(EmberrImportProgress.Failed("The disk is full"), finished)
        assertTrue(backupFile.isClosed)
    }

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
}
