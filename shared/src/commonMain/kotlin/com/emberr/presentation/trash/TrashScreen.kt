package com.emberr.presentation.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.topHeaderBarPadding
import com.emberr.presentation.home.NoteCard
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.EmberrVerticalScrollbar
import com.emberr.presentation.shared.components.orHighContrastLine
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.common_back
import emberr.shared.generated.resources.common_cancel
import emberr.shared.generated.resources.trash_delete_permanently
import emberr.shared.generated.resources.trash_empty_trash
import emberr.shared.generated.resources.trash_empty_trash_question
import emberr.shared.generated.resources.trash_empty_trash_subtitle
import emberr.shared.generated.resources.trash_empty_trash_tooltip
import emberr.shared.generated.resources.trash_is_empty
import emberr.shared.generated.resources.trash_manage_note
import emberr.shared.generated.resources.trash_manage_note_subtitle
import emberr.shared.generated.resources.trash_restore_note
import emberr.shared.generated.resources.trash_title
import org.jetbrains.compose.resources.stringResource

private val HORIZONTAL_PADDING = 16.dp

@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = koinViewModel()
) {
    val trashedNotes by viewModel.trashedNotes.collectAsState()
    val parentTitlesByNoteId by viewModel.parentTitlesByNoteId.collectAsState()
    var selectedNoteToManage by remember { mutableStateOf<NoteMetadataEntity?>(null) }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    val backgroundColor = if (isDesktopPlatform) Color.Transparent else MaterialTheme.colorScheme.background
    val hazeState = remember { HazeState() }

    val gridState = rememberLazyGridState()
    val topEdgeFadeAlpha by remember {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0) 1f else 0f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (trashedNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .background(backgroundColor)
                    .padding(top = topBarHeightDp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.trash_is_empty),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(
                    top = topBarHeightDp + 8.dp,
                    bottom = 80.dp,
                    start = HORIZONTAL_PADDING,
                    end = HORIZONTAL_PADDING
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .background(backgroundColor)
            ) {
                items(trashedNotes, key = { it.noteId }) { note ->
                    if (isDesktopPlatform) {
                        DesktopTrashNoteCard(
                            note = note,
                            parentTitle = parentTitlesByNoteId[note.noteId],
                            onRestore = { viewModel.restoreNote(note.noteId) },
                            onPermanentlyDelete = { viewModel.permanentlyDelete(note.noteId, note.filePath) }
                        )
                    } else {
                        NoteCard(
                            note = note,
                            isSelected = false,
                            onClick = { selectedNoteToManage = note },
                            onLongClick = { selectedNoteToManage = note },
                            parentTitle = parentTitlesByNoteId[note.noteId]
                        )
                    }
                }
            }

            EmberrVerticalScrollbar(
                gridState = gridState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = topBarHeightDp + 8.dp, bottom = 24.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
                .onGloballyPositioned { coordinates -> topBarHeightPx = coordinates.size.height.toFloat() }
        ) {
            EmberrTopHeaderBar(
                title = stringResource(Res.string.trash_title),
                hazeState = hazeState,
                contentPadding = topHeaderBarPadding(bottom = 16.dp),
                topEdgeFadeAlpha = topEdgeFadeAlpha,
                backTooltip = stringResource(Res.string.common_back),
                onBackClick = onNavigateBack,
                actions = {
                    if (trashedNotes.isNotEmpty()) {
                        TopBarIconButton(
                            icon = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(Res.string.trash_empty_trash),
                            bgColor = Color.Transparent,
                            tint = MaterialTheme.colorScheme.error,
                            hazeState = hazeState,
                            hazeStyle = EmberrBlur.Regular,
                            tooltip = stringResource(Res.string.trash_empty_trash_tooltip),
                            onClick = { showEmptyTrashConfirm = true }
                        )
                    }
                }
            )
        }

        ManageNoteBottomSheet(
            expanded = selectedNoteToManage != null,
            onDismiss = { selectedNoteToManage = null },
            onRestore = {
                val noteId = selectedNoteToManage?.noteId ?: return@ManageNoteBottomSheet
                selectedNoteToManage = null
                viewModel.restoreNote(noteId)
            },
            onPermanentlyDelete = {
                val note = selectedNoteToManage ?: return@ManageNoteBottomSheet
                selectedNoteToManage = null
                viewModel.permanentlyDelete(note.noteId, note.filePath)
            }
        )

        EmptyTrashBottomSheet(
            expanded = showEmptyTrashConfirm,
            onDismiss = { showEmptyTrashConfirm = false },
            onConfirmEmpty = {
                showEmptyTrashConfirm = false
                viewModel.emptyTrash()
            }
        )
    }
}

@Composable
private fun DesktopTrashNoteCard(
    note: NoteMetadataEntity,
    parentTitle: String?,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }

    Box(
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                var pressEvent = awaitPointerEvent()
                while (pressEvent.type != PointerEventType.Press) pressEvent = awaitPointerEvent()
                val press = pressEvent.changes.first()
                val isRightClick = pressEvent.buttons.isSecondaryPressed
                val isClickFinished = isRightClick || waitForUpOrCancellation() != null
                if (isClickFinished) {
                    menuOffset = DpOffset(press.position.x.toDp(), press.position.y.toDp())
                    isMenuOpen = true
                }
            }
        }
    ) {
        NoteCard(
            note = note,
            isSelected = false,
            onClick = {},
            onLongClick = {},
            handlesGestures = false,
            parentTitle = parentTitle
        )

        Box(modifier = Modifier.offset(x = menuOffset.x, y = menuOffset.y)) {
            EmberrDesktopMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false }
            ) {
                EmberrDesktopMenuOption(
                    label = stringResource(Res.string.trash_restore_note),
                    icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        isMenuOpen = false
                        onRestore()
                    }
                )
                EmberrDesktopMenuOption(
                    label = stringResource(Res.string.trash_delete_permanently),
                    icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) },
                    labelColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        isMenuOpen = false
                        onPermanentlyDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun ManageNoteBottomSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit
) {
    EmberrBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = stringResource(Res.string.trash_manage_note),
        subtitle = stringResource(Res.string.trash_manage_note_subtitle)
    ) { closeAnd ->

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            BottomSheetActionItem(Icons.Default.Restore, stringResource(Res.string.trash_restore_note)) {
                closeAnd(onRestore)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f).orHighContrastLine()
            )

            BottomSheetActionItem(
                Icons.Default.DeleteForever,
                stringResource(Res.string.trash_delete_permanently),
                isDestructive = true
            ) {
                closeAnd(onPermanentlyDelete)
            }

            EmberrButtonPrimary(
                text = stringResource(Res.string.common_cancel),
                onClick = { closeAnd(onDismiss) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
fun EmptyTrashBottomSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onConfirmEmpty: () -> Unit
) {
    val scope = rememberCoroutineScope()

    EmberrBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = stringResource(Res.string.trash_empty_trash_question),
        subtitle = stringResource(Res.string.trash_empty_trash_subtitle)
    ) { closeAnd ->

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            BottomSheetActionItem(Icons.Default.DeleteSweep, stringResource(Res.string.trash_empty_trash), isDestructive = true) {
                closeAnd {
                    scope.launch { delay(250.milliseconds); onConfirmEmpty() }
                }
            }

            EmberrButtonPrimary(
                text = stringResource(Res.string.common_cancel),
                onClick = { closeAnd(onDismiss) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun BottomSheetActionItem(icon: ImageVector, text: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onClick() }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = textColor)
    }
}
