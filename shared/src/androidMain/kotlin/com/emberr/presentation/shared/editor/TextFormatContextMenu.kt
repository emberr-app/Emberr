package com.emberr.presentation.shared.editor

import androidx.compose.runtime.Composable

@Composable
actual fun TextFormatContextMenu(
    onToggleFormat: (String) -> Unit,
    markdownClipboardActions: MarkdownClipboardActions?,
    content: @Composable () -> Unit
) {
    content()
}
