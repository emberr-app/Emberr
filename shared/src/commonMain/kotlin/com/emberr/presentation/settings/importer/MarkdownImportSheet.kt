package com.emberr.presentation.settings.importer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.importer.ImportProgress
import com.emberr.domain.importer.ImportResult
import com.emberr.domain.importer.ImportSummary
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrChoiceRow
import com.emberr.presentation.shared.components.shownSpaceName
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.file_text
import emberr.shared.generated.resources.folder
import emberr.shared.generated.resources.import_attachment_count
import emberr.shared.generated.resources.import_folder_count
import emberr.shared.generated.resources.import_markdown_choose_different_files
import emberr.shared.generated.resources.import_markdown_choose_files
import emberr.shared.generated.resources.import_markdown_choose_files_hint
import emberr.shared.generated.resources.import_markdown_choose_folder
import emberr.shared.generated.resources.import_markdown_coming_soon
import emberr.shared.generated.resources.import_markdown_copying_file
import emberr.shared.generated.resources.import_markdown_could_not_read
import emberr.shared.generated.resources.import_markdown_emberr_notes
import emberr.shared.generated.resources.import_markdown_emberr_notes_subtitle
import emberr.shared.generated.resources.import_markdown_failed_files
import emberr.shared.generated.resources.import_markdown_import_as
import emberr.shared.generated.resources.import_markdown_imported_notes
import emberr.shared.generated.resources.import_markdown_imported_notes_into_space
import emberr.shared.generated.resources.import_markdown_importing_notes
import emberr.shared.generated.resources.import_markdown_markdown_files
import emberr.shared.generated.resources.import_markdown_picked_files
import emberr.shared.generated.resources.import_markdown_reading_files
import emberr.shared.generated.resources.import_markdown_save_to_space
import emberr.shared.generated.resources.import_markdown_skipped_files
import emberr.shared.generated.resources.import_markdown_subtitle
import emberr.shared.generated.resources.import_note_count
import emberr.shared.generated.resources.import_nothing_added
import emberr.shared.generated.resources.import_stopped
import emberr.shared.generated.resources.import_stopping
import emberr.shared.generated.resources.import_text_and_markdown
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val ChooseFolderButtonHeight = 46.dp

@Composable
fun MarkdownImportSheet(
    spaces: List<SpaceEntity>,
    selectedSpaceId: String,
    pickedFilesState: PickedFilesState,
    importProgress: ImportProgress,
    onFilesPicked: (List<String>) -> Unit,
    onFolderPicked: (String) -> Unit,
    onSpaceSelected: (String) -> Unit,
    onImportClick: () -> Unit,
    onStopClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val openFilePicker = rememberImportFilePicker(onFilesPicked)
    val openFolderPicker = rememberImportFolderPicker(onFolderPicked)

    EmberrBottomSheet(
        expanded = true,
        onDismiss = onDismiss,
        title = stringResource(Res.string.import_text_and_markdown),
        subtitle = stringResource(Res.string.import_markdown_subtitle),
        contentHorizontalPadding = 0.dp,
        isDismissible = importProgress !is ImportProgress.Importing && importProgress != ImportProgress.Stopping
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            when (importProgress) {
                ImportProgress.NotStarted -> ImportChoices(
                    spaces = spaces,
                    selectedSpaceId = selectedSpaceId,
                    pickedFilesState = pickedFilesState,
                    onChooseFilesClick = openFilePicker,
                    onFilesDropped = onFilesPicked,
                    onChooseFolderClick = openFolderPicker,
                    onSpaceSelected = onSpaceSelected,
                    onImportClick = onImportClick,
                    onCloseClick = onDismiss
                )
                is ImportProgress.Importing -> ImportProgressMessage(
                    doneFraction = importProgress.doneFraction,
                    progressText = importProgress.copyingFileName?.let { fileName ->
                        stringResource(Res.string.import_markdown_copying_file, fileName)
                    } ?: pluralStringResource(
                        Res.plurals.import_markdown_importing_notes,
                        importProgress.totalNoteCount,
                        importProgress.handledNoteCount,
                        importProgress.totalNoteCount
                    ),
                    onStopClick = onStopClick
                )
                ImportProgress.Stopping -> ImportWorkingMessage(stringResource(Res.string.import_stopping))
                ImportProgress.Stopped -> ImportOutcomeMessage(
                    title = stringResource(Res.string.import_stopped),
                    detailLines = listOf(stringResource(Res.string.import_nothing_added)),
                    errorLine = null,
                    onDoneClick = onDismiss
                )
                is ImportProgress.Finished -> ImportFinishedMessage(
                    result = importProgress.result,
                    spaceName = spaces.firstOrNull { it.spaceId == importProgress.result.spaceId }?.let { shownSpaceName(it.displayName) },
                    onDoneClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ImportChoices(
    spaces: List<SpaceEntity>,
    selectedSpaceId: String,
    pickedFilesState: PickedFilesState,
    onChooseFilesClick: () -> Unit,
    onFilesDropped: (List<String>) -> Unit,
    onChooseFolderClick: (() -> Unit)?,
    onSpaceSelected: (String) -> Unit,
    onImportClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    ImportChooseBox(
        isClickable = pickedFilesState != PickedFilesState.Reading,
        onClick = onChooseFilesClick,
        onFilesDropped = onFilesDropped,
        modifier = Modifier.padding(horizontal = ImportSheetContentPadding)
    ) {
        when (pickedFilesState) {
            PickedFilesState.NothingPicked -> ImportChoosePrompt(
                title = stringResource(Res.string.import_markdown_choose_files),
                hint = stringResource(Res.string.import_markdown_choose_files_hint)
            )
            PickedFilesState.Reading -> ImportReadingMessage(stringResource(Res.string.import_markdown_reading_files))
            PickedFilesState.CouldNotRead -> ImportFadedLine(text = stringResource(Res.string.import_markdown_could_not_read, importChooseAgainHint()))
            is PickedFilesState.Ready -> PickedFilesSummary(pickedFilesState.summary)
        }
    }
    if (onChooseFolderClick != null) {
        ChooseFolderButton(
            onClick = onChooseFolderClick,
            modifier = Modifier.padding(top = 12.dp, start = ImportSheetContentPadding, end = ImportSheetContentPadding)
        )
    }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        EmberrChoiceRow(
            label = stringResource(Res.string.import_markdown_save_to_space),
            value = shownSpaceName(spaces.firstOrNull { it.spaceId == selectedSpaceId }?.displayName.orEmpty()),
            icon = Res.drawable.folder
        ) { closeMenuAnd ->
            spaces.forEach { space ->
                ImportMenuOption(
                    label = shownSpaceName(space.displayName),
                    isSelected = space.spaceId == selectedSpaceId,
                    onClick = { closeMenuAnd { onSpaceSelected(space.spaceId) } }
                )
            }
        }
        val emberrNotesLabel = stringResource(Res.string.import_markdown_emberr_notes)
        EmberrChoiceRow(
            label = stringResource(Res.string.import_markdown_import_as),
            value = emberrNotesLabel,
            icon = Res.drawable.file_text
        ) { closeMenuAnd ->
            ImportMenuOption(
                label = emberrNotesLabel,
                subtitle = stringResource(Res.string.import_markdown_emberr_notes_subtitle),
                isSelected = true,
                onClick = { closeMenuAnd {} }
            )
            ImportMenuOption(
                label = stringResource(Res.string.import_markdown_markdown_files),
                subtitle = stringResource(Res.string.import_markdown_coming_soon),
                isSelected = false,
                isAvailable = false,
                onClick = {}
            )
        }
    }

    ImportStartButtons(
        isImportEnabled = pickedFilesState is PickedFilesState.Ready && pickedFilesState.summary.noteCount > 0,
        onImportClick = onImportClick,
        onCloseClick = onCloseClick
    )
}

@Composable
private fun ImportFinishedMessage(result: ImportResult, spaceName: String?, onDoneClick: () -> Unit) {
    val extrasLine = listOfNotNull(
        pluralStringResource(Res.plurals.import_folder_count, result.createdFolderCount, result.createdFolderCount)
            .takeIf { result.createdFolderCount > 0 },
        pluralStringResource(Res.plurals.import_attachment_count, result.copiedAttachmentCount, result.copiedAttachmentCount)
            .takeIf { result.copiedAttachmentCount > 0 }
    ).joinToString(" · ")

    ImportOutcomeMessage(
        title = if (spaceName == null) {
            pluralStringResource(Res.plurals.import_markdown_imported_notes, result.importedNoteCount, result.importedNoteCount)
        } else {
            pluralStringResource(
                Res.plurals.import_markdown_imported_notes_into_space,
                result.importedNoteCount,
                result.importedNoteCount,
                spaceName
            )
        },
        detailLines = listOf(extrasLine).filter { it.isNotEmpty() },
        errorLine = result.failedNames.takeIf { it.isNotEmpty() }?.let { failedNames ->
            stringResource(Res.string.import_markdown_failed_files, failedNames.size, failedNames.joinToString(", "))
        },
        onDoneClick = onDoneClick
    )
}

@Composable
private fun ChooseFolderButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(ChooseFolderButtonHeight)
            .importDashedBorder(importChooseBorderColor())
            .clip(ImportChooseBoxShape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = stringResource(Res.string.import_markdown_choose_folder),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PickedFilesSummary(summary: ImportSummary) {
    ImportPickedSummary(
        pickedLabel = summary.pickedNames.singleOrNull()
            ?: pluralStringResource(Res.plurals.import_markdown_picked_files, summary.pickedNames.size, summary.pickedNames.size),
        countsLine = listOfNotNull(
            pluralStringResource(Res.plurals.import_note_count, summary.noteCount, summary.noteCount),
            pluralStringResource(Res.plurals.import_folder_count, summary.folderCount, summary.folderCount)
                .takeIf { summary.folderCount > 0 },
            pluralStringResource(Res.plurals.import_attachment_count, summary.attachmentCount, summary.attachmentCount)
                .takeIf { summary.attachmentCount > 0 }
        ).joinToString(" · "),
        extraLines = listOfNotNull(
            summary.skippedNames.takeIf { it.isNotEmpty() }?.let { skippedNames ->
                stringResource(Res.string.import_markdown_skipped_files, skippedNames.size, skippedNames.joinToString(", "))
            }
        ),
        changeHint = stringResource(Res.string.import_markdown_choose_different_files)
    )
}
