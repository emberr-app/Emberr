// The saved-state keys this widget uses to remember its note, the card it last drew, and whether the note was deleted.
package com.emberr.presentation.widget.notecard

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val cardNoteIdKey = stringPreferencesKey("card_note_id")
val cachedCardKey = stringPreferencesKey("cached_card")
val cardNoteDeletedKey = booleanPreferencesKey("card_note_deleted")
