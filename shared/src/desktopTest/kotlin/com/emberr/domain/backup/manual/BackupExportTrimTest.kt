package com.emberr.domain.backup.manual

import androidx.room.Room
import androidx.room.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.CalendarEventExceptionEntity
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.CanvasEdgeEntity
import com.emberr.data.local.room.entity.CanvasNodeEntity
import com.emberr.data.local.room.entity.CanvasNodeType
import com.emberr.data.local.room.entity.CanvasSide
import com.emberr.data.local.room.entity.CanvasStrokeEntity
import com.emberr.data.local.room.entity.CategoryEntity
import com.emberr.data.local.room.entity.ChatSessionEntity
import com.emberr.data.local.room.entity.CustomPropertyEntity
import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.ImageBlockEntity
import com.emberr.data.local.room.entity.MediaReferenceEntity
import com.emberr.data.local.room.entity.NoteBlockEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.PropertyTagEntity
import com.emberr.data.local.room.entity.SelfHostDeletedApiConfigEntity
import com.emberr.data.local.room.entity.SelfHostDeletedNoteEntity
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.data.local.room.entity.TaskSource
import com.emberr.data.local.room.entity.UnappliedSyncChangeEntity
import com.emberr.data.local.room.entity.UnusedMediaFileEntity
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteBlockSerializer
import com.emberr.domain.model.PropertyValueType
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.VoiceBlock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupExportTrimTest {

    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val backupDao = database.backupDao()
    private val blockJson = Json { encodeDefaults = true }

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    private fun space(spaceId: String, isDeleted: Boolean = false) =
        SpaceEntity(spaceId = spaceId, displayName = spaceId, createdAt = 1L, isDeleted = isDeleted)

    private fun note(
        noteId: String,
        spaceId: String,
        trashedAt: Long? = null,
        coverImagePath: String? = null,
        dailyDate: String? = null
    ) =
        NoteMetadataEntity(
            noteId = noteId,
            title = noteId,
            folderId = null,
            isDaily = dailyDate != null,
            dateString = dailyDate,
            createdAt = 1L,
            updatedAt = 1L,
            filePath = "",
            coverImagePath = coverImagePath,
            trashedAt = trashedAt,
            spaceId = spaceId
        )

    private fun stored(noteId: String, block: NoteBlock) = NoteBlockEntity(
        blockId = block.id,
        noteId = noteId,
        displayOrder = 0,
        blockDataJson = blockJson.encodeToString(NoteBlockSerializer, block),
        updatedAt = 1L,
        isDeleted = block.isDeleted
    )

    private fun canvasNode(nodeId: String, noteId: String, imagePath: String? = null, isDeleted: Boolean = false) =
        CanvasNodeEntity(
            nodeId = nodeId,
            noteId = noteId,
            x = 0f,
            y = 0f,
            width = 100f,
            height = 100f,
            text = "",
            createdAt = 1L,
            updatedAt = 1L,
            isDeleted = isDeleted,
            type = if (imagePath != null) CanvasNodeType.IMAGE else CanvasNodeType.TEXT,
            imagePath = imagePath
        )

    private fun canvasEdge(edgeId: String, noteId: String, isDeleted: Boolean = false) = CanvasEdgeEntity(
        edgeId = edgeId,
        noteId = noteId,
        fromNodeId = "node",
        fromSide = CanvasSide.RIGHT,
        toNodeId = "node",
        toSide = CanvasSide.LEFT,
        createdAt = 1L,
        updatedAt = 1L,
        isDeleted = isDeleted
    )

    private fun canvasStroke(strokeId: String, noteId: String, isDeleted: Boolean = false) = CanvasStrokeEntity(
        strokeId = strokeId,
        noteId = noteId,
        x = 0f,
        y = 0f,
        points = "",
        width = 2f,
        createdAt = 1L,
        updatedAt = 1L,
        isDeleted = isDeleted
    )

    private fun task(blockId: String, noteId: String, spaceId: String, sourceType: TaskSource = TaskSource.NOTE) =
        CalendarTaskEntity(
            blockId = blockId,
            noteId = noteId,
            text = blockId,
            isChecked = false,
            targetDate = "2026-10-11",
            reminderTimestamp = null,
            sourceType = sourceType,
            spaceId = spaceId
        )

    private suspend fun fillDatabase() {
        backupDao.insertSpaces(listOf(space("space-a"), space("space-b"), space("space-deleted", isDeleted = true)))
        backupDao.insertNotes(
            listOf(
                note("note-a", "space-a", coverImagePath = "/media/cover.png"),
                note("note-a-trashed", "space-a", trashedAt = 5L),
                note("note-b", "space-b"),
                note("daily-a", "space-a", dailyDate = "2026-10-11")
            )
        )
        backupDao.insertBlocks(
            listOf(
                stored("note-a", TextBlock(id = "a-text", text = "hello")),
                stored("note-a", ImageBlock(id = "a-image", localFilePath = "/media/kept.jpg")),
                stored("note-a", VoiceBlock(id = "a-voice", localFilePath = "voice.m4a")),
                stored("note-a", DocumentBlock(id = "a-document", localFilePath = "/media/document.pdf")),
                stored("note-a", ImageBlock(id = "a-deleted-image", localFilePath = "/media/deleted.jpg", isDeleted = true)),
                stored("note-a-trashed", ImageBlock(id = "trashed-image", localFilePath = "/media/trashed.jpg")),
                stored("note-b", ImageBlock(id = "b-image", localFilePath = "/media/other-space.jpg")),
                stored("note-a", CheckboxBlock(id = "a-task", text = "a-task")),
                stored("note-a-trashed", CheckboxBlock(id = "trashed-task", text = "trashed-task")),
                stored("note-b", CheckboxBlock(id = "b-task", text = "b-task")),
                stored("daily-a", CheckboxBlock(id = "daily-task", text = "daily-task"))
            )
        )
        backupDao.insertCanvasNodes(
            listOf(
                canvasNode("a-node", "note-a", imagePath = "/media/canvas.png"),
                canvasNode("a-deleted-node", "note-a", imagePath = "/media/deleted-canvas.png", isDeleted = true),
                canvasNode("b-node", "note-b", imagePath = "/media/other-canvas.png")
            )
        )
        backupDao.insertCanvasEdges(
            listOf(canvasEdge("a-edge", "note-a"), canvasEdge("a-deleted-edge", "note-a", isDeleted = true), canvasEdge("b-edge", "note-b"))
        )
        backupDao.insertCanvasStrokes(
            listOf(canvasStroke("a-stroke", "note-a"), canvasStroke("a-deleted-stroke", "note-a", isDeleted = true), canvasStroke("b-stroke", "note-b"))
        )
        backupDao.insertFolders(
            listOf(
                FolderEntity(folderId = "folder-a", name = "A", parentFolderId = null, createdAt = 1L, spaceId = "space-a"),
                FolderEntity(folderId = "folder-a-deleted", name = "A", parentFolderId = null, createdAt = 1L, isDeleted = true, spaceId = "space-a"),
                FolderEntity(folderId = "folder-b", name = "B", parentFolderId = null, createdAt = 1L, spaceId = "space-b")
            )
        )
        backupDao.insertCategories(
            listOf(
                CategoryEntity(categoryId = "category-a", name = "A", colorHex = "#000000", createdAt = 1L, spaceId = "space-a"),
                CategoryEntity(categoryId = "category-a-deleted", name = "A", colorHex = "#000000", createdAt = 1L, isDeleted = true, spaceId = "space-a"),
                CategoryEntity(categoryId = "category-b", name = "B", colorHex = "#000000", createdAt = 1L, spaceId = "space-b")
            )
        )
        backupDao.insertPropertyTags(
            listOf(
                PropertyTagEntity(tagId = "tag-a", propertyKey = "tags", name = "A", createdAt = 1L, spaceId = "space-a"),
                PropertyTagEntity(tagId = "tag-a-deleted", propertyKey = "tags", name = "A", createdAt = 1L, isDeleted = true, spaceId = "space-a"),
                PropertyTagEntity(tagId = "tag-b", propertyKey = "tags", name = "B", createdAt = 1L, spaceId = "space-b")
            )
        )
        backupDao.insertCustomProperties(
            listOf(
                CustomPropertyEntity(propertyId = "property-a", name = "A", valueType = PropertyValueType.TEXT, createdAt = 1L, spaceId = "space-a"),
                CustomPropertyEntity(propertyId = "property-a-deleted", name = "A", valueType = PropertyValueType.TEXT, createdAt = 1L, isDeleted = true, spaceId = "space-a"),
                CustomPropertyEntity(propertyId = "property-b", name = "B", valueType = PropertyValueType.TEXT, createdAt = 1L, spaceId = "space-b")
            )
        )
        database.calendarTaskDao().upsertTasks(
            listOf(
                task("a-task", "note-a", "space-a"),
                task("trashed-task", "note-a-trashed", "space-a"),
                task("b-task", "note-b", "space-b"),
                task("daily-task", "2026-10-11", "space-a", sourceType = TaskSource.DAILY)
            )
        )
        backupDao.insertCalendarEventExceptions(
            listOf(
                CalendarEventExceptionEntity(blockId = "a-task", occurrenceDate = "2026-10-12", updatedAt = 1L),
                CalendarEventExceptionEntity(blockId = "b-task", occurrenceDate = "2026-10-12", updatedAt = 1L),
                CalendarEventExceptionEntity(blockId = "daily-task", occurrenceDate = "2026-10-12", updatedAt = 1L)
            )
        )
        database.imageBlockDao().upsertImages(
            listOf(
                ImageBlockEntity(blockId = "a-image", noteId = "note-a", localFilePath = "/media/kept.jpg", noteCreatedAt = 1L, sourceType = TaskSource.NOTE),
                ImageBlockEntity(blockId = "b-image", noteId = "note-b", localFilePath = "/media/other-space.jpg", noteCreatedAt = 1L, sourceType = TaskSource.NOTE)
            )
        )
        database.mediaReferenceDao().upsertReferences(
            listOf(MediaReferenceEntity(noteId = "note-a", fileName = "kept.jpg"), MediaReferenceEntity(noteId = "note-b", fileName = "other-space.jpg"))
        )
        database.chatSessionDao().upsertSession(
            ChatSessionEntity(id = "chat-a", title = "Chat", messagesJson = "[]", createdAt = 1L, updatedAt = 1L, spaceId = "space-a")
        )
        database.selfHostDeletedNoteDao().upsertTombstone(
            SelfHostDeletedNoteEntity(noteId = "gone-note", isDaily = false, dateString = null, deletedAt = 1L, spaceId = "space-a")
        )
        database.selfHostDeletedApiConfigDao().upsertTombstone(SelfHostDeletedApiConfigEntity(provider = "openai", deletedAt = 1L))
        database.unappliedSyncChangeDao().saveChange(
            UnappliedSyncChangeEntity(
                entityType = "NOTE",
                entityId = "note-a",
                envelopeJson = "{}",
                failedOnAppVersion = "1.0",
                failedAt = 1L,
                waitsForAppUpdate = false,
                failedAttempts = 1
            )
        )
        database.unusedMediaFileDao().markUnused(listOf(UnusedMediaFileEntity(fileName = "old.jpg", unusedSince = 1L)))
    }

    private suspend fun firstColumnOf(sql: String): Set<String> =
        database.useReaderConnection { connection ->
            connection.usePrepared(sql) { statement ->
                buildSet {
                    while (statement.step()) add(statement.getText(0))
                }
            }
        }

    @Test
    fun onlyThePickedSpacesAndTheirLiveNotesAreKept() = runTest {
        fillDatabase()

        database.keepOnlySpacesForExport(setOf("space-a"))

        assertEquals(setOf("space-a"), firstColumnOf("SELECT spaceId FROM spaces"))
        assertEquals(setOf("note-a", "daily-a"), firstColumnOf("SELECT noteId FROM notes_metadata"))
        assertEquals(
            setOf("a-text", "a-image", "a-voice", "a-document", "a-task", "daily-task"),
            firstColumnOf("SELECT blockId FROM note_blocks")
        )
        assertEquals(setOf("a-node"), firstColumnOf("SELECT nodeId FROM canvas_nodes"))
        assertEquals(setOf("a-edge"), firstColumnOf("SELECT edgeId FROM canvas_edges"))
        assertEquals(setOf("a-stroke"), firstColumnOf("SELECT strokeId FROM canvas_strokes"))
    }

    @Test
    fun spaceItemsFromOtherSpacesAndDeletedItemsAreLeftOut() = runTest {
        fillDatabase()

        database.keepOnlySpacesForExport(setOf("space-a"))

        assertEquals(setOf("folder-a"), firstColumnOf("SELECT folderId FROM folders"))
        assertEquals(setOf("category-a"), firstColumnOf("SELECT categoryId FROM calendar_categories"))
        assertEquals(setOf("tag-a"), firstColumnOf("SELECT tagId FROM property_tags"))
        assertEquals(setOf("property-a"), firstColumnOf("SELECT propertyId FROM custom_properties"))
        assertEquals(setOf("a-task", "daily-task"), firstColumnOf("SELECT blockId FROM calendar_tasks"))
        assertEquals(setOf("a-task", "daily-task"), firstColumnOf("SELECT blockId FROM calendar_event_exceptions"))
        assertEquals(setOf("a-image"), firstColumnOf("SELECT blockId FROM image_blocks"))
        assertEquals(setOf("kept.jpg"), firstColumnOf("SELECT fileName FROM media_references"))
    }

    @Test
    fun chatsAndSyncBookkeepingAreLeftOut() = runTest {
        fillDatabase()

        database.keepOnlySpacesForExport(setOf("space-a"))

        assertEquals(emptySet(), firstColumnOf("SELECT id FROM chat_sessions"))
        assertEquals(emptySet(), firstColumnOf("SELECT noteId FROM self_host_deleted_notes"))
        assertEquals(emptySet(), firstColumnOf("SELECT provider FROM self_host_deleted_api_configs"))
        assertEquals(emptySet(), firstColumnOf("SELECT entityId FROM unapplied_sync_changes"))
        assertEquals(emptySet(), firstColumnOf("SELECT fileName FROM unused_media_files"))
    }

    @Test
    fun onlyMediaUsedByTheKeptNotesIsListed() = runTest {
        fillDatabase()

        val mediaFileNames = database.keepOnlySpacesForExport(setOf("space-a"))

        assertEquals(setOf("kept.jpg", "voice.m4a", "document.pdf", "cover.png", "canvas.png"), mediaFileNames)
    }

    @Test
    fun pickingEverySpaceKeepsEveryLiveNote() = runTest {
        fillDatabase()

        val mediaFileNames = database.keepOnlySpacesForExport(setOf("space-a", "space-b"))

        assertEquals(setOf("space-a", "space-b"), firstColumnOf("SELECT spaceId FROM spaces"))
        assertEquals(setOf("note-a", "note-b", "daily-a"), firstColumnOf("SELECT noteId FROM notes_metadata"))
        assertEquals(
            setOf("kept.jpg", "voice.m4a", "document.pdf", "cover.png", "canvas.png", "other-space.jpg", "other-canvas.png"),
            mediaFileNames
        )
    }
}
