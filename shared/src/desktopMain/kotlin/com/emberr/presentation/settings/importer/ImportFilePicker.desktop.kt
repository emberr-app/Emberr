package com.emberr.presentation.settings.importer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.emberr.domain.importer.ImportPaths
import java.awt.Dialog
import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager

private const val IMPORT_PICKER_TITLE = "Choose Files to Import"
private const val BACKUP_PICKER_TITLE = "Choose an Emberr Backup"

@Composable
actual fun rememberImportFilePicker(onFilesPicked: (List<String>) -> Unit): () -> Unit {
    val latestOnFilesPicked by rememberUpdatedState(onFilesPicked)
    return remember {
        {
            val dialog = fileDialogAboveActiveWindow(IMPORT_PICKER_TITLE)
            dialog.isMultipleMode = true
            dialog.setFilenameFilter { _, name -> ImportPaths.isSupportedPickedFile(name) }
            dialog.isVisible = true
            latestOnFilesPicked(dialog.files.map { file -> file.absolutePath })
        }
    }
}

@Composable
actual fun rememberBackupFilePicker(onFilePicked: (String) -> Unit): () -> Unit {
    val latestOnFilePicked by rememberUpdatedState(onFilePicked)
    return remember {
        {
            val dialog = fileDialogAboveActiveWindow(BACKUP_PICKER_TITLE)
            dialog.setFilenameFilter { _, name -> name.endsWith(".emberr", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true) }
            dialog.isVisible = true
            dialog.files.firstOrNull()?.let { file -> latestOnFilePicked(file.absolutePath) }
        }
    }
}

private fun fileDialogAboveActiveWindow(title: String): FileDialog =
    when (val activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow) {
        is Dialog -> FileDialog(activeWindow, title, FileDialog.LOAD)
        is Frame -> FileDialog(activeWindow, title, FileDialog.LOAD)
        else -> FileDialog(null as Frame?, title, FileDialog.LOAD)
    }
