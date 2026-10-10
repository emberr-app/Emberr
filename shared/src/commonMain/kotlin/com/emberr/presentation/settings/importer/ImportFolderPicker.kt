package com.emberr.presentation.settings.importer

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImportFolderPicker(onFolderPicked: (String) -> Unit): (() -> Unit)?
