package com.emberr.presentation.shared.editor.components

import androidx.compose.ui.Modifier

enum class DesktopCursor { HAND, RESIZE_HORIZONTAL, RESIZE_VERTICAL }
expect fun Modifier.desktopPointerCursor(
    cursor: DesktopCursor,
    overrideDescendants: Boolean = false
): Modifier
