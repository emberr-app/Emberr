package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.entity.CalendarEventExceptionEntity
import com.emberr.data.local.room.entity.CategoryEntity
import com.emberr.data.local.room.entity.CustomPropertyEntity
import com.emberr.data.local.room.entity.PropertyTagEntity
import com.emberr.domain.model.BookmarkCategoryOrderBySpace
import com.emberr.domain.model.FavoriteNoteOrderBySpace
import com.emberr.domain.selfhost.sync.ApiConfigSyncEntry
import kotlinx.serialization.Serializable

internal const val SPACE_LISTS_ITEM_ID = "space_lists"
internal const val ACCOUNT_LISTS_ITEM_ID = "account_lists"

@Serializable
data class CloudListsPayload(
    val categories: List<CategoryEntity> = emptyList(),
    val propertyTags: List<PropertyTagEntity> = emptyList(),
    val customProperties: List<CustomPropertyEntity> = emptyList(),
    val eventExceptions: List<CalendarEventExceptionEntity> = emptyList(),
    val apiConfigs: List<ApiConfigSyncEntry> = emptyList(),
    val bookmarkCategoryOrders: BookmarkCategoryOrderBySpace = BookmarkCategoryOrderBySpace(),
    val favoriteNoteOrders: FavoriteNoteOrderBySpace = FavoriteNoteOrderBySpace()
)

fun CloudListsPayload.hasSameEntriesAs(other: CloudListsPayload): Boolean =
    categories.toSet() == other.categories.toSet() &&
        propertyTags.toSet() == other.propertyTags.toSet() &&
        customProperties.toSet() == other.customProperties.toSet() &&
        eventExceptions.toSet() == other.eventExceptions.toSet() &&
        apiConfigs.toSet() == other.apiConfigs.toSet() &&
        bookmarkCategoryOrders == other.bookmarkCategoryOrders &&
        favoriteNoteOrders == other.favoriteNoteOrders

fun CloudListsPayload.hasEntries(): Boolean = !hasSameEntriesAs(CloudListsPayload())

fun CloudListsPayload.newestEntryTime(): Long =
    (categories.map { it.updatedAt } +
        propertyTags.map { it.updatedAt } +
        customProperties.map { it.updatedAt } +
        eventExceptions.map { it.updatedAt } +
        apiConfigs.map { it.updatedAt } +
        bookmarkCategoryOrders.ordersBySpaceId.values.map { it.updatedAt } +
        favoriteNoteOrders.ordersBySpaceId.values.map { it.updatedAt }
        ).maxOrNull() ?: 0L
