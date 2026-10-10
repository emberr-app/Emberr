package com.emberr.presentation.settings.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

private val importPickerMimeTypes = arrayOf(
    "text/*",
    "application/json",
    "application/zip",
    "application/x-zip-compressed",
    "application/octet-stream"
)

@Composable
actual fun rememberImportFilePicker(onFilesPicked: (List<String>) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        onFilesPicked(uris.map { uri -> uri.toString() })
    }
    return { launcher.launch(importPickerMimeTypes) }
}
