package com.emberr.presentation.people

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.settings.importer.fileDialogAboveActiveWindow
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.people_choose_contact_files
import emberr.shared.generated.resources.people_could_not_read_file
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import java.io.IOException

@Composable
actual fun rememberContactFilePicker(onFilesRead: (List<ByteArray>) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val latestOnFilesRead by rememberUpdatedState(onFilesRead)
    val pickerTitle = stringResource(Res.string.people_choose_contact_files)
    return remember(pickerTitle) {
        {
            val dialog = fileDialogAboveActiveWindow(pickerTitle)
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
                    if (fileContents.isEmpty()) showNativeToast(getString(Res.string.people_could_not_read_file)) else latestOnFilesRead(fileContents)
                }
            }
        }
    }
}
