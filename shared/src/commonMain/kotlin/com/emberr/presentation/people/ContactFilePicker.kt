package com.emberr.presentation.people

import androidx.compose.runtime.Composable

@Composable
expect fun rememberContactFilePicker(onFilesRead: (List<ByteArray>) -> Unit): () -> Unit
