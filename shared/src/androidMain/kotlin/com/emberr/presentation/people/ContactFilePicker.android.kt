package com.emberr.presentation.people

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.emberr.domain.util.system.showNativeToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val contactFileMimeTypes = arrayOf("text/vcard", "text/x-vcard", "text/directory")

@Composable
actual fun rememberContactFilePicker(onFilesRead: (List<ByteArray>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnFilesRead by rememberUpdatedState(onFilesRead)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val fileContents = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream -> stream.readBytes() }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }
            if (fileContents.isEmpty()) showNativeToast("Couldn't read the file") else latestOnFilesRead(fileContents)
        }
    }
    return { launcher.launch(contactFileMimeTypes) }
}
