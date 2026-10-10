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

@Composable
actual fun rememberImportFilePicker(onFilesPicked: (List<String>) -> Unit): () -> Unit {
    val latestOnFilesPicked by rememberUpdatedState(onFilesPicked)
    return remember {
        {
            val dialog = fileDialogAboveActiveWindow()
            dialog.isMultipleMode = true
            dialog.setFilenameFilter { _, name -> ImportPaths.isSupportedPickedFile(name) }
            dialog.isVisible = true
            latestOnFilesPicked(dialog.files.map { file -> file.absolutePath })
        }
    }
}

private fun fileDialogAboveActiveWindow(): FileDialog =
    when (val activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow) {
        is Dialog -> FileDialog(activeWindow, IMPORT_PICKER_TITLE, FileDialog.LOAD)
        is Frame -> FileDialog(activeWindow, IMPORT_PICKER_TITLE, FileDialog.LOAD)
        else -> FileDialog(null as Frame?, IMPORT_PICKER_TITLE, FileDialog.LOAD)
    }
