package com.emberr.presentation.settings.importer

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.IOException

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun Modifier.importFileDropTarget(
    isEnabled: Boolean,
    onFilesDropped: (List<String>) -> Unit,
    onDragOverChange: (Boolean) -> Unit
): Modifier {
    val latestOnFilesDropped by rememberUpdatedState(onFilesDropped)
    val latestOnDragOverChange by rememberUpdatedState(onDragOverChange)
    val fileDropTarget = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) = latestOnDragOverChange(true)

            override fun onExited(event: DragAndDropEvent) = latestOnDragOverChange(false)

            override fun onEnded(event: DragAndDropEvent) = latestOnDragOverChange(false)

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val droppedPaths = droppedFilePaths(event)
                if (droppedPaths.isEmpty()) return false
                latestOnFilesDropped(droppedPaths)
                return true
            }
        }
    }
    return dragAndDropTarget(
        shouldStartDragAndDrop = { event ->
            isEnabled && event.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
        },
        target = fileDropTarget
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private fun droppedFilePaths(event: DragAndDropEvent): List<String> =
    try {
        (event.awtTransferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
            .orEmpty()
            .filterIsInstance<File>()
            .map { file -> file.absolutePath }
    } catch (error: UnsupportedFlavorException) {
        emptyList()
    } catch (error: IOException) {
        emptyList()
    }
