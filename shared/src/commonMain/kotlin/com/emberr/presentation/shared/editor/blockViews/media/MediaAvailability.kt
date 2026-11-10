package com.emberr.presentation.shared.editor.blockViews.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.emberr.domain.sync.MediaTransferBytes
import com.emberr.domain.sync.MediaTransferPhase
import com.emberr.domain.sync.MediaTransferStatusBus
import com.emberr.domain.util.format.formatByteSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import emberr.shared.generated.resources.editor_downloading_progress
import emberr.shared.generated.resources.editor_uploading
import emberr.shared.generated.resources.editor_uploading_progress
import emberr.shared.generated.resources.editor_waiting_for_other_device_upload
import emberr.shared.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource

sealed class MediaAvailability {
    data object Available : MediaAvailability()
    data object Downloading : MediaAvailability()
    data object Failed : MediaAvailability()
}

// Determines media availability status for UI components using local disk state
// and real-time events from MediaTransferStatusBus.
@Composable
fun rememberMediaAvailability(absolutePath: String, fileName: String): MediaAvailability {
    var fileExists by remember(absolutePath) { mutableStateOf(File(absolutePath).exists()) }

    val inProgressPhase by remember(fileName) {
        MediaTransferStatusBus.inProgress.map { it[fileName] }
    }.collectAsState(initial = MediaTransferStatusBus.inProgress.collectAsState().value[fileName])

    val hasFailed by remember(fileName) {
        MediaTransferStatusBus.failed.map { fileName in it }
    }.collectAsState(initial = fileName in MediaTransferStatusBus.failed.collectAsState().value)

    // Checks for local file existence immediately when in-flight transfer state changes,
    // falling back to periodic polling if a file arrives outside the status bus.
    LaunchedEffect(absolutePath, inProgressPhase) {
        if (!fileExists) {
            val file = File(absolutePath)
            while (!file.exists()) {
                delay(1500L.milliseconds)
            }
            fileExists = true
        }
    }

    return remember(fileExists, inProgressPhase, hasFailed) {
        when {
            fileExists -> MediaAvailability.Available
            hasFailed && inProgressPhase == null -> MediaAvailability.Failed
            else -> MediaAvailability.Downloading
        }
    }
}

@Composable
fun rememberMediaTransferText(fileName: String, availability: MediaAvailability): String? {
    val phase by remember(fileName) { MediaTransferStatusBus.inProgress.map { it[fileName] } }
        .collectAsState(initial = MediaTransferStatusBus.inProgress.collectAsState().value[fileName])
    val bytes by remember(fileName) { MediaTransferStatusBus.bytes.map { it[fileName] } }
        .collectAsState(initial = null)
    val isWaitingForUpload by remember(fileName) { MediaTransferStatusBus.waitingForUpload.map { fileName in it } }
        .collectAsState(initial = fileName in MediaTransferStatusBus.waitingForUpload.collectAsState().value)

    return when {
        availability == MediaAvailability.Downloading && phase == MediaTransferPhase.DOWNLOADING ->
            bytes?.let { describeTransfer(Res.string.editor_downloading_progress, it) }
        availability == MediaAvailability.Downloading && isWaitingForUpload -> stringResource(Res.string.editor_waiting_for_other_device_upload)
        availability == MediaAvailability.Available && phase == MediaTransferPhase.UPLOADING ->
            bytes?.let { describeTransfer(Res.string.editor_uploading_progress, it) } ?: stringResource(Res.string.editor_uploading)
        else -> null
    }
}

@Composable
private fun describeTransfer(phrase: StringResource, bytes: MediaTransferBytes): String {
    val percent = if (bytes.total > 0) (bytes.done * 100 / bytes.total).coerceIn(0, 100) else 100
    return stringResource(phrase, percent, formatByteSize(bytes.done), formatByteSize(bytes.total))
}
