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
import com.emberr.domain.backup.manual.BackupContentChoice
import com.emberr.domain.backup.manual.EmberrImportProgress
import com.emberr.domain.backup.manual.EmberrImportResult
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrChoiceRow
import com.emberr.presentation.shared.components.EmberrSpaceCheckboxOptions
import com.emberr.presentation.shared.components.EmberrSwitchRow
import com.emberr.presentation.shared.components.checkedSpacesLabel
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.calendar
import emberr.shared.generated.resources.daily
import emberr.shared.generated.resources.folder

@Composable
fun EmberrImportSheet(
    pickedBackupState: PickedBackupState,
    importProgress: EmberrImportProgress,
    onFilePicked: (String) -> Unit,
    onImportClick: (choice: BackupContentChoice) -> Unit,
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
                    onFileDropped = onFilePicked,
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
                EmberrImportProgress.Saving -> ImportWorkingMessage("Saving the import…")
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
    onFileDropped: (String) -> Unit,
    onImportClick: (choice: BackupContentChoice) -> Unit,
    onCloseClick: () -> Unit
) {
    val readyBackup = pickedBackupState as? PickedBackupState.Ready
    val spaces = readyBackup?.spaces.orEmpty()
    var checkedSpaceIds by remember(pickedBackupState) { mutableStateOf(spaces.map { it.spaceId }.toSet()) }
    var wantsDailyNotes by remember(pickedBackupState) { mutableStateOf(true) }
    var wantsCalendar by remember(pickedBackupState) { mutableStateOf(true) }
    val choice = BackupContentChoice(
        spaceIds = checkedSpaceIds,
        includesDailyNotes = readyBackup?.hasDailyNotes == true && wantsDailyNotes,
        includesCalendar = readyBackup?.hasCalendar == true && wantsCalendar
    )

    ImportChooseBox(
        isClickable = pickedBackupState != PickedBackupState.Reading,
        onClick = onChooseFileClick,
        onFilesDropped = { droppedPaths -> droppedPaths.firstOrNull()?.let(onFileDropped) },
        modifier = Modifier.padding(horizontal = ImportSheetContentPadding)
    ) {
        when (pickedBackupState) {
            PickedBackupState.NothingPicked -> ImportChoosePrompt(
                title = "Choose a Backup File",
                hint = ".emberr file from Export Backup"
            )
            PickedBackupState.Reading -> ImportReadingMessage("Reading the backup…")
            is PickedBackupState.CouldNotRead -> ImportFadedLine(text = "${pickedBackupState.reason} $importChooseAgainHint")
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
        if (readyBackup?.hasDailyNotes == true) {
            EmberrSwitchRow(
                label = "Daily Notes",
                icon = Res.drawable.daily,
                isOn = wantsDailyNotes,
                onClick = { wantsDailyNotes = !wantsDailyNotes }
            )
        }
        if (readyBackup?.hasCalendar == true) {
            EmberrSwitchRow(
                label = "Calendar",
                icon = Res.drawable.calendar,
                isOn = wantsCalendar,
                onClick = { wantsCalendar = !wantsCalendar }
            )
        }
    }

    ImportStartButtons(
        isImportEnabled = choice.spaceIds.isNotEmpty() || choice.includesDailyNotes || choice.includesCalendar,
        onImportClick = { onImportClick(choice) },
        onCloseClick = onCloseClick
    )
}

@Composable
private fun ImportFinishedMessage(result: EmberrImportResult, onDoneClick: () -> Unit) {
    val addedLine = when {
        result.addedDailyNotes && result.addedCalendar -> "Daily notes and calendar events added"
        result.addedDailyNotes -> "Daily notes added"
        result.addedCalendar -> "Calendar events added"
        else -> null
    }
    ImportOutcomeMessage(
        title = if (result.importedSpaceNames.isEmpty()) "Import finished" else "Imported ${countLabel(result.importedSpaceNames.size, "space")}",
        detailLines = listOfNotNull(result.importedSpaceNames.joinToString(" · ").ifEmpty { null }, addedLine),
        errorLine = when (result.missingMediaCount) {
            0 -> null
            1 -> "Couldn't find 1 media file in the backup, so it will show as missing."
            else -> "Couldn't find ${result.missingMediaCount} media files in the backup, so they will show as missing."
        },
        onDoneClick = onDoneClick
    )
}
