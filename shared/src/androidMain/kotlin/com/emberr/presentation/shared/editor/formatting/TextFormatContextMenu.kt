package com.emberr.presentation.shared.editor.formatting

import androidx.compose.runtime.Composable
import com.emberr.presentation.shared.editor.markdown.MarkdownClipboardActions

@Composable
actual fun TextFormatContextMenu(
    onToggleFormat: (String) -> Unit,
    markdownClipboardActions: MarkdownClipboardActions?,
    content: @Composable () -> Unit
) {
    content()
}
