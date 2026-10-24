package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.dao.CategoryDao
import com.emberr.data.local.room.dao.ChatSessionDao
import com.emberr.data.local.room.dao.CloudItemDao
import com.emberr.data.local.room.dao.CustomPropertyDao
import com.emberr.data.local.room.dao.FolderDao
import com.emberr.data.local.room.dao.NoteDao
import com.emberr.data.local.room.dao.PropertyTagDao
import com.emberr.data.local.room.dao.SelfHostDeletedNoteDao
import com.emberr.data.local.room.dao.SpaceDao
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CloudItemEntity
import com.emberr.data.local.room.entity.CloudSpaceEntity
import com.emberr.data.local.room.entity.PLACEHOLDER_SPACE_UPDATED_AT
import com.emberr.domain.cloud.keys.CloudItemType

class CloudLocalChanges(
    private val cloudItemDao: CloudItemDao,
    private val noteDao: NoteDao,
    private val spaceDao: SpaceDao,
    private val folderDao: FolderDao,
    private val chatSessionDao: ChatSessionDao,
    private val selfHostDeletedNoteDao: SelfHostDeletedNoteDao,
    private val categoryDao: CategoryDao,
    private val propertyTagDao: PropertyTagDao,
    private val customPropertyDao: CustomPropertyDao
) {

    internal suspend fun areWaiting(savedSpaces: List<CloudSpaceEntity>): Boolean {
        if (noteDao.getNotesNeedingCloudSync().isNotEmpty()) return true

        val localSpaces = spaceDao.getAllSpacesOnce()
        val savedSpaceIds = savedSpaces.map { it.appSpaceId }.toSet()
        if (ACCOUNT_SPACE_ID !in savedSpaceIds || localSpaces.any { it.spaceId !in savedSpaceIds }) return true

        val rowsBySpaceId = savedSpaces.associate { space ->
            space.appSpaceId to cloudItemDao.getItemsInSpace(space.spaceRef).associateBy { it.itemId }
        }
        fun isNewerThanRow(spaceId: String, itemId: String, updatedAt: Long): Boolean {
            val streamRows = rowsBySpaceId[spaceId] ?: return false
            val row = streamRows[itemId]
            return row == null || updatedAt > row.syncedUpdatedAt
        }
        fun listRowTimeOf(spaceId: String): Long {
            val listItemId = if (spaceId == ACCOUNT_SPACE_ID) ACCOUNT_LISTS_ITEM_ID else SPACE_LISTS_ITEM_ID
            return rowsBySpaceId[spaceId]?.get(listItemId)?.syncedUpdatedAt ?: 0L
        }

        val spaceChanged = localSpaces.any { space ->
            space.spaceId != ACCOUNT_SPACE_ID && space.updatedAt != PLACEHOLDER_SPACE_UPDATED_AT &&
                isNewerThanRow(space.spaceId, space.spaceId, space.updatedAt)
        }
        if (spaceChanged) return true

        val folderChanged = folderDao.getFoldersModifiedSince(0L).any { folder ->
            folder.spaceId != ACCOUNT_SPACE_ID && isNewerThanRow(folder.spaceId, folder.folderId, folder.updatedAt)
        }
        if (folderChanged) return true

        val chatChanged = chatSessionDao.getAllSessionsIncludingDeleted().any { session ->
            isNewerThanRow(session.spaceId, session.id, session.updatedAt)
        }
        if (chatChanged) return true

        val categoryChanged = categoryDao.getCategoriesModifiedSince(listRowTimeOf(ACCOUNT_SPACE_ID))
            .any { it.spaceId == ACCOUNT_SPACE_ID }
        val tagChanged = propertyTagDao.getTagsModifiedSince(0L).any { tag ->
            tag.spaceId in rowsBySpaceId && tag.updatedAt > listRowTimeOf(tag.spaceId)
        }
        val customPropertyChanged = customPropertyDao.getPropertiesModifiedSince(0L).any { property ->
            property.spaceId in rowsBySpaceId && property.updatedAt > listRowTimeOf(property.spaceId)
        }
        if (categoryChanged || tagChanged || customPropertyChanged) return true

        return localDeletesAreWaiting(rowsBySpaceId.values.flatMap { it.values })
    }

    private suspend fun localDeletesAreWaiting(rows: List<CloudItemEntity>): Boolean {
        val liveNoteIds = rows
            .filter { !it.isDeleted && (it.itemType == CloudItemType.NOTE.name || it.itemType == CloudItemType.DAILY.name) }
            .mapTo(HashSet()) { it.localId }
        if (liveNoteIds.isEmpty()) return false
        return selfHostDeletedNoteDao.getAllTombstones().any { tombstone ->
            val dateString = tombstone.dateString
            tombstone.noteId in liveNoteIds &&
                noteDao.getNoteById(tombstone.noteId) == null &&
                !(tombstone.isDaily && dateString != null && noteDao.getDailyNoteMetadata(ACCOUNT_SPACE_ID, dateString) != null)
        }
    }
}
