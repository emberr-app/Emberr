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
import emberr.shared.generated.resources.import_emberr_added_calendar
import emberr.shared.generated.resources.import_emberr_added_daily_notes
import emberr.shared.generated.resources.import_emberr_added_daily_notes_and_calendar
import emberr.shared.generated.resources.import_emberr_calendar
import emberr.shared.generated.resources.import_emberr_choose_backup_file
import emberr.shared.generated.resources.import_emberr_choose_backup_first
import emberr.shared.generated.resources.import_emberr_choose_backup_hint
import emberr.shared.generated.resources.import_emberr_choose_different_file
import emberr.shared.generated.resources.import_emberr_copying_media
import emberr.shared.generated.resources.import_emberr_could_not_read
import emberr.shared.generated.resources.import_emberr_daily_notes
import emberr.shared.generated.resources.import_emberr_failed
import emberr.shared.generated.resources.import_emberr_finished
import emberr.shared.generated.resources.import_emberr_getting_ready
import emberr.shared.generated.resources.import_emberr_imported_spaces
import emberr.shared.generated.resources.import_emberr_missing_media
import emberr.shared.generated.resources.import_emberr_reading_backup
import emberr.shared.generated.resources.import_emberr_saving
import emberr.shared.generated.resources.import_emberr_spaces_to_import
import emberr.shared.generated.resources.import_emberr_subtitle
import emberr.shared.generated.resources.import_nothing_added
import emberr.shared.generated.resources.import_space_count
import emberr.shared.generated.resources.import_stopped
import emberr.shared.generated.resources.import_stopping
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

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
        subtitle = stringResource(Res.string.import_emberr_subtitle),
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
                        stringResource(Res.string.import_emberr_copying_media, importProgress.copiedCount, importProgress.totalCount)
                    } else {
                        stringResource(Res.string.import_emberr_getting_ready)
                    },
                    onStopClick = onStopClick
                )
                EmberrImportProgress.Saving -> ImportWorkingMessage(stringResource(Res.string.import_emberr_saving))
                EmberrImportProgress.Stopping -> ImportWorkingMessage(stringResource(Res.string.import_stopping))
                EmberrImportProgress.Stopped -> ImportOutcomeMessage(
                    title = stringResource(Res.string.import_stopped),
                    detailLines = listOf(stringResource(Res.string.import_nothing_added)),
                    errorLine = null,
                    onDoneClick = onDismiss
                )
                is EmberrImportProgress.Finished -> ImportFinishedMessage(importProgress.result, onDoneClick = onDismiss)
                is EmberrImportProgress.Failed -> ImportOutcomeMessage(
                    title = stringResource(Res.string.import_emberr_failed),
                    detailLines = listOf(stringResource(Res.string.import_nothing_added)),
                    errorLine = importProgress.problem.text(),
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
                title = stringResource(Res.string.import_emberr_choose_backup_file),
                hint = stringResource(Res.string.import_emberr_choose_backup_hint)
            )
            PickedBackupState.Reading -> ImportReadingMessage(stringResource(Res.string.import_emberr_reading_backup))
            is PickedBackupState.CouldNotRead -> ImportFadedLine(text = stringResource(Res.string.import_emberr_could_not_read, pickedBackupState.reason, importChooseAgainHint()))
            is PickedBackupState.Ready -> ImportPickedSummary(
                pickedLabel = pickedBackupState.pickedName,
                countsLine = pluralStringResource(Res.plurals.import_space_count, spaces.size, spaces.size),
                extraLines = emptyList(),
                changeHint = stringResource(Res.string.import_emberr_choose_different_file)
            )
        }
    }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        EmberrChoiceRow(
            label = stringResource(Res.string.import_emberr_spaces_to_import),
            value = checkedSpacesLabel(spaces, checkedSpaceIds),
            icon = Res.drawable.folder
        ) {
            if (spaces.isEmpty()) {
                ImportMenuOption(label = stringResource(Res.string.import_emberr_choose_backup_first), isSelected = false, isAvailable = false, onClick = {})
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
                label = stringResource(Res.string.import_emberr_daily_notes),
                icon = Res.drawable.daily,
                isOn = wantsDailyNotes,
                onClick = { wantsDailyNotes = !wantsDailyNotes }
            )
        }
        if (readyBackup?.hasCalendar == true) {
            EmberrSwitchRow(
                label = stringResource(Res.string.import_emberr_calendar),
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
        result.addedDailyNotes && result.addedCalendar -> stringResource(Res.string.import_emberr_added_daily_notes_and_calendar)
        result.addedDailyNotes -> stringResource(Res.string.import_emberr_added_daily_notes)
        result.addedCalendar -> stringResource(Res.string.import_emberr_added_calendar)
        else -> null
    }
    ImportOutcomeMessage(
        title = if (result.importedSpaceNames.isEmpty()) {
            stringResource(Res.string.import_emberr_finished)
        } else {
            pluralStringResource(Res.plurals.import_emberr_imported_spaces, result.importedSpaceNames.size, result.importedSpaceNames.size)
        },
        detailLines = listOfNotNull(result.importedSpaceNames.joinToString(" · ").ifEmpty { null }, addedLine),
        errorLine = if (result.missingMediaCount == 0) {
            null
        } else {
            pluralStringResource(Res.plurals.import_emberr_missing_media, result.missingMediaCount, result.missingMediaCount)
        },
        onDoneClick = onDoneClick
    )
}
