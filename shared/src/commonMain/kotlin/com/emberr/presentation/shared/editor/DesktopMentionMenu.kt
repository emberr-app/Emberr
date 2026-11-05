package com.emberr.presentation.shared.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import kotlin.math.roundToInt
import com.emberr.presentation.shared.editor.slash.SlashMenuList
import com.emberr.presentation.shared.editor.slash.SlashMenuSectionData

@Composable
internal fun rememberMentionMenuSections(
    mentionQuery: String?,
    allLinkableNotes: List<NoteMetadataEntity>,
    onCreateBlankNote: () -> Unit,
    onCreateNote: (String) -> Unit,
    onPick: (NoteMetadataEntity) -> Unit
): List<SlashMenuSectionData> {
    var shownMentionNoteCount by remember(mentionQuery) { mutableIntStateOf(MENTION_RESULTS_PER_STEP) }
    var shownMentionPeopleCount by remember(mentionQuery) { mutableIntStateOf(MENTION_RESULTS_PER_STEP) }
    return remember(mentionQuery, allLinkableNotes, shownMentionNoteCount, shownMentionPeopleCount) {
        mentionMenuSections(
            query = mentionQuery.orEmpty(),
            mentionableNotes = allLinkableNotes,
            shownNoteCount = shownMentionNoteCount,
            shownPeopleCount = shownMentionPeopleCount,
            onShowMoreNotes = { shownMentionNoteCount += MENTION_RESULTS_PER_STEP },
            onShowMorePeople = { shownMentionPeopleCount += MENTION_RESULTS_PER_STEP },
            onCreateBlankNote = onCreateBlankNote,
            onCreateNote = onCreateNote,
            onPick = onPick
        )
    }
}

@Composable
internal fun DesktopMentionMenu(
    anchorRect: Rect,
    sections: List<SlashMenuSectionData>,
    selectedIndex: Int,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { IntOffset(anchorRect.left.roundToInt(), anchorRect.top.roundToInt()) }
            .size(width = 1.dp, height = with(density) { anchorRect.height.toDp() })
    ) {
        EmberrDesktopMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = false),
            modifier = Modifier
                .width(290.dp)
                .heightIn(max = 400.dp)
        ) {
            SlashMenuList(
                sections = sections,
                selectedIndex = selectedIndex
            )
        }
    }
}
