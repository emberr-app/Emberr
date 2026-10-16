package com.emberr.presentation.search

import com.emberr.domain.canvas.isEmbeddedCanvas
import com.emberr.domain.model.NoteSearchResult
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private const val PINNED_BLOCKS_DAILY_NOTE_DATE = "global_pinned"

val NoteSearchResult.canBeRenamed: Boolean
    get() = !note.isDaily

val NoteSearchResult.canBeFavoritedOrTrashed: Boolean
    get() = !note.isDaily && !note.isEmbeddedCanvas

val NoteSearchResult.isPinnedBlocks: Boolean
    get() = note.isDaily && note.dateString == PINNED_BLOCKS_DAILY_NOTE_DATE

val NoteSearchResult.dailyDateToOpen: String?
    get() = when {
        !note.isDaily -> null
        isPinnedBlocks -> Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        else -> note.dateString
    }

val NoteSearchResult.favoriteOptionLabel: String
    get() = if (note.isFavorite) "Remove from Favorites" else "Add to Favorites"
