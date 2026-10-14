package com.emberr.presentation.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent

class TitleBarRevealBlocker {
    var isPointerOverBlockingElement by mutableStateOf(false)
}

val LocalTitleBarRevealBlocker = staticCompositionLocalOf<TitleBarRevealBlocker?> { null }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.keepTitleBarHiddenWhileHovered(): Modifier {
    val blocker = LocalTitleBarRevealBlocker.current ?: return this
    DisposableEffect(blocker) {
        onDispose { blocker.isPointerOverBlockingElement = false }
    }
    return this
        .onPointerEvent(PointerEventType.Enter) { blocker.isPointerOverBlockingElement = true }
        .onPointerEvent(PointerEventType.Exit) { blocker.isPointerOverBlockingElement = false }
}
