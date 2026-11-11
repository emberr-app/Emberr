package com.emberr.presentation.ai.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.domain.ai.models.ModelDownloadProgress
import com.emberr.domain.ai.models.ModelFileNames
import com.emberr.presentation.ai.LocalModelUploadState
import com.emberr.presentation.ai.RagViewModel
import com.emberr.presentation.ai.text
import com.emberr.presentation.ai.components.ModelOptionCard
import com.emberr.presentation.ai.components.UnsupportedHardwareNotice
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrTextField
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.ai_context_length
import emberr.shared.generated.resources.ai_context_length_hint
import emberr.shared.generated.resources.ai_copying_model_file
import emberr.shared.generated.resources.ai_delete_model
import emberr.shared.generated.resources.ai_download
import emberr.shared.generated.resources.ai_downloading_percent
import emberr.shared.generated.resources.ai_got_it
import emberr.shared.generated.resources.ai_installed_models
import emberr.shared.generated.resources.ai_local_ai
import emberr.shared.generated.resources.ai_manage_on_device_models
import emberr.shared.generated.resources.ai_model_pending_deletion
import emberr.shared.generated.resources.ai_pause_download
import emberr.shared.generated.resources.ai_paused
import emberr.shared.generated.resources.ai_proceed
import emberr.shared.generated.resources.ai_recommended_balanced
import emberr.shared.generated.resources.ai_recommended_downloaded
import emberr.shared.generated.resources.ai_restore_model
import emberr.shared.generated.resources.ai_resume_download
import emberr.shared.generated.resources.ai_unverified_model_type
import emberr.shared.generated.resources.ai_upload_your_own_model
import emberr.shared.generated.resources.ai_uploaded_successfully
import emberr.shared.generated.resources.ai_uploading_custom_model
import emberr.shared.generated.resources.ai_uploading_custom_model_warning
import emberr.shared.generated.resources.common_cancel
import emberr.shared.generated.resources.common_close
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LocalAiSettingsSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    viewModel: RagViewModel,
    onPickDocument: (onPathSelected: (String) -> Unit) -> Unit
) {
    var isResumable by remember { mutableStateOf(false) }
    val downloadProgress by viewModel.localGeneratorDownloadProgress.collectAsState()
    val uploadState by viewModel.localModelUploadState.collectAsState()
    val uploadProgress by viewModel.localModelUploadProgress.collectAsState()
    val installedLocalModels by viewModel.installedLocalModels.collectAsState()
    val selectedLocalModelFileName by viewModel.selectedLocalModelFileName.collectAsState()
    val pendingDeletionFileNames by viewModel.pendingDeletionLocalModelFileNames.collectAsState()
    val localContextLength by viewModel.localContextLength.collectAsState()
    val finetuneWarning by viewModel.uploadedModelFinetuneWarning.collectAsState()
    var showUploadWarning by remember { mutableStateOf(false) }
    var contextLengthInput by remember(localContextLength) { mutableStateOf(localContextLength.toString()) }

    val isDefaultModelInstalled = installedLocalModels.any { it.fileName == ModelFileNames.GENERATOR }

    LaunchedEffect(expanded) {
        if (expanded) {
            viewModel.refreshInstalledLocalModels()
            isResumable = viewModel.hasResumableGeneratorDownload()
        } else {
            viewModel.resetLocalModelUploadState()
        }
    }

    LaunchedEffect(downloadProgress) {
        if (downloadProgress is ModelDownloadProgress.Failed || downloadProgress is ModelDownloadProgress.Paused) {
            isResumable = viewModel.hasResumableGeneratorDownload()
        }
        if (downloadProgress is ModelDownloadProgress.Completed) {
            isResumable = false
        }
    }

    EmberrBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = stringResource(Res.string.ai_local_ai),
        subtitle = stringResource(Res.string.ai_manage_on_device_models),
    ) { _ ->
        val unsupportedHardwareReason = viewModel.localAiUnsupportedReason
        if (unsupportedHardwareReason != null) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                UnsupportedHardwareNotice(reason = unsupportedHardwareReason)
                EmberrButtonPrimary(
                    text = stringResource(Res.string.common_close),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            }
            return@EmberrBottomSheet
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            val currentDownloadProgress = downloadProgress
            val isDownloading = currentDownloadProgress is ModelDownloadProgress.Downloading

            ModelOptionCard(
                icon = null,
                title = "qwen2.5-1.5b-instruct-q8_0",
                subtitle = when (currentDownloadProgress) {
                    is ModelDownloadProgress.Downloading ->
                        stringResource(Res.string.ai_downloading_percent, (currentDownloadProgress.fraction * 100).toInt())
                    is ModelDownloadProgress.Failed -> currentDownloadProgress.problem.text()
                    ModelDownloadProgress.Paused -> stringResource(Res.string.ai_paused)
                    else -> if (isDefaultModelInstalled)
                        stringResource(Res.string.ai_recommended_downloaded)
                    else
                        stringResource(Res.string.ai_recommended_balanced)
                },
                onClick = {}
            )

            if (isDownloading || !isDefaultModelInstalled) {
                Spacer(Modifier.height(10.dp))
                EmberrButtonPrimary(
                    text = when {
                        isDownloading -> stringResource(Res.string.ai_pause_download)
                        isResumable -> stringResource(Res.string.ai_resume_download)
                        else -> stringResource(Res.string.ai_download)
                    },
                    onClick = {
                        if (isDownloading) {
                            viewModel.pauseGeneratorModelDownload()
                        } else {
                            viewModel.downloadGeneratorModel()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(10.dp))

            val currentUploadState = uploadState
            val isUploading = currentUploadState is LocalModelUploadState.Uploading

            EmberrButtonPrimary(
                text = if (isUploading)
                    stringResource(Res.string.ai_copying_model_file, (uploadProgress * 100).toInt())
                else
                    stringResource(Res.string.ai_upload_your_own_model),
                enabled = !isUploading,
                onClick = { showUploadWarning = true },
                modifier = Modifier.fillMaxWidth()
            )

            val uploadStatusMessage = when (currentUploadState) {
                LocalModelUploadState.Success -> stringResource(Res.string.ai_uploaded_successfully)
                is LocalModelUploadState.Failed -> currentUploadState.problem.text()
                else -> null
            }
            if (uploadStatusMessage != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = uploadStatusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.ai_context_length),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            EmberrTextField(
                value = contextLengthInput,
                onValueChange = { input ->
                    val digitsOnly = input.filter { it.isDigit() }
                    contextLengthInput = digitsOnly
                    digitsOnly.toIntOrNull()?.let { tokens ->
                        if (tokens > 0) viewModel.selectLocalContextLength(tokens)
                    }
                },
                placeholder = "4096",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.ai_context_length_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            if (installedLocalModels.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(Res.string.ai_installed_models),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))

                installedLocalModels.forEach { model ->
                    val isPendingDeletion = model.fileName in pendingDeletionFileNames
                    EmberrBottomSheetOption(
                        label = model.displayName,
                        labelMaxLines = 1,
                        isSelected = !isPendingDeletion && selectedLocalModelFileName == model.fileName,
                        subtitle = if (isPendingDeletion)
                            stringResource(Res.string.ai_model_pending_deletion)
                        else
                            null,
                        outerHorizontalPadding = 0.dp,
                        onClick = {
                            if (!isPendingDeletion) viewModel.selectLocalModel(model.fileName)
                        },
                        trailing = {
                            if (isPendingDeletion) {
                                Spacer(Modifier.width(6.dp))
                                IconTooltip(text = stringResource(Res.string.ai_restore_model)) {
                                    Icon(
                                        Icons.Default.Restore,
                                        contentDescription = stringResource(Res.string.ai_restore_model),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { viewModel.restoreLocalModel(model.fileName) }
                                    )
                                }
                                Spacer(Modifier.width(5.dp))
                            } else {
                                Spacer(Modifier.width(6.dp))
                                IconTooltip(text = stringResource(Res.string.ai_delete_model)) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(Res.string.ai_delete_model),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { viewModel.deleteLocalModel(model.fileName) }
                                    )
                                }
                                Spacer(Modifier.width(5.dp))
                            }
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            EmberrButtonPrimary(
                text = stringResource(Res.string.common_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }
    }

    if (showUploadWarning) {
        EmberrAlertDialog(
            onDismissRequest = { showUploadWarning = false },
            title = stringResource(Res.string.ai_uploading_custom_model)
        ) {
            Text(
                text = stringResource(Res.string.ai_uploading_custom_model_warning),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EmberrButtonSecondary(
                    text = stringResource(Res.string.common_cancel),
                    onClick = { showUploadWarning = false },
                    modifier = Modifier.weight(1f)
                )
                EmberrButtonPrimary(
                    text = stringResource(Res.string.ai_proceed),
                    onClick = {
                        showUploadWarning = false
                        onPickDocument { path -> viewModel.uploadGeneratorModel(path) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (finetuneWarning != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUploadedModelFinetuneWarning() },
            title = {
                Text(stringResource(Res.string.ai_unverified_model_type), style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(finetuneWarning.orEmpty(), style = MaterialTheme.typography.labelSmall)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissUploadedModelFinetuneWarning() }) {
                    Text(stringResource(Res.string.ai_got_it), style = MaterialTheme.typography.bodyLarge)
                }
            }
        )
    }
}
