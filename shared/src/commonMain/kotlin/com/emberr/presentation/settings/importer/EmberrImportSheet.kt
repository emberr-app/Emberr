package com.emberr.presentation.settings.importer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emberr.domain.backup.manual.EmberrImportProgress
import com.emberr.domain.backup.manual.EmberrImportResult
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrChoiceRow
import com.emberr.presentation.shared.components.EmberrSpaceCheckboxOptions
import com.emberr.presentation.shared.components.checkedSpacesLabel
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.folder

@Composable
fun EmberrImportSheet(
    pickedBackupState: PickedBackupState,
    importProgress: EmberrImportProgress,
    onFilePicked: (String) -> Unit,
    onImportClick: (spaceIds: Set<String>) -> Unit,
    onStopClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val openFilePicker = rememberBackupFilePicker(onFilePicked)
    val isImporting = importProgress is EmberrImportProgress.CopyingMedia ||
            importProgress == EmberrImportProgress.Saving ||
            importProgress == EmberrImportProgress.Stopping

    EmberrBottomSheet(
        expanded = true,
        onDismiss = onDismiss,
        title = "Emberr",
        subtitle = "Import a backup without replacing your current notes.",
        contentHorizontalPadding = 0.dp,
        isDismissible = !isImporting
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            when (importProgress) {
                EmberrImportProgress.NotStarted -> BackupChoices(
                    pickedBackupState = pickedBackupState,
                    onChooseFileClick = openFilePicker,
                    onImportClick = onImportClick,
                    onCloseClick = onDismiss
                )
                is EmberrImportProgress.CopyingMedia -> ImportProgressMessage(
                    doneFraction = if (importProgress.totalCount > 0) {
                        importProgress.copiedCount.toFloat() / importProgress.totalCount
                    } else {
                        0f
                    },
                    progressText = if (importProgress.totalCount > 0) {
                        "Copying media ${importProgress.copiedCount} of ${importProgress.totalCount}…"
                    } else {
                        "Getting ready…"
                    },
                    onStopClick = onStopClick
                )
                EmberrImportProgress.Saving -> ImportWorkingMessage("Saving the imported spaces…")
                EmberrImportProgress.Stopping -> ImportWorkingMessage("Stopping and removing what was imported…")
                EmberrImportProgress.Stopped -> ImportOutcomeMessage(
                    title = "Import stopped",
                    detailLines = listOf("Nothing was added."),
                    errorLine = null,
                    onDoneClick = onDismiss
                )
                is EmberrImportProgress.Finished -> ImportFinishedMessage(importProgress.result, onDoneClick = onDismiss)
                is EmberrImportProgress.Failed -> ImportOutcomeMessage(
                    title = "Import failed",
                    detailLines = listOf("Nothing was added."),
                    errorLine = importProgress.reason,
                    onDoneClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun BackupChoices(
    pickedBackupState: PickedBackupState,
    onChooseFileClick: () -> Unit,
    onImportClick: (spaceIds: Set<String>) -> Unit,
    onCloseClick: () -> Unit
) {
    val spaces = (pickedBackupState as? PickedBackupState.Ready)?.spaces.orEmpty()
    var checkedSpaceIds by remember(pickedBackupState) { mutableStateOf(spaces.map { it.spaceId }.toSet()) }

    ImportChooseBox(
        isClickable = pickedBackupState != PickedBackupState.Reading,
        onClick = onChooseFileClick,
        modifier = Modifier.padding(horizontal = ImportSheetContentPadding)
    ) {
        when (pickedBackupState) {
            PickedBackupState.NothingPicked -> ImportChoosePrompt(
                title = "Choose a Backup File",
                hint = ".emberr file from Export Backup"
            )
            PickedBackupState.Reading -> ImportReadingMessage("Reading the backup…")
            is PickedBackupState.CouldNotRead -> ImportFadedLine(text = "${pickedBackupState.reason} Tap to choose again.")
            is PickedBackupState.Ready -> ImportPickedSummary(
                pickedLabel = pickedBackupState.pickedName,
                countsLine = countLabel(spaces.size, "space"),
                extraLines = emptyList(),
                changeHint = "Choose a different file"
            )
        }
    }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        EmberrChoiceRow(
            label = "Spaces to Import",
            value = checkedSpacesLabel(spaces, checkedSpaceIds),
            icon = Res.drawable.folder
        ) {
            if (spaces.isEmpty()) {
                ImportMenuOption(label = "Choose a backup first", isSelected = false, isAvailable = false, onClick = {})
            } else {
                EmberrSpaceCheckboxOptions(
                    spaces = spaces,
                    checkedSpaceIds = checkedSpaceIds,
                    onCheckedSpaceIdsChange = { checkedSpaceIds = it }
                )
            }
        }
    }

    ImportStartButtons(
        isImportEnabled = checkedSpaceIds.isNotEmpty(),
        onImportClick = { onImportClick(checkedSpaceIds) },
        onCloseClick = onCloseClick
    )
}

@Composable
private fun ImportFinishedMessage(result: EmberrImportResult, onDoneClick: () -> Unit) {
    ImportOutcomeMessage(
        title = "Imported ${countLabel(result.importedSpaceNames.size, "space")}",
        detailLines = listOf(result.importedSpaceNames.joinToString(" · ")),
        errorLine = when (result.missingMediaCount) {
            0 -> null
            1 -> "Couldn't find 1 media file in the backup, so it will show as missing."
            else -> "Couldn't find ${result.missingMediaCount} media files in the backup, so they will show as missing."
        },
        onDoneClick = onDoneClick
    )
}
