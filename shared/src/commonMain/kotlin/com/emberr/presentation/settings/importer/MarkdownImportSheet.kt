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
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.file_text
import emberr.shared.generated.resources.folder

private val ChooseFolderButtonHeight = 46.dp
private const val EMBERR_NOTES_LABEL = "Emberr Notes"

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
        title = "Text & Markdown",
        subtitle = "Turn .md, .txt and .canvas files, or a .zip with folders, into Emberr notes.",
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
                    onChooseFolderClick = openFolderPicker,
                    onSpaceSelected = onSpaceSelected,
                    onImportClick = onImportClick,
                    onCloseClick = onDismiss
                )
                is ImportProgress.Importing -> ImportProgressMessage(
                    doneFraction = importProgress.doneFraction,
                    progressText = importProgress.copyingFileName?.let { fileName -> "Copying $fileName…" }
                        ?: "Importing ${importProgress.handledNoteCount} of ${countLabel(importProgress.totalNoteCount, "note")}…",
                    onStopClick = onStopClick
                )
                ImportProgress.Stopping -> ImportWorkingMessage("Stopping and removing what was imported…")
                ImportProgress.Stopped -> ImportOutcomeMessage(
                    title = "Import stopped",
                    detailLines = listOf("Nothing was added."),
                    errorLine = null,
                    onDoneClick = onDismiss
                )
                is ImportProgress.Finished -> ImportFinishedMessage(
                    result = importProgress.result,
                    spaceName = spaces.firstOrNull { it.spaceId == importProgress.result.spaceId }?.displayName,
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
    onChooseFolderClick: (() -> Unit)?,
    onSpaceSelected: (String) -> Unit,
    onImportClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    ImportChooseBox(
        isClickable = pickedFilesState != PickedFilesState.Reading,
        onClick = onChooseFilesClick,
        modifier = Modifier.padding(horizontal = ImportSheetContentPadding)
    ) {
        when (pickedFilesState) {
            PickedFilesState.NothingPicked -> ImportChoosePrompt(
                title = "Choose Files or a .zip",
                hint = ".md, .markdown, .txt, .canvas or .zip"
            )
            PickedFilesState.Reading -> ImportReadingMessage("Reading files…")
            PickedFilesState.CouldNotRead -> ImportFadedLine(text = "Couldn't read those files. Tap to choose again.")
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
            label = "Save to Space",
            value = spaces.firstOrNull { it.spaceId == selectedSpaceId }?.displayName.orEmpty(),
            icon = Res.drawable.folder
        ) { closeMenuAnd ->
            spaces.forEach { space ->
                ImportMenuOption(
                    label = space.displayName,
                    isSelected = space.spaceId == selectedSpaceId,
                    onClick = { closeMenuAnd { onSpaceSelected(space.spaceId) } }
                )
            }
        }
        EmberrChoiceRow(label = "Import As", value = EMBERR_NOTES_LABEL, icon = Res.drawable.file_text) { closeMenuAnd ->
            ImportMenuOption(
                label = EMBERR_NOTES_LABEL,
                subtitle = "Each file becomes a note made of blocks.",
                isSelected = true,
                onClick = { closeMenuAnd {} }
            )
            ImportMenuOption(
                label = "Markdown Files",
                subtitle = "Coming soon",
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
    val destination = spaceName?.let { " into $it" }.orEmpty()
    val extrasLine = listOfNotNull(
        countLabel(result.createdFolderCount, "folder").takeIf { result.createdFolderCount > 0 },
        countLabel(result.copiedAttachmentCount, "attachment").takeIf { result.copiedAttachmentCount > 0 }
    ).joinToString(" · ")

    ImportOutcomeMessage(
        title = "Imported ${countLabel(result.importedNoteCount, "note")}$destination",
        detailLines = listOf(extrasLine).filter { it.isNotEmpty() },
        errorLine = result.failedNames.takeIf { it.isNotEmpty() }?.let { failedNames ->
            "Couldn't import ${failedNames.size}: ${failedNames.joinToString(", ")}"
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
            text = "Choose a Folder",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PickedFilesSummary(summary: ImportSummary) {
    ImportPickedSummary(
        pickedLabel = summary.pickedNames.singleOrNull() ?: "${summary.pickedNames.size} files",
        countsLine = listOfNotNull(
            countLabel(summary.noteCount, "note"),
            countLabel(summary.folderCount, "folder").takeIf { summary.folderCount > 0 },
            countLabel(summary.attachmentCount, "attachment").takeIf { summary.attachmentCount > 0 }
        ).joinToString(" · "),
        extraLines = listOfNotNull(
            summary.skippedNames.takeIf { it.isNotEmpty() }?.let { skippedNames ->
                "${skippedNames.size} skipped: ${skippedNames.joinToString(", ")}"
            }
        ),
        changeHint = "Choose different files"
    )
}
