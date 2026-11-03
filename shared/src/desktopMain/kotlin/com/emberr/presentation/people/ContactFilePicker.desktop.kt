package com.emberr.presentation.people

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.settings.importer.fileDialogAboveActiveWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private const val CONTACT_FILE_PICKER_TITLE = "Choose Contact Files"

@Composable
actual fun rememberContactFilePicker(onFilesRead: (List<ByteArray>) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val latestOnFilesRead by rememberUpdatedState(onFilesRead)
    return remember {
        {
            val dialog = fileDialogAboveActiveWindow(CONTACT_FILE_PICKER_TITLE)
            dialog.isMultipleMode = true
            dialog.setFilenameFilter { _, name -> name.endsWith(".vcf", ignoreCase = true) || name.endsWith(".vcard", ignoreCase = true) }
            dialog.isVisible = true
            val pickedFiles = dialog.files.toList()
            if (pickedFiles.isNotEmpty()) {
                scope.launch {
                    val fileContents = withContext(Dispatchers.IO) {
                        pickedFiles.mapNotNull { file ->
                            try {
                                file.readBytes()
                            } catch (e: IOException) {
                                e.printStackTrace()
                                null
                            }
                        }
                    }
                    if (fileContents.isEmpty()) showNativeToast("Couldn't read the file") else latestOnFilesRead(fileContents)
                }
            }
        }
    }
}
