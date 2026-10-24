package com.emberr.domain.selfhost.merge

import com.emberr.data.local.room.entity.CalendarEventExceptionEntity
import com.emberr.data.local.room.entity.CategoryEntity
import com.emberr.data.local.room.entity.CustomPropertyEntity
import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.PropertyTagEntity
import com.emberr.data.local.room.entity.SelfHostDeletedApiConfigEntity
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.ai.external.ExternalAiProvider
import com.emberr.domain.ai.external.ExternalAiProviderConfig
import com.emberr.domain.selfhost.sync.ApiConfigSyncEntry

fun mergeSpaces(localSpaces: List<SpaceEntity>, remoteSpaces: List<SpaceEntity>): List<SpaceEntity> =
    mergeRowsNewestWinsTieGoesToRemote(localSpaces, remoteSpaces, keyOf = { it.spaceId }, updatedAtOf = { it.updatedAt })

fun mergeFolders(localFolders: List<FolderEntity>, remoteFolders: List<FolderEntity>): List<FolderEntity> =
    mergeRowsNewestWinsTieGoesToRemote(localFolders, remoteFolders, keyOf = { it.folderId }, updatedAtOf = { it.updatedAt })

fun mergeCategories(localCategories: List<CategoryEntity>, remoteCategories: List<CategoryEntity>): List<CategoryEntity> =
    mergeRowsNewestWinsTieGoesToRemote(localCategories, remoteCategories, keyOf = { it.categoryId }, updatedAtOf = { it.updatedAt })

fun mergePropertyTags(localTags: List<PropertyTagEntity>, remoteTags: List<PropertyTagEntity>): List<PropertyTagEntity> =
    mergeRowsNewestWinsTieGoesToRemote(localTags, remoteTags, keyOf = { it.tagId }, updatedAtOf = { it.updatedAt })

fun mergeCustomProperties(
    localProperties: List<CustomPropertyEntity>,
    remoteProperties: List<CustomPropertyEntity>
): List<CustomPropertyEntity> =
    mergeRowsNewestWinsTieGoesToRemote(localProperties, remoteProperties, keyOf = { it.propertyId }, updatedAtOf = { it.updatedAt })

fun mergeEventExceptions(
    localExceptions: List<CalendarEventExceptionEntity>,
    remoteExceptions: List<CalendarEventExceptionEntity>
): List<CalendarEventExceptionEntity> =
    mergeRowsNewestWinsTieGoesToRemote(localExceptions, remoteExceptions, keyOf = ::occurrenceKeyOf, updatedAtOf = { it.updatedAt })

private fun occurrenceKeyOf(exception: CalendarEventExceptionEntity): String =
    "${exception.blockId}|${exception.occurrenceDate}"

private fun <Row> mergeRowsNewestWinsTieGoesToRemote(
    localRows: List<Row>,
    remoteRows: List<Row>,
    keyOf: (Row) -> String,
    updatedAtOf: (Row) -> Long
): List<Row> {
    val merged = LinkedHashMap<String, Row>()
    localRows.forEach { merged[keyOf(it)] = it }
    remoteRows.forEach { remote ->
        val local = merged[keyOf(remote)]
        if (local == null || updatedAtOf(remote) >= updatedAtOf(local)) {
            merged[keyOf(remote)] = remote
        }
    }
    return merged.values.toList()
}

data class ApiConfigMergeResult(
    val mergedEntry: ApiConfigSyncEntry?,
    val remoteEntryToApply: ApiConfigSyncEntry?
)

fun mergeApiConfig(
    provider: ExternalAiProvider,
    localConfig: ExternalAiProviderConfig?,
    localTombstone: SelfHostDeletedApiConfigEntity?,
    remoteEntry: ApiConfigSyncEntry?
): ApiConfigMergeResult {
    val localIsDeleted = localConfig == null ||
        (localTombstone != null && localTombstone.deletedAt >= localConfig.updatedAt)
    val localUpdatedAt = maxOf(localConfig?.updatedAt ?: 0L, localTombstone?.deletedAt ?: 0L)

    return when {
        remoteEntry != null && remoteEntry.updatedAt > localUpdatedAt ->
            ApiConfigMergeResult(mergedEntry = remoteEntry, remoteEntryToApply = remoteEntry)
        !localIsDeleted -> ApiConfigMergeResult(
            mergedEntry = ApiConfigSyncEntry(
                provider = provider.name,
                apiKey = localConfig.apiKey,
                model = localConfig.model,
                baseUrl = localConfig.baseUrl,
                updatedAt = localConfig.updatedAt
            ),
            remoteEntryToApply = null
        )
        localTombstone != null -> ApiConfigMergeResult(
            mergedEntry = ApiConfigSyncEntry(
                provider = provider.name,
                apiKey = "",
                model = "",
                baseUrl = null,
                updatedAt = localTombstone.deletedAt,
                isDeleted = true
            ),
            remoteEntryToApply = null
        )
        else -> ApiConfigMergeResult(mergedEntry = null, remoteEntryToApply = null)
    }
}
