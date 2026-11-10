package com.emberr.presentation.shared.editor.selection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.BOTTOM_BAR_BOTTOM_PADDING
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.IconTooltipPosition
import com.emberr.presentation.shared.components.customEmberrShadow
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.editor.blockViews.LinkedNoteOptionsMenu
import dev.chrisbanes.haze.HazeState
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.arrow_down
import emberr.shared.generated.resources.arrow_up
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.eye_open
import emberr.shared.generated.resources.scissor2
import emberr.shared.generated.resources.select_all
import emberr.shared.generated.resources.text_tool_2
import emberr.shared.generated.resources.thumbtack
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.x
import org.jetbrains.compose.resources.painterResource
import com.emberr.presentation.shared.editor.toolbar.DefaultCornerShape
import emberr.shared.generated.resources.editor_pin
import emberr.shared.generated.resources.editor_pin_block
import emberr.shared.generated.resources.editor_unpin
import emberr.shared.generated.resources.editor_unpin_block
import org.jetbrains.compose.resources.stringResource
import emberr.shared.generated.resources.common_delete
import emberr.shared.generated.resources.editor_add_above
import emberr.shared.generated.resources.editor_add_below
import emberr.shared.generated.resources.editor_add_block_above_sentence_case
import emberr.shared.generated.resources.editor_add_block_below_sentence_case
import emberr.shared.generated.resources.editor_clear_selection_sentence_case
import emberr.shared.generated.resources.editor_copy
import emberr.shared.generated.resources.editor_cut
import emberr.shared.generated.resources.editor_preview
import emberr.shared.generated.resources.editor_select_all
import emberr.shared.generated.resources.editor_select_all_title_case
import emberr.shared.generated.resources.editor_style_blocks
import emberr.shared.generated.resources.editor_style_selected_blocks

@Composable
fun SelectionModeObserver(isSelectionMode: Boolean, onSelectionModeChange: (Boolean) -> Unit) {
    LaunchedEffect(isSelectionMode) { onSelectionModeChange(isSelectionMode) }
}

@Composable
fun BlockSelectionPill(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onAddBlockAbove: () -> Unit,
    onAddBlockBelow: () -> Unit,
    onTogglePin: () -> Unit,
    isSelectionPinned: Boolean = false,
    selectedBlocks: List<NoteBlock> = emptyList(),
    onUpdateLinkedNoteOptions: (id: String, showIcon: Boolean, showCoverImage: Boolean) -> Unit = { _, _, _ -> },
    showStyleButton: Boolean = false,
    isStyleBarOpen: Boolean = false,
    onToggleStyleBar: () -> Unit = {},
    hazeState: HazeState
) {
    val selectedLinkedNoteBlock = selectedBlocks.singleOrNull() as? LinkedNoteBlock
    var showLinkedNoteOptions by remember { mutableStateOf(false) }

    val tint = MaterialTheme.colorScheme.primary

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        Surface(
            shape = DefaultCornerShape,
            color = Color.Transparent,
            modifier = Modifier
                .padding(bottom = if (isDesktopPlatform) 32.dp else BOTTOM_BAR_BOTTOM_PADDING)
                .emberrBorder(DefaultCornerShape)
                .customEmberrShadow(DefaultCornerShape)
                .clip(DefaultCornerShape)
                .emberrBlur(hazeState, EmberrBlur.Regular)
        ) {
            val scrollState = rememberScrollState()
            val divider = @Composable {
                Box(Modifier.width(1.dp).height(18.dp).background(tint.copy(alpha = 0.2f)))
            }

            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                val iconSize = 18.dp
                IconTooltip(text = stringResource(Res.string.editor_clear_selection_sentence_case), position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.x), null, modifier = Modifier.size(iconSize).clickable { onClearSelection() }, tint = tint)
                }
                Text("$selectedCount", style = MaterialTheme.typography.bodyLarge, color = tint)
                divider()

                IconTooltip(text = if (isSelectionPinned) stringResource(Res.string.editor_unpin) else stringResource(Res.string.editor_pin), position = IconTooltipPosition.Above) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelectionPinned) tint.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { onTogglePin() }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painterResource(Res.drawable.thumbtack),
                            if (isSelectionPinned) stringResource(Res.string.editor_unpin_block) else stringResource(Res.string.editor_pin_block),
                            modifier = Modifier.size(iconSize),
                            tint = tint
                        )
                    }
                }
                IconTooltip(text = stringResource(Res.string.common_delete), position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.trash), stringResource(Res.string.common_delete), modifier = Modifier.size(iconSize).clickable { onDelete() }, tint = tint)
                }
                divider()

                if (selectedLinkedNoteBlock != null) {
                    Box {
                        IconTooltip(text = stringResource(Res.string.editor_preview), position = IconTooltipPosition.Above) {
                            Icon(
                                painterResource(Res.drawable.eye_open),
                                stringResource(Res.string.editor_preview),
                                modifier = Modifier.size(iconSize + 3.dp).clickable { showLinkedNoteOptions = true },
                                tint = tint
                            )
                        }
                        LinkedNoteOptionsMenu(
                            expanded = showLinkedNoteOptions,
                            onDismiss = { showLinkedNoteOptions = false },
                            block = selectedLinkedNoteBlock,
                            onUpdateOptions = { showIcon, showCoverImage ->
                                onUpdateLinkedNoteOptions(selectedLinkedNoteBlock.id, showIcon, showCoverImage)
                            }
                        )
                    }
                    divider()
                }

                if (showStyleButton) {
                    IconTooltip(text = stringResource(Res.string.editor_style_blocks), position = IconTooltipPosition.Above) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isStyleBarOpen) tint.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { onToggleStyleBar() }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(Res.drawable.text_tool_2), stringResource(Res.string.editor_style_selected_blocks), modifier = Modifier.size(iconSize + 3.dp), tint = tint)
                        }
                    }
                    divider()
                }

                IconTooltip(text = stringResource(Res.string.editor_select_all), position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.select_all), stringResource(Res.string.editor_select_all_title_case), modifier = Modifier.size(iconSize).clickable { onSelectAll() }, tint = tint)
                }
                IconTooltip(text = stringResource(Res.string.editor_copy), position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.copy), stringResource(Res.string.editor_copy), modifier = Modifier.size(iconSize).clickable { onCopy() }, tint = tint)
                }
                IconTooltip(text = stringResource(Res.string.editor_cut), position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.scissor2), stringResource(Res.string.editor_cut), modifier = Modifier.size(iconSize).clickable { onCut() }, tint = tint)
                }
                divider()
                IconTooltip(text = stringResource(Res.string.editor_add_block_above_sentence_case), position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.arrow_up), stringResource(Res.string.editor_add_above), modifier = Modifier.size(iconSize).clickable { onAddBlockAbove() }, tint = tint)
                }
                IconTooltip(text = stringResource(Res.string.editor_add_block_below_sentence_case), position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.arrow_down), stringResource(Res.string.editor_add_below), modifier = Modifier.size(iconSize).clickable { onAddBlockBelow() }, tint = tint)
                }
            }
        }
    }
}
