package com.emberr.presentation.settings.importer

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImportFilePicker(onFilesPicked: (List<String>) -> Unit): () -> Unit

@Composable
expect fun rememberBackupFilePicker(onFilePicked: (String) -> Unit): () -> Unit
