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
                IconTooltip(text = "Clear selection", position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.x), null, modifier = Modifier.size(iconSize).clickable { onClearSelection() }, tint = tint)
                }
                Text("$selectedCount", style = MaterialTheme.typography.bodyLarge, color = tint)
                divider()

                IconTooltip(text = if (isSelectionPinned) "Unpin" else "Pin", position = IconTooltipPosition.Above) {
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
                            if (isSelectionPinned) "Unpin Block" else "Pin Block",
                            modifier = Modifier.size(iconSize),
                            tint = tint
                        )
                    }
                }
                IconTooltip(text = "Delete", position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.trash), "Delete", modifier = Modifier.size(iconSize).clickable { onDelete() }, tint = tint)
                }
                divider()

                if (selectedLinkedNoteBlock != null) {
                    Box {
                        IconTooltip(text = "Preview", position = IconTooltipPosition.Above) {
                            Icon(
                                painterResource(Res.drawable.eye_open),
                                "Preview",
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
                    IconTooltip(text = "Style blocks", position = IconTooltipPosition.Above) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isStyleBarOpen) tint.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { onToggleStyleBar() }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(Res.drawable.text_tool_2), "Style Selected Blocks", modifier = Modifier.size(iconSize + 3.dp), tint = tint)
                        }
                    }
                    divider()
                }

                IconTooltip(text = "Select all", position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.select_all), "Select All", modifier = Modifier.size(iconSize).clickable { onSelectAll() }, tint = tint)
                }
                IconTooltip(text = "Copy", position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.copy), "Copy", modifier = Modifier.size(iconSize).clickable { onCopy() }, tint = tint)
                }
                IconTooltip(text = "Cut", position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.scissor2), "Cut", modifier = Modifier.size(iconSize).clickable { onCut() }, tint = tint)
                }
                divider()
                IconTooltip(text = "Add block above", position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.arrow_up), "Add above", modifier = Modifier.size(iconSize).clickable { onAddBlockAbove() }, tint = tint)
                }
                IconTooltip(text = "Add block below", position = IconTooltipPosition.Above) {
                    Icon(painterResource(Res.drawable.arrow_down), "Add below", modifier = Modifier.size(iconSize).clickable { onAddBlockBelow() }, tint = tint)
                }
            }
        }
    }
}
