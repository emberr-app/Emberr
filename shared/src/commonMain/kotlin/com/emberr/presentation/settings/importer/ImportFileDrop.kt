package com.emberr.presentation.settings.importer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun Modifier.importFileDropTarget(
    isEnabled: Boolean,
    onFilesDropped: (List<String>) -> Unit,
    onDragOverChange: (Boolean) -> Unit
): Modifier
