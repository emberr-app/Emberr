package com.emberr.presentation.search

import com.emberr.domain.canvas.isEmbeddedCanvas
import com.emberr.domain.model.NoteSearchResult

val NoteSearchResult.canBeRenamed: Boolean
    get() = !note.isDaily

val NoteSearchResult.canBeFavoritedOrTrashed: Boolean
    get() = !note.isDaily && !note.isEmbeddedCanvas

val NoteSearchResult.favoriteOptionLabel: String
    get() = if (note.isFavorite) "Remove from Favorites" else "Add to Favorites"
