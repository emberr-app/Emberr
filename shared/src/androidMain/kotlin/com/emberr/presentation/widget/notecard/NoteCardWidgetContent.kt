// The note details that the card widget stores and draws.
package com.emberr.presentation.widget.notecard

import kotlinx.serialization.Serializable

@Serializable
data class NoteCardWidgetContent(
    val noteId: String,
    val title: String,
    val icon: String?,
    val snippet: String,
    val isFavorite: Boolean,
    val isCanvas: Boolean,
    val coverImageAbsolutePath: String?
)
