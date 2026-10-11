package com.emberr.domain.importer

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.CanvasNodeType
import com.emberr.data.local.room.entity.CanvasSide
import com.emberr.data.local.room.entity.DEFAULT_SPACE_ID
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.database.EmberrDatabase
import com.emberr.domain.ai.LocalAiEngine
import com.emberr.domain.ai.NoteIndexer
import com.emberr.domain.ai.external.AiSettingsRepository
import com.emberr.domain.canvas.CanvasRepository
import com.emberr.domain.model.BookmarkBlock
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.PropertyValueType
import com.emberr.domain.model.TextBlock
import com.emberr.domain.reminders.ReminderRescheduler
import com.emberr.domain.reminders.ReminderScheduler
import com.emberr.domain.repository.NoteRepositoryImpl
import com.emberr.domain.space.ActiveSpaceStore
import com.emberr.domain.util.media.MediaInfo
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.domain.util.media.WebImageFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import okio.Buffer
import okio.Source
import okio.buffer
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

    private val canvasRepository = CanvasRepository(canvasDao = appDatabase.canvasDao(), noteDao = appDatabase.noteDao())

    private val mediaStorage = InMemoryMediaStorage()

    @AfterTest
    fun closeDatabases() {
        appDatabase.close()
        aiIndexDriver.close()
    }

    private class InMemoryMediaStorage : MediaStorageHelper {
        val savedFileNames = mutableListOf<String>()
        val savedTextsByFileName = mutableMapOf<String, String>()
        var whileCopying: () -> Unit = {}

        override suspend fun copyUriToInternalStorage(uriString: String): MediaInfo? =
            error("The importer is not expected to copy from a uri")

        val fileNamesSavedFromBytes = mutableListOf<String>()

        override suspend fun saveBytesToInternalStorage(bytes: ByteArray, extension: String, mimeType: String): MediaInfo {
            val fileName = "media_${savedFileNames.size + 1}.$extension"
            savedFileNames += fileName
            fileNamesSavedFromBytes += fileName
            return MediaInfo(localFileName = fileName, originalName = fileName, mimeType = mimeType, sizeBytes = bytes.size.toLong())
        }

        override suspend fun saveStreamToInternalStorage(
            openSource: () -> Source,
            extension: String,
            mimeType: String,
            onBytesCopied: (copiedBytes: Long) -> Unit
        ): MediaInfo? {
            val bytes = try {
                openSource().buffer().use { source -> source.readByteArray() }
            } catch (_: IOException) {
                return null
            }
            onBytesCopied(bytes.size / 2L)
            whileCopying()
            onBytesCopied(bytes.size.toLong())
            val fileName = "media_${savedFileNames.size + 1}.$extension"
            savedFileNames += fileName
            savedTextsByFileName[fileName] = bytes.decodeToString()
            return MediaInfo(localFileName = fileName, originalName = fileName, mimeType = mimeType, sizeBytes = bytes.size.toLong())
        }

        override fun getAbsoluteMediaPath(fileName: String): String = fileName

        override fun listAllMediaFileNames(): List<String> = savedFileNames.toList()

        override suspend fun deleteMediaFile(fileName: String) {
            savedFileNames -= fileName
            savedTextsByFileName -= fileName
        }
    }

    private class FakeImportFile(
        override val relativePath: String,
        private val content: String?,
        override val lastModifiedAt: Long? = null
    ) : ImportFile {
        override val sizeBytes: Long = content?.encodeToByteArray()?.size?.toLong() ?: 0L

        override suspend fun readBytes(): ByteArray = content?.encodeToByteArray() ?: throw IOException("Cannot read $relativePath")

        override fun openSource(): Source = content?.let { Buffer().writeUtf8(it) } ?: throw IOException("Cannot read $relativePath")
    }

    private fun pickedFiles(
        vararg files: Pair<String, String?>,
        fileDatesByPath: Map<String, Long> = emptyMap(),
        onClose: () -> Unit = {}
    ) = PickedImportFiles(
        pickedNames = listOf("vault.zip"),
        files = files.map { (path, content) -> FakeImportFile(path, content, fileDatesByPath[path]) },
        skippedNames = emptyList(),
        releaseResources = onClose
    )

    private val webImages = FakeWebImageFetcher()

    private val scheduledReminderTexts = mutableListOf<String>()
    private val reminderScheduler: ReminderScheduler = stubOf { methodName, arguments ->
        when (methodName) {
            "schedule" -> scheduledReminderTexts += arguments[2] as String
            "cancel" -> Unit
            else -> error("These tests do not expect ReminderScheduler.$methodName to be called")
        }
        Unit
    }

    private class FakeWebImageFetcher : WebImageFetcher {
        val savedImagesByUrl = mutableMapOf<String, MediaInfo>()
        val requestedUrls = mutableListOf<String>()

        override suspend fun fetchIntoMedia(url: String): MediaInfo? {
            requestedUrls += url
            return savedImagesByUrl[url]
        }
    }

    private fun importer(appScope: CoroutineScope, webImageFetcher: WebImageFetcher = webImages) =
        MarkdownNotesImporter(
            repository = repository,
            canvasRepository = canvasRepository,
            mediaStorageHelper = mediaStorage,
            webImageFetcher = webImageFetcher,
            reminderRescheduler = ReminderRescheduler(
                calendarTaskDao = appDatabase.calendarTaskDao(),
                calendarEventExceptionDao = appDatabase.calendarEventExceptionDao(),
                noteDao = appDatabase.noteDao(),
                reminderScheduler = reminderScheduler
            ),
            appScope = appScope
        )

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
        assertEquals("png bytes", mediaStorage.savedTextsByFileName["media_1.png"])
        assertEquals(emptyList(), mediaStorage.fileNamesSavedFromBytes)
    }

    @Test
    fun anObsidianCanvasBecomesACanvasNoteInItsFolderAndNotesCanLinkToIt() = runTest {
        val roadmapJson = """
            {
              "nodes": [
                {"id": "group", "type": "group", "label": "Ideas", "x": -150, "y": -100, "width": 900, "height": 700},
                {"id": "start", "type": "text", "text": "**Start** here", "x": -100, "y": -50, "width": 250, "height": 60, "color": "4"},
                {"id": "plan", "type": "file", "file": "Vault/Plan.md", "subpath": "#Goals", "x": 300, "y": -50, "width": 250, "height": 60},
                {"id": "cat", "type": "file", "file": "images/cat.png", "x": -100, "y": 100, "width": 300, "height": 200},
                {"id": "broken", "type": "file", "file": "images/broken.png", "x": 300, "y": 100, "width": 300, "height": 200}
              ],
              "edges": [
                {"id": "edge", "fromNode": "start", "fromSide": "right", "toNode": "plan", "toSide": "left", "label": "then"}
              ]
            }
        """.trimIndent()

        val result = importer(backgroundScope).importFiles(
            pickedFiles(
                "Vault/Boards/Roadmap.canvas" to roadmapJson,
                "Vault/Plan.md" to "[[Roadmap.canvas]]\n\n![[Roadmap.canvas]]",
                "Vault/images/cat.png" to "png bytes",
                "Vault/images/broken.png" to null,
                "Vault/Broken.canvas" to "{ not a canvas"
            ),
            spaceId = IMPORT_SPACE_ID
        )

        assertEquals(
            ImportResult(
                spaceId = IMPORT_SPACE_ID,
                importedNoteCount = 2,
                createdFolderCount = 2,
                copiedAttachmentCount = 1,
                failedNames = listOf("Vault/Broken.canvas", "Vault/images/broken.png")
            ),
            result
        )

        val boards = appDatabase.folderDao().getAllFolders(IMPORT_SPACE_ID).first().single { it.name == "Boards" }
        val notes = importedNotes()
        val roadmap = notes.single { it.title == "Roadmap" }
        val plan = notes.single { it.title == "Plan" }
        assertEquals(NoteKind.CANVAS, roadmap.kind)
        assertEquals(boards.folderId, roadmap.folderId)
        assertEquals(2, notes.size)

        val planBlocks = repository.getNoteContent(plan.noteId)!!.blocks
        assertEquals(listOf(roadmap.noteId, roadmap.noteId), planBlocks.map { (it as LinkedNoteBlock).linkedNoteId })

        val canvas = canvasRepository.loadCanvasIncludingDeleted(roadmap.noteId)
        val group = canvas.nodes.single { it.type == CanvasNodeType.GROUP }
        val startCard = canvas.nodes.single { it.text == "Start here" }
        val planCard = canvas.nodes.single { it.text == "Plan › Goals" }
        val catImage = canvas.nodes.single { it.type == CanvasNodeType.IMAGE }
        val brokenImageCard = canvas.nodes.single { it.text == "broken.png" }
        assertEquals("Ideas", group.text)
        assertEquals(40f to 40f, group.x to group.y)
        assertEquals(80f to 80f, startCard.x to startCard.y)
        assertEquals("green", startCard.color)
        assertEquals("media_1.png", catImage.imagePath)
        assertEquals(CanvasNodeType.TEXT, brokenImageCard.type)
        assertEquals(5, canvas.nodes.size)

        val edge = canvas.edges.single()
        assertEquals(startCard.nodeId to CanvasSide.RIGHT, edge.fromNodeId to edge.fromSide)
        assertEquals(planCard.nodeId to CanvasSide.LEFT, edge.toNodeId to edge.toSide)
        assertEquals("then", edge.label)
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
    fun notesAndCanvasesWithoutAFrontMatterDateAreCreatedAtTheirFileDate() = runTest {
        val fileDate = LocalDateTime(2024, 3, 5, 10, 20).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val importStartedAt = System.currentTimeMillis()

        importer(backgroundScope).importFiles(
            pickedFiles(
                "Old.md" to "Written long ago",
                "Dated.md" to "---\ncreated: 2026-10-01\n---\nHas its own date",
                "Board.canvas" to "{\"nodes\": [], \"edges\": []}",
                "Future.md" to "The file clock was wrong",
                "Undated.md" to "No file date",
                fileDatesByPath = mapOf(
                    "Old.md" to fileDate,
                    "Dated.md" to fileDate,
                    "Board.canvas" to fileDate,
                    "Future.md" to importStartedAt + 86_400_000L
                )
            ),
            spaceId = IMPORT_SPACE_ID
        )
        val importFinishedAt = System.currentTimeMillis()

        val notesByTitle = importedNotes().associateBy { it.title }
        val frontMatterDate = LocalDateTime(2026, 10, 1, 0, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        assertEquals(fileDate, notesByTitle.getValue("Old").createdAt)
        assertEquals(frontMatterDate, notesByTitle.getValue("Dated").createdAt)
        assertEquals(fileDate, notesByTitle.getValue("Board").createdAt)
        assertTrue(notesByTitle.getValue("Future").createdAt in importStartedAt..importFinishedAt)
        assertTrue(notesByTitle.getValue("Undated").createdAt in importStartedAt..importFinishedAt)
        assertTrue(notesByTitle.values.all { note -> note.updatedAt in importStartedAt..importFinishedAt })
    }

    @Test
    fun htmlImagesAndIframesAreImportedLikeMarkdownImagesAndLinks() = runTest {
        webImages.savedImagesByUrl["https://example.com/logo.png"] =
            MediaInfo(localFileName = "media_web.png", originalName = "media_web.png", mimeType = "image/png", sizeBytes = 9)

        val result = importer(backgroundScope).importFiles(
            pickedFiles(
                "Page.md" to "<img src=\"https://example.com/logo.png\" alt=\"Logo\">\n\n" +
                    "See <img src=\"pics/cat.png\" alt=\"Cat\"> here\n\n" +
                    "<img src=\"https://example.com/gone.png\" alt=\"Gone\">\n\n" +
                    "<iframe src=\"https://www.youtube.com/embed/xyz\"></iframe>",
                "pics/cat.png" to "png bytes"
            ),
            spaceId = IMPORT_SPACE_ID
        )

        val blocks = repository.getNoteContent(importedNotes().single { it.title == "Page" }.noteId)!!.blocks
        assertEquals("media_web.png", (blocks[0] as ImageBlock).localFilePath)
        assertEquals("See", (blocks[1] as TextBlock).text)
        assertEquals("png bytes", mediaStorage.savedTextsByFileName[(blocks[2] as ImageBlock).localFilePath])
        assertNull((blocks[2] as ImageBlock).sourceUrl)
        assertEquals("here", (blocks[3] as TextBlock).text)
        val fallback = blocks[4] as BookmarkBlock
        assertEquals("https://example.com/gone.png" to "Gone", fallback.url to fallback.title)
        assertEquals("https://www.youtube.com/embed/xyz", (blocks[5] as BookmarkBlock).url)
        assertEquals(6, blocks.size)
        assertEquals(emptyList(), result.failedNames)
    }

    @Test
    fun webImagesAreDownloadedOnceAndBecomeBookmarksWhenTheyCannotBe() = runTest {
        webImages.savedImagesByUrl["https://example.com/logo.png"] =
            MediaInfo(localFileName = "media_web.png", originalName = "media_web.png", mimeType = "image/png", sizeBytes = 9)

        val result = importer(backgroundScope).importFiles(
            pickedFiles(
                "First.md" to "![logo](https://example.com/logo.png)\n\n![gone](https://example.com/gone.png)",
                "Second.md" to "![logo](https://example.com/logo.png)"
            ),
            spaceId = IMPORT_SPACE_ID
        )

        val firstBlocks = repository.getNoteContent(importedNotes().single { it.title == "First" }.noteId)!!.blocks
        assertEquals("media_web.png", (firstBlocks[0] as ImageBlock).localFilePath)
        assertEquals("https://example.com/logo.png", (firstBlocks[0] as ImageBlock).sourceUrl)
        val fallback = firstBlocks[1] as BookmarkBlock
        assertEquals("https://example.com/gone.png", fallback.url)
        assertEquals("gone", fallback.title)
        assertEquals(listOf("https://example.com/logo.png", "https://example.com/gone.png"), webImages.requestedUrls)
        assertEquals(1, result.copiedAttachmentCount)
        assertEquals(emptyList(), result.failedNames)
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
    fun theProgressMovesWhileABigAttachmentIsCopied() = runTest {
        val importer = importer(backgroundScope)
        val progressSeenWhileCopying = mutableListOf<ImportProgress>()
        mediaStorage.whileCopying = { progressSeenWhileCopying += importer.progress.value }

        importer.importFiles(
            pickedFiles("Note.md" to "![](video.mp4)", "video.mp4" to "x".repeat(10_000)),
            spaceId = IMPORT_SPACE_ID
        )

        val halfwayThroughTheVideo = progressSeenWhileCopying.single() as ImportProgress.Importing
        assertEquals("video.mp4", halfwayThroughTheVideo.copyingFileName)
        assertTrue(halfwayThroughTheVideo.doneFraction in 0.4f..0.6f)
    }

    @Test
    fun filesThatNoNoteLinksToAreGatheredInAnUnlinkedAttachmentsNote() = runTest {
        val result = importer(backgroundScope).importFiles(
            pickedFiles(
                "Vault/Notes/Plan.md" to "![[chart.png]]",
                "Vault/Files/chart.png" to "chart bytes",
                "Vault/Files/tax-2023.pdf" to "pdf bytes",
                "Vault/Files/scan.jpg" to "jpg bytes",
                "Vault/song.mp3" to "mp3 bytes"
            ),
            spaceId = IMPORT_SPACE_ID
        )

        assertEquals(1, result.importedNoteCount)
        assertEquals(4, result.copiedAttachmentCount)
        assertEquals(emptyList(), result.failedNames)

        val vault = appDatabase.folderDao().getAllFolders(IMPORT_SPACE_ID).first().single { it.name == "Vault" }
        val unlinked = importedNotes().single { it.title == "Unlinked attachments" }
        assertEquals(vault.folderId, unlinked.folderId)

        val blocks = repository.getNoteContent(unlinked.noteId)!!.blocks
        assertEquals("Vault/Files", (blocks[0] as HeadingBlock).text)
        assertEquals("jpg bytes", mediaStorage.savedTextsByFileName[(blocks[1] as ImageBlock).localFilePath])
        assertEquals("tax-2023.pdf", (blocks[2] as DocumentBlock).fileName)
        assertEquals("Vault", (blocks[3] as HeadingBlock).text)
        val song = blocks[4] as DocumentBlock
        assertEquals("song.mp3" to "audio/mpeg", song.fileName to song.mimeType)
        assertEquals(5, blocks.size)
    }

    @Test
    fun anImportWithoutUnlinkedFilesAddsNoExtraNote() = runTest {
        importer(backgroundScope).importFiles(
            pickedFiles("Note.md" to "![](a.png)", "a.png" to "png bytes"),
            spaceId = IMPORT_SPACE_ID
        )

        assertEquals(listOf("Note"), importedNotes().map { it.title })
    }

    @Test
    fun aStartedImportArmsTheRemindersOfImportedTasks() = runTest {
        val importer = importer(backgroundScope)

        importer.startImport(
            pickedFiles("Chores.md" to "- [ ] Pay rent 📅 2099-11-01\n- [ ] Old bill 📅 2001-01-01\n- [x] Done already 📅 2099-11-01"),
            spaceId = IMPORT_SPACE_ID
        )
        importer.progress.first { it is ImportProgress.Finished }

        assertEquals(listOf("Pay rent"), scheduledReminderTexts)
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

    @Test
    fun stoppingAnImportRemovesWhatItAddedAndKeepsWhatWasThereBefore() = runTest {
        val existingAuthor = repository.createCustomPropertyInSpace(IMPORT_SPACE_ID, "author", PropertyValueType.TEXT)
        repository.createPropertyTagsInSpace(IMPORT_SPACE_ID, "TAGS", listOf("project"))
        val webImageSaver = object : WebImageFetcher {
            override suspend fun fetchIntoMedia(url: String): MediaInfo =
                mediaStorage.saveBytesToInternalStorage("web bytes".encodeToByteArray(), "png", "image/png")
        }
        val importer = importer(backgroundScope, webImageFetcher = webImageSaver)
        mediaStorage.whileCopying = { if (mediaStorage.fileNamesSavedFromBytes.isNotEmpty()) importer.stopImport() }
        val boardJson = """
            {
              "nodes": [
                {"id": "start", "type": "text", "text": "Start", "x": 0, "y": 0, "width": 200, "height": 60},
                {"id": "end", "type": "text", "text": "End", "x": 300, "y": 0, "width": 200, "height": 60}
              ],
              "edges": [
                {"id": "edge", "fromNode": "start", "toNode": "end"}
              ]
            }
        """.trimIndent()

        importer.startImport(
            pickedFiles(
                "Vault/Work/Plan.md" to "---\ntags: [project, idea]\nauthor: Ann\nmood: calm\n---\n![](chart.png)\n\n" +
                    "![](https://example.com/photo.png)\n\n- [ ] Call 📅 2030-01-01",
                "Vault/Work/chart.png" to "png bytes",
                "Vault/Board.canvas" to boardJson,
                "Vault/Later.md" to "![](video.mp4)",
                "Vault/video.mp4" to "video bytes"
            ),
            spaceId = IMPORT_SPACE_ID
        )
        importer.progress.first { it == ImportProgress.Stopped }

        assertEquals(emptyList(), importedNotes())
        assertEquals(emptyList(), appDatabase.folderDao().getAllFolders(IMPORT_SPACE_ID).first())
        assertEquals(emptyList(), appDatabase.canvasDao().getAllNodesForBackup())
        assertEquals(emptyList(), appDatabase.canvasDao().getAllEdgesForBackup())
        assertEquals(emptyList(), appDatabase.calendarTaskDao().getAllTasksAcrossSpaces())
        assertEquals(
            listOf(existingAuthor.propertyId),
            appDatabase.customPropertyDao().getAllProperties(IMPORT_SPACE_ID).first().map { it.propertyId }
        )
        assertEquals(
            listOf("TAGS" to "project"),
            appDatabase.propertyTagDao().getAllTagsOnce(IMPORT_SPACE_ID).map { it.propertyKey to it.name }
        )
        assertEquals(emptyList(), mediaStorage.savedFileNames)
    }
}
