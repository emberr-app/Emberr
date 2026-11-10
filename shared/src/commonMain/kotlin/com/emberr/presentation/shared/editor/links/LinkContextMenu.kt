package com.emberr.presentation.shared.editor.links

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.presentation.shared.components.EmberrPopupMenuRow
import com.emberr.presentation.shared.components.EmberrPopupMenuSurface
import com.emberr.presentation.shared.components.MenuAtClickPositionProvider
import emberr.shared.generated.resources.editor_call_number
import emberr.shared.generated.resources.editor_copy_email
import emberr.shared.generated.resources.editor_copy_link_sentence_case
import emberr.shared.generated.resources.editor_copy_number
import emberr.shared.generated.resources.editor_open_link
import emberr.shared.generated.resources.editor_open_note
import emberr.shared.generated.resources.editor_send_email
import emberr.shared.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import emberr.shared.generated.resources.common_untitled

@Composable
fun LinkContextMenu(
    hoverState: LinkHoverState,
    onOpenNoteLink: (String) -> Unit = {},
    findNote: (String) -> NoteMetadataEntity? = { null }
) {
    val link = hoverState.rightClickedLink ?: return
    val webLinkActions = rememberWebLinkActions()
    val positionProvider = remember(hoverState.rightClickPosition) {
        MenuAtClickPositionProvider(
            IntOffset(
                hoverState.rightClickPosition.x.toInt(),
                hoverState.rightClickPosition.y.toInt()
            )
        )
    }

    Box(modifier = Modifier.size(1.dp)) {
        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = { hoverState.closeMenu() },
            properties = PopupProperties(focusable = true)
        ) {
            EmberrPopupMenuSurface {
                when (link) {
                    is HoveredLink.Web -> {
                        EmberrPopupMenuRow(stringResource(Res.string.editor_open_link)) {
                            hoverState.closeMenu()
                            webLinkActions.openLink(link.url)
                        }
                        EmberrPopupMenuRow(stringResource(Res.string.editor_copy_link_sentence_case)) {
                            hoverState.closeMenu()
                            webLinkActions.copyLink(link.url)
                        }
                    }
                    is HoveredLink.Note -> {
                        val untitledLabel = stringResource(Res.string.common_untitled)
                        val noteTitle = findNote(link.noteId)?.title?.ifBlank { untitledLabel } ?: untitledLabel
                        EmberrPopupMenuRow(stringResource(Res.string.editor_open_note)) {
                            hoverState.closeMenu()
                            onOpenNoteLink(link.noteId)
                        }
                        EmberrPopupMenuRow(stringResource(Res.string.editor_copy_link_sentence_case)) {
                            hoverState.closeMenu()
                            webLinkActions.copyLink("[$noteTitle](emberr://note/${link.noteId})")
                        }
                    }
                    is HoveredLink.Email -> {
                        EmberrPopupMenuRow(stringResource(Res.string.editor_send_email)) {
                            hoverState.closeMenu()
                            webLinkActions.openEmail(link.email)
                        }
                        EmberrPopupMenuRow(stringResource(Res.string.editor_copy_email)) {
                            hoverState.closeMenu()
                            webLinkActions.copyLink(link.email)
                        }
                    }
                    is HoveredLink.Phone -> {
                        EmberrPopupMenuRow(stringResource(Res.string.editor_call_number)) {
                            hoverState.closeMenu()
                            webLinkActions.openPhone(link.phone)
                        }
                        EmberrPopupMenuRow(stringResource(Res.string.editor_copy_number)) {
                            hoverState.closeMenu()
                            webLinkActions.copyLink(link.phone)
                        }
                    }
                }
            }
        }
    }
}
