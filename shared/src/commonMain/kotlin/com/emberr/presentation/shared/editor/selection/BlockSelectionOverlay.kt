package com.emberr.presentation.shared.editor.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.ui.theme.LocalAppIsDark
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.unit.Dp

@Composable
internal fun BoxScope.BlockSelectionOverlay(
    blockId: String,
    isSelected: Boolean,
    inSelectionMode: Boolean,
    horizontalPadding: Dp,
    onToggleSelection: () -> Unit
) {
    val isDarkTheme = LocalAppIsDark.current
    val crossBlockSelection = LocalCrossBlockSelection.current
    var selectionOverlayTopLeftInWindow by remember { mutableStateOf(Offset.Zero) }
    val leaveBlockSelectionAt: (Offset) -> Unit = leave@{ clickInWindow ->
        val selection = crossBlockSelection ?: return@leave
        val caret = selection.positionAt(clickInWindow, mustBeInsideBlock = true)
            ?.takeIf { it.blockId == blockId && selection.hasTextField(blockId) }
        if (caret != null) selection.host?.placeCaret(caret) else selection.host?.clearBlockSelection()
    }

    if (isSelected) {
        Box(
            Modifier
                .matchParentSize()
                .padding(horizontal = horizontalPadding)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.15f else 0.1f))
        )
    }

    if (inSelectionMode) {
        val selectedBlocksClickModifier = if (isDesktopPlatform) {
            Modifier
                .onGloballyPositioned { selectionOverlayTopLeftInWindow = it.positionInWindow() }
                .pointerInput(blockId) {
                    awaitEachGesture {
                        awaitFirstDown()
                        val pressedKeys = currentEvent.keyboardModifiers
                        val togglesThisBlock = pressedKeys.isCtrlPressed || pressedKeys.isMetaPressed
                        val release = waitForUpOrCancellation() ?: return@awaitEachGesture
                        release.consume()
                        if (togglesThisBlock) {
                            onToggleSelection()
                        } else {
                            leaveBlockSelectionAt(selectionOverlayTopLeftInWindow + release.position)
                        }
                    }
                }
        } else {
            Modifier.clickable(onClick = { onToggleSelection() })
        }
        Box(Modifier.matchParentSize().then(selectedBlocksClickModifier))
    }
}
