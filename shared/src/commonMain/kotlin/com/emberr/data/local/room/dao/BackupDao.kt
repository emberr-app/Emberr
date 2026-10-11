package com.emberr.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emberr.data.local.room.entity.CalendarEventExceptionEntity
import com.emberr.data.local.room.entity.CanvasEdgeEntity
import com.emberr.data.local.room.entity.CanvasNodeEntity
import com.emberr.data.local.room.entity.CanvasStrokeEntity
import com.emberr.data.local.room.entity.CategoryEntity
import com.emberr.data.local.room.entity.CustomPropertyEntity
import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.NoteBlockEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.PropertyTagEntity
import com.emberr.data.local.room.entity.SpaceEntity

@Dao
interface BackupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpaces(spaces: List<SpaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteMetadataEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<NoteBlockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPropertyTags(tags: List<PropertyTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomProperties(properties: List<CustomPropertyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEventExceptions(exceptions: List<CalendarEventExceptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanvasNodes(nodes: List<CanvasNodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanvasEdges(edges: List<CanvasEdgeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanvasStrokes(strokes: List<CanvasStrokeEntity>)

    @Query("DELETE FROM notes_metadata WHERE spaceId NOT IN (:spaceIdsToKeep) OR trashedAt IS NOT NULL")
    suspend fun deleteNotesOutsideSpacesOrInTrash(spaceIdsToKeep: List<String>)

    @Query("DELETE FROM note_blocks WHERE isDeleted = 1 OR noteId NOT IN (SELECT noteId FROM notes_metadata)")
    suspend fun deleteBlocksThatAreDeletedOrHaveNoNote()

    @Query("DELETE FROM canvas_nodes WHERE isDeleted = 1 OR noteId NOT IN (SELECT noteId FROM notes_metadata)")
    suspend fun deleteCanvasNodesThatAreDeletedOrHaveNoNote()

    @Query("DELETE FROM canvas_edges WHERE isDeleted = 1 OR noteId NOT IN (SELECT noteId FROM notes_metadata)")
    suspend fun deleteCanvasEdgesThatAreDeletedOrHaveNoNote()

    @Query("DELETE FROM canvas_strokes WHERE isDeleted = 1 OR noteId NOT IN (SELECT noteId FROM notes_metadata)")
    suspend fun deleteCanvasStrokesThatAreDeletedOrHaveNoNote()

    @Query("DELETE FROM calendar_tasks WHERE blockId NOT IN (SELECT blockId FROM note_blocks)")
    suspend fun deleteCalendarTasksThatHaveNoBlock()

    @Query("DELETE FROM calendar_event_exceptions WHERE blockId NOT IN (SELECT blockId FROM note_blocks)")
    suspend fun deleteCalendarEventExceptionsThatHaveNoBlock()

    @Query("DELETE FROM image_blocks WHERE noteId NOT IN (SELECT noteId FROM notes_metadata)")
    suspend fun deleteImageBlocksThatHaveNoNote()

    @Query("DELETE FROM document_blocks WHERE noteId NOT IN (SELECT noteId FROM notes_metadata)")
    suspend fun deleteDocumentBlocksThatHaveNoNote()

    @Query("DELETE FROM bookmark_blocks WHERE noteId NOT IN (SELECT noteId FROM notes_metadata)")
    suspend fun deleteBookmarkBlocksThatHaveNoNote()

    @Query("DELETE FROM media_references WHERE noteId NOT IN (SELECT noteId FROM notes_metadata)")
    suspend fun deleteMediaReferencesThatHaveNoNote()

    @Query("DELETE FROM spaces WHERE spaceId NOT IN (:spaceIdsToKeep) OR isDeleted = 1")
    suspend fun deleteSpacesNotKept(spaceIdsToKeep: List<String>)

    @Query("DELETE FROM folders WHERE spaceId NOT IN (:spaceIdsToKeep) OR isDeleted = 1")
    suspend fun deleteFoldersOutsideSpacesOrDeleted(spaceIdsToKeep: List<String>)

    @Query("DELETE FROM calendar_categories WHERE spaceId NOT IN (:spaceIdsToKeep) OR isDeleted = 1")
    suspend fun deleteCategoriesOutsideSpacesOrDeleted(spaceIdsToKeep: List<String>)

    @Query("DELETE FROM property_tags WHERE spaceId NOT IN (:spaceIdsToKeep) OR isDeleted = 1")
    suspend fun deletePropertyTagsOutsideSpacesOrDeleted(spaceIdsToKeep: List<String>)

    @Query("DELETE FROM custom_properties WHERE spaceId NOT IN (:spaceIdsToKeep) OR isDeleted = 1")
    suspend fun deleteCustomPropertiesOutsideSpacesOrDeleted(spaceIdsToKeep: List<String>)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllChatSessions()

    @Query("DELETE FROM self_host_deleted_notes")
    suspend fun deleteAllNoteTombstones()

    @Query("DELETE FROM self_host_deleted_api_configs")
    suspend fun deleteAllApiConfigTombstones()

    @Query("DELETE FROM unapplied_sync_changes")
    suspend fun deleteAllUnappliedSyncChanges()

    @Query("DELETE FROM unused_media_files")
    suspend fun deleteAllUnusedMediaFiles()

    @Query("SELECT blockDataJson FROM note_blocks")
    suspend fun getAllBlockDataJson(): List<String>
}
