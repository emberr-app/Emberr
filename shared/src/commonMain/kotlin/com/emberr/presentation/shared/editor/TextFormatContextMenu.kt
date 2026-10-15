package com.emberr.presentation.shared.editor

import androidx.compose.runtime.Composable

@Composable
expect fun TextFormatContextMenu(
    onToggleFormat: (String) -> Unit,
    markdownClipboardActions: MarkdownClipboardActions? = null,
    content: @Composable () -> Unit
)
