package com.emberr.presentation.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
expect fun fullKeyboardHeightPx(): Int

fun heightWhileKeyboardMoves(preferredHeight: Dp, spaceNow: Dp, keyboardNow: Dp, keyboardOpen: Dp): Dp {
    val spaceWithKeyboardOpen = spaceNow - (keyboardOpen - keyboardNow)
    val shrinkWithKeyboardOpen = (preferredHeight - spaceWithKeyboardOpen).coerceAtLeast(0.dp)
    val keyboardProgress = if (keyboardOpen > 0.dp) (keyboardNow / keyboardOpen).coerceIn(0f, 1f) else 0f
    return (preferredHeight - shrinkWithKeyboardOpen * keyboardProgress).coerceAtMost(spaceNow)
}
