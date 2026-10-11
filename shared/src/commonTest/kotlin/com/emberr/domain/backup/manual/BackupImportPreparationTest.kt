package com.emberr.domain.backup.manual

import com.emberr.data.local.room.entity.CalendarEventExceptionEntity
import com.emberr.data.local.room.entity.CanvasNodeEntity
import com.emberr.data.local.room.entity.CategoryEntity
import com.emberr.data.local.room.entity.ChatSessionEntity
import com.emberr.data.local.room.entity.CustomPropertyEntity
import com.emberr.data.local.room.entity.DEFAULT_SPACE_ID
import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.NoteBlockEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.PropertyTagEntity
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.database.cellStyleKey
import com.emberr.domain.database.databaseCellBlockId
import com.emberr.domain.database.databaseNoteId
import com.emberr.domain.model.CanvasBlock
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseCellStyle
import com.emberr.domain.model.DatabaseColumnTarget
import com.emberr.domain.model.DatabaseCustomProperty
import com.emberr.domain.model.DatabaseView
import com.emberr.domain.model.DatabaseViewType
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteBlockSerializer
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.PropertyValueType
import com.emberr.domain.model.TextBlock
import com.emberr.domain.template.ProjectsTemplate
import com.emberr.domain.template.noteIdInSpace
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupImportPreparationTest {

    private val workSpaceId = "6f1c2a9e-3b4d-4e5f-8a7b-9c0d1e2f3a4b"
    private val noteId = "11111111-2222-4333-8444-555555555555"
    private val otherNoteId = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"
    private val folderId = "f0f0f0f0-1111-4222-8333-444444444444"
    private val categoryId = "cccccccc-1111-4222-8333-444444444444"
    private val customPropertyId = "dddddddd-1111-4222-8333-444444444444"
    private val now = 9_000L
    private val blockJson = Json { encodeDefaults = true }

    private var lastNewIdNumber = 0
    private fun newId(): String {
        lastNewIdNumber += 1
        return "00000000-0000-4000-8000-" + lastNewIdNumber.toString().padStart(12, '0')
    }

    private fun space(spaceId: String, name: String, isDeleted: Boolean = false) =
        SpaceEntity(spaceId = spaceId, displayName = name, createdAt = 1L, updatedAt = 1L, isDeleted = isDeleted)

    private fun note(
        noteId: String,
        spaceId: String = DEFAULT_SPACE_ID,
        folderId: String? = null,
        databaseId: String? = null,
        trashedAt: Long? = null
    ) = NoteMetadataEntity(
        noteId = noteId,
        title = noteId,
        folderId = folderId,
        isDaily = false,
        dateString = null,
        createdAt = 1L,
        updatedAt = 1L,
        filePath = "",
        trashedAt = trashedAt,
        selfHostSyncedAt = 5L,
        spaceId = spaceId,
        databaseId = databaseId
    )

    private fun stored(noteId: String, block: NoteBlock) = NoteBlockEntity(
        blockId = block.id,
        noteId = noteId,
        displayOrder = 0,
        blockDataJson = blockJson.encodeToString(NoteBlockSerializer, block),
        updatedAt = 1L,
        isDeleted = block.isDeleted
    )

    private fun backup(
        spaces: List<SpaceEntity> = listOf(space(DEFAULT_SPACE_ID, "Personal")),
        notes: List<NoteMetadataEntity> = emptyList(),
        blocks: List<NoteBlockEntity> = emptyList(),
        folders: List<FolderEntity> = emptyList(),
        categories: List<CategoryEntity> = emptyList(),
        customProperties: List<CustomPropertyEntity> = emptyList(),
        propertyTags: List<PropertyTagEntity> = emptyList(),
        exceptions: List<CalendarEventExceptionEntity> = emptyList(),
        canvasNodes: List<CanvasNodeEntity> = emptyList(),
        chatSessions: List<ChatSessionEntity> = emptyList()
    ) = EmberrBackupData(
        exportTimestamp = 1L,
        spaces = spaces,
        notes = notes,
        blocks = blocks,
        folders = folders,
        categories = categories,
        customProperties = customProperties,
        propertyTags = propertyTags,
        calendarEventExceptions = exceptions,
        canvasNodes = canvasNodes,
        chatSessions = chatSessions
    )

    private fun prepare(backup: EmberrBackupData, spaceIds: Set<String> = backup.spaces.map { it.spaceId }.toSet()) =
        prepareSpacesForImport(backup, spaceIds, now, ::newId)

    private fun decoded(block: NoteBlockEntity): NoteBlock =
        Json { ignoreUnknownKeys = true }.decodeFromString(NoteBlockSerializer, block.blockDataJson)

    private fun allIdsIn(data: EmberrBackupData): Set<String> =
        data.spaces.map { it.spaceId }.toSet() + data.notes.map { it.noteId } + data.blocks.map { it.blockId } +
                data.folders.map { it.folderId } + data.categories.map { it.categoryId } +
                data.customProperties.map { it.propertyId } + data.propertyTags.map { it.tagId }

    @Test
    fun importedSpacesGetNewIdsAndImportedNames() {
        val prepared = prepare(backup(spaces = listOf(space(DEFAULT_SPACE_ID, "Personal"), space(workSpaceId, "Work"))))

        assertEquals(listOf("Personal (Imported)", "Work (Imported)"), prepared.spaces.map { it.displayName })
        assertTrue(prepared.spaces.none { it.spaceId == DEFAULT_SPACE_ID || it.spaceId == workSpaceId })
    }

    @Test
    fun onlyThePickedSpacesAreImported() {
        val prepared = prepare(
            backup(
                spaces = listOf(space(DEFAULT_SPACE_ID, "Personal"), space(workSpaceId, "Work")),
                notes = listOf(note(noteId, spaceId = DEFAULT_SPACE_ID), note(otherNoteId, spaceId = workSpaceId))
            ),
            spaceIds = setOf(workSpaceId)
        )

        assertEquals(listOf("Work (Imported)"), prepared.spaces.map { it.displayName })
        assertEquals(listOf(otherNoteId), prepared.notes.map { it.title })
        assertEquals(prepared.spaces.single().spaceId, prepared.notes.single().spaceId)
    }

    @Test
    fun importingTheSameBackupTwiceGivesTwoSeparateCopies() {
        val original = backup(
            notes = listOf(note(noteId, folderId = folderId)),
            blocks = listOf(stored(noteId, TextBlock(id = "77777777-1111-4222-8333-444444444444", text = "hello"))),
            folders = listOf(FolderEntity(folderId = folderId, name = "Folder", parentFolderId = null, createdAt = 1L))
        )

        val firstCopy = prepare(original)
        val secondCopy = prepare(original)

        val originalIds = allIdsIn(original)
        assertTrue(allIdsIn(firstCopy).none { it in originalIds })
        assertTrue(allIdsIn(secondCopy).none { it in originalIds || it in allIdsIn(firstCopy) })
    }

    @Test
    fun linksInsideTheImportPointToTheCopies() {
        val prepared = prepare(
            backup(
                notes = listOf(note(noteId, folderId = folderId), note(otherNoteId)),
                blocks = listOf(
                    stored(noteId, LinkedNoteBlock(id = "b1", linkedNoteId = otherNoteId)),
                    stored(noteId, CanvasBlock(id = "b2", canvasNoteId = otherNoteId)),
                    stored(noteId, CheckboxBlock(id = "b3", text = "Task", categoryId = categoryId)),
                    stored(noteId, PropertyBlock(id = "b4", customPropertyId = customPropertyId, text = "Value"))
                ),
                folders = listOf(FolderEntity(folderId = folderId, name = "Folder", parentFolderId = null, createdAt = 1L)),
                categories = listOf(CategoryEntity(categoryId = categoryId, name = "Work", colorHex = "#000000", createdAt = 1L)),
                customProperties = listOf(
                    CustomPropertyEntity(propertyId = customPropertyId, name = "Owner", valueType = PropertyValueType.TEXT, createdAt = 1L)
                )
            )
        )

        val copiedNote = prepared.notes.single { it.title == noteId }
        val copiedOtherNote = prepared.notes.single { it.title == otherNoteId }
        val blocks = prepared.blocks.filter { it.noteId == copiedNote.noteId }.map(::decoded)
        assertEquals(copiedOtherNote.noteId, blocks.filterIsInstance<LinkedNoteBlock>().single().linkedNoteId)
        assertEquals(copiedOtherNote.noteId, blocks.filterIsInstance<CanvasBlock>().single().canvasNoteId)
        assertEquals(prepared.categories.single().categoryId, blocks.filterIsInstance<CheckboxBlock>().single().categoryId)
        assertEquals(prepared.customProperties.single().propertyId, blocks.filterIsInstance<PropertyBlock>().single().customPropertyId)
        assertEquals(prepared.folders.single().folderId, copiedNote.folderId)
    }

    @Test
    fun linksToNotesOutsideTheImportAreLeftAsTheyWere() {
        val prepared = prepare(
            backup(notes = listOf(note(noteId)), blocks = listOf(stored(noteId, LinkedNoteBlock(id = "b1", linkedNoteId = otherNoteId))))
        )

        assertEquals(otherNoteId, (decoded(prepared.blocks.single()) as LinkedNoteBlock).linkedNoteId)
    }

    @Test
    fun databaseRowsCellsAndSettingsStayConnected() {
        val databaseId = "dbdbdbdb-1111-4222-8333-444444444444"
        val rowNoteId = "e0e0e0e0-1111-4222-8333-444444444444"
        val templateNoteId = "abababab-1111-4222-8333-444444444444"
        val ratingPropertyId = "acacacac-1111-4222-8333-444444444444"
        val ratingColumn = DatabaseColumnTarget.CustomProperty(ratingPropertyId)
        val database = DatabaseBlock(
            id = "database-block",
            databaseId = databaseId,
            columns = listOf(ratingColumn),
            customProperties = listOf(DatabaseCustomProperty(id = ratingPropertyId, name = "Rating", valueType = PropertyValueType.NUMBER)),
            defaultTemplateId = templateNoteId,
            rowStyles = mapOf(rowNoteId to DatabaseCellStyle(backgroundColorName = "red")),
            cellStyles = mapOf(cellStyleKey(rowNoteId, ratingPropertyId) to DatabaseCellStyle(textColorName = "blue")),
            views = listOf(DatabaseView(id = "default-table", name = "Table", type = DatabaseViewType.TABLE, manualRowOrder = listOf(rowNoteId)))
        )
        val prepared = prepare(
            backup(
                notes = listOf(
                    note(noteId),
                    note(databaseNoteId(databaseId)),
                    note(rowNoteId, databaseId = databaseId),
                    note(templateNoteId, databaseId = databaseId)
                ),
                blocks = listOf(
                    stored(noteId, database),
                    stored(
                        rowNoteId,
                        PropertyBlock(id = databaseCellBlockId(ratingColumn, rowNoteId), customPropertyId = ratingPropertyId, text = "5")
                    )
                )
            )
        )

        val copiedDatabase = prepared.blocks.map(::decoded).filterIsInstance<DatabaseBlock>().single()
        val copiedRow = prepared.notes.single { it.title == rowNoteId }
        val copiedTemplate = prepared.notes.single { it.title == templateNoteId }
        val copiedCell = prepared.blocks.single { it.noteId == copiedRow.noteId }
        val copiedColumn = copiedDatabase.columns.single()

        assertEquals(copiedDatabase.databaseId, copiedRow.databaseId)
        assertTrue(copiedDatabase.databaseId != databaseId)
        assertEquals(databaseNoteId(copiedDatabase.databaseId), prepared.notes.single { it.title == databaseNoteId(databaseId) }.noteId)
        assertEquals(databaseCellBlockId(copiedColumn, copiedRow.noteId), copiedCell.blockId)
        assertEquals(copiedCell.blockId, decoded(copiedCell).id)
        assertEquals(copiedTemplate.noteId, copiedDatabase.defaultTemplateId)
        assertEquals(setOf(copiedRow.noteId), copiedDatabase.rowStyles.keys)
        assertEquals(setOf(cellStyleKey(copiedRow.noteId, (copiedColumn as DatabaseColumnTarget.CustomProperty).propertyId)), copiedDatabase.cellStyles.keys)
        assertEquals(listOf(copiedRow.noteId), copiedDatabase.views.single().manualRowOrder)
    }

    @Test
    fun templateNotesStayNamedAfterTheirNewSpace() {
        val prepared = prepare(backup(notes = listOf(note(ProjectsTemplate.noteIdInSpace(DEFAULT_SPACE_ID)))))

        assertEquals(ProjectsTemplate.noteIdInSpace(prepared.spaces.single().spaceId), prepared.notes.single().noteId)
    }

    @Test
    fun starterContentIdsWithoutUuidsGetNewIdsToo() {
        val sampleDatabaseId = "sample_note_database"
        val prepared = prepare(
            backup(
                notes = listOf(
                    note("sample_note_welcome"),
                    note(databaseNoteId(sampleDatabaseId)),
                    note("sample_note_database_row_1", databaseId = sampleDatabaseId)
                ),
                blocks = listOf(stored("sample_note_welcome", DatabaseBlock(id = "sample_note_welcome_database", databaseId = sampleDatabaseId)))
            )
        )

        val copiedDatabase = decoded(prepared.blocks.single()) as DatabaseBlock
        assertTrue(allIdsIn(prepared).none { "sample_note" in it })
        assertEquals(copiedDatabase.databaseId, prepared.notes.single { it.title == "sample_note_database_row_1" }.databaseId)
        assertEquals(databaseNoteId(copiedDatabase.databaseId), prepared.notes.single { it.title == databaseNoteId(sampleDatabaseId) }.noteId)
    }

    @Test
    fun trashDeletedItemsAndChatsAreLeftOut() {
        val prepared = prepare(
            backup(
                spaces = listOf(space(DEFAULT_SPACE_ID, "Personal"), space(workSpaceId, "Old", isDeleted = true)),
                notes = listOf(note(noteId), note(otherNoteId, trashedAt = 5L)),
                blocks = listOf(
                    stored(noteId, TextBlock(id = "kept", text = "kept")),
                    stored(noteId, TextBlock(id = "gone", text = "gone", isDeleted = true)),
                    stored(otherNoteId, TextBlock(id = "trashed", text = "trashed"))
                ),
                folders = listOf(FolderEntity(folderId = folderId, name = "Gone", parentFolderId = null, createdAt = 1L, isDeleted = true)),
                chatSessions = listOf(ChatSessionEntity(id = "chat", title = "Chat", messagesJson = "[]", createdAt = 1L, updatedAt = 1L))
            )
        )

        assertEquals(listOf("Personal (Imported)"), prepared.spaces.map { it.displayName })
        assertEquals(listOf(noteId), prepared.notes.map { it.title })
        assertEquals(listOf("kept"), prepared.blocks.map { (decoded(it) as TextBlock).text })
        assertEquals(emptyList(), prepared.folders)
        assertEquals(emptyList(), prepared.chatSessions)
    }

    @Test
    fun shortIdsWithoutUuidsAreNotReplacedInsideOtherWords() {
        val prepared = prepare(
            backup(
                notes = listOf(note("note")),
                blocks = listOf(stored("note", PropertyBlock(id = "cell", tags = listOf("notebook"), text = "my note")))
            )
        )

        val copiedCell = decoded(prepared.blocks.single()) as PropertyBlock
        assertEquals(listOf("notebook"), copiedCell.tags)
        assertEquals("my note", copiedCell.text)
        assertEquals(prepared.notes.single().noteId, prepared.blocks.single().noteId)
    }

    @Test
    fun anIdThatOnlyStartsLikeAnotherIdStillGetsItsOwnNewId() {
        val prepared = prepare(backup(notes = listOf(note("note"), note("notebook"))))

        assertTrue(prepared.notes.none { it.noteId == "note" || it.noteId == "notebook" })
        assertEquals(2, prepared.notes.map { it.noteId }.toSet().size)
    }

    @Test
    fun noteInAFolderThatIsNotImportedGoesToTheTopLevel() {
        val prepared = prepare(backup(notes = listOf(note(noteId, folderId = folderId))))

        assertNull(prepared.notes.single().folderId)
    }

    @Test
    fun calendarExceptionsFollowTheirTask() {
        val taskBlockId = "7a7a7a7a-1111-4222-8333-444444444444"
        val prepared = prepare(
            backup(
                notes = listOf(note(noteId)),
                blocks = listOf(stored(noteId, CheckboxBlock(id = taskBlockId, text = "Gym"))),
                exceptions = listOf(
                    CalendarEventExceptionEntity(blockId = taskBlockId, occurrenceDate = "2026-10-12", isCancelled = true, updatedAt = 1L),
                    CalendarEventExceptionEntity(blockId = "not-in-import", occurrenceDate = "2026-10-12", updatedAt = 1L)
                )
            )
        )

        val exception = prepared.calendarEventExceptions.single()
        assertEquals(prepared.blocks.single().blockId, exception.blockId)
        assertTrue(exception.isCancelled)
    }

    @Test
    fun everythingImportedCountsAsChangedSoItSyncs() {
        val prepared = prepare(
            backup(
                notes = listOf(note(noteId, folderId = folderId)),
                folders = listOf(FolderEntity(folderId = folderId, name = "Folder", parentFolderId = null, createdAt = 1L)),
                propertyTags = listOf(PropertyTagEntity(tagId = "9b9b9b9b-1111-4222-8333-444444444444", propertyKey = "TAGS", name = "idea", createdAt = 1L))
            )
        )

        assertEquals(listOf(now), prepared.spaces.map { it.updatedAt })
        assertEquals(listOf(now), prepared.notes.map { it.updatedAt })
        assertEquals(listOf(0L), prepared.notes.map { it.selfHostSyncedAt })
        assertEquals(listOf(now), prepared.folders.map { it.updatedAt })
        assertEquals(listOf(now), prepared.propertyTags.map { it.updatedAt })
        assertEquals("TAGS", prepared.propertyTags.single().propertyKey)
    }
}
