package com.emberr.presentation.settings.importer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.emberr.domain.importer.ImportPaths
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.import_emberr_picker_title
import emberr.shared.generated.resources.import_markdown_picker_title
import org.jetbrains.compose.resources.stringResource
import java.awt.Dialog
import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager

@Composable
actual fun rememberImportFilePicker(onFilesPicked: (List<String>) -> Unit): () -> Unit {
    val latestOnFilesPicked by rememberUpdatedState(onFilesPicked)
    val latestPickerTitle by rememberUpdatedState(stringResource(Res.string.import_markdown_picker_title))
    return remember {
        {
            val dialog = fileDialogAboveActiveWindow(latestPickerTitle)
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
    val latestPickerTitle by rememberUpdatedState(stringResource(Res.string.import_emberr_picker_title))
    return remember {
        {
            val dialog = fileDialogAboveActiveWindow(latestPickerTitle)
            dialog.setFilenameFilter { _, name -> name.endsWith(".emberr", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true) }
            dialog.isVisible = true
            dialog.files.firstOrNull()?.let { file -> latestOnFilePicked(file.absolutePath) }
        }
    }
}

internal fun fileDialogAboveActiveWindow(title: String): FileDialog =
    when (val activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow) {
        is Dialog -> FileDialog(activeWindow, title, FileDialog.LOAD)
        is Frame -> FileDialog(activeWindow, title, FileDialog.LOAD)
        else -> FileDialog(null as Frame?, title, FileDialog.LOAD)
    }
