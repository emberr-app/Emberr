package com.emberr.presentation.settings.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
actual fun rememberImportFolderPicker(onFolderPicked: (String) -> Unit): (() -> Unit)? {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folderUri ->
        if (folderUri != null) onFolderPicked(folderUri.toString())
    }
    return { launcher.launch(null) }
}
