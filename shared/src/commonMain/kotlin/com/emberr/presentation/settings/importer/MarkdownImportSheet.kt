package com.emberr.presentation.settings.importer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.importer.ImportProgress
import com.emberr.domain.importer.ImportResult
import com.emberr.domain.importer.ImportSummary
import com.emberr.presentation.settings.SettingsGroupTitle
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.folder_open
import org.jetbrains.compose.resources.painterResource

private val SheetContentPadding = 20.dp
private val ChooseFilesBoxMinHeight = 132.dp
private val ChooseFilesBoxCornerRadius = 14.dp
private val ChooseFilesBoxShape = RoundedCornerShape(ChooseFilesBoxCornerRadius)
private val ChooseFilesBoxBorderWidth = 1.5.dp
private val ChooseFilesBoxDashLength = 8.dp
private val ChooseFilesBoxDashGap = 6.dp
private const val UNAVAILABLE_OPTION_ALPHA = 0.38f
private const val FADED_TEXT_ALPHA = 0.6f

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
    onDismiss: () -> Unit
) {
    val openFilePicker = rememberImportFilePicker(onFilesPicked)
    val openFolderPicker = rememberImportFolderPicker(onFolderPicked)

    EmberrBottomSheet(
        expanded = true,
        onDismiss = onDismiss,
        title = "Text & Markdown",
        subtitle = "Turn .md and .txt files, or a .zip with folders, into Emberr notes.",
        contentHorizontalPadding = 0.dp
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
                    onImportClick = onImportClick
                )
                is ImportProgress.Importing -> ImportingMessage(importProgress)
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
    onImportClick: () -> Unit
) {
    ChooseFilesBox(
        pickedFilesState = pickedFilesState,
        onClick = onChooseFilesClick,
        modifier = Modifier.padding(horizontal = SheetContentPadding)
    )
    if (onChooseFolderClick != null) {
        EmberrButtonSecondary(
            text = "Choose a Folder",
            onClick = onChooseFolderClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = SheetContentPadding, end = SheetContentPadding)
        )
    }

    SheetSectionTitle(title = "Save to Space")
    spaces.forEach { space ->
        EmberrBottomSheetOption(
            label = space.displayName,
            isSelected = space.spaceId == selectedSpaceId,
            onClick = { onSpaceSelected(space.spaceId) }
        )
    }

    SheetSectionTitle(title = "Import As")
    EmberrBottomSheetOption(
        label = "Emberr Notes",
        subtitle = "Each file becomes a note made of blocks.",
        isSelected = true,
        onClick = {}
    )
    EmberrBottomSheetOption(
        label = "Markdown Files",
        subtitle = "Coming soon",
        modifier = Modifier.alpha(UNAVAILABLE_OPTION_ALPHA),
        onClick = {}
    )

    EmberrButtonPrimary(
        text = "Import",
        onClick = onImportClick,
        enabled = pickedFilesState is PickedFilesState.Ready && pickedFilesState.summary.noteCount > 0,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = SheetContentPadding, end = SheetContentPadding)
    )
}

@Composable
private fun ImportingMessage(importing: ImportProgress.Importing) {
    val progressFraction = if (importing.totalNoteCount == 0) 0f else importing.handledNoteCount.toFloat() / importing.totalNoteCount

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = SheetContentPadding, vertical = 8.dp)
    ) {
        LinearProgressIndicator(progress = { progressFraction }, modifier = Modifier.fillMaxWidth())
        Text(
            text = "Importing ${importing.handledNoteCount} of ${countLabel(importing.totalNoteCount, "note")}…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        FadedLine(text = "You can close this. The import keeps going in the background.")
    }
}

@Composable
private fun ImportFinishedMessage(result: ImportResult, spaceName: String?, onDoneClick: () -> Unit) {
    val destination = spaceName?.let { " into $it" }.orEmpty()
    val extrasLine = listOfNotNull(
        countLabel(result.createdFolderCount, "folder").takeIf { result.createdFolderCount > 0 },
        countLabel(result.copiedAttachmentCount, "attachment").takeIf { result.copiedAttachmentCount > 0 }
    ).joinToString(" · ")

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = SheetContentPadding, vertical = 8.dp)
    ) {
        Text(
            text = "Imported ${countLabel(result.importedNoteCount, "note")}$destination",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (extrasLine.isNotEmpty()) {
            Text(text = extrasLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        if (result.failedNames.isNotEmpty()) {
            Text(
                text = "Couldn't import ${result.failedNames.size}: ${result.failedNames.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
        EmberrButtonPrimary(
            text = "Done",
            onClick = onDoneClick,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

@Composable
private fun ChooseFilesBox(
    pickedFilesState: PickedFilesState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ChooseFilesBoxMinHeight)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(ChooseFilesBoxCornerRadius.toPx()),
                    style = Stroke(
                        width = ChooseFilesBoxBorderWidth.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(ChooseFilesBoxDashLength.toPx(), ChooseFilesBoxDashGap.toPx())
                        )
                    )
                )
            }
            .clip(ChooseFilesBoxShape)
            .clickable(enabled = pickedFilesState != PickedFilesState.Reading, onClick = onClick)
            .padding(16.dp)
    ) {
        when (pickedFilesState) {
            PickedFilesState.NothingPicked -> ChooseFilesPrompt()
            PickedFilesState.Reading -> ReadingFilesMessage()
            is PickedFilesState.Ready -> PickedFilesSummary(pickedFilesState.summary)
        }
    }
}

@Composable
private fun ChooseFilesPrompt() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            painter = painterResource(Res.drawable.folder_open),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = FADED_TEXT_ALPHA),
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "Choose Files or a .zip",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        FadedLine(text = ".md, .markdown, .txt or .zip")
    }
}

@Composable
private fun ReadingFilesMessage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        FadedLine(text = "Reading files…")
    }
}

@Composable
private fun PickedFilesSummary(summary: ImportSummary) {
    val pickedLabel = summary.pickedNames.singleOrNull() ?: "${summary.pickedNames.size} files"
    val countsLine = listOfNotNull(
        countLabel(summary.noteCount, "note"),
        countLabel(summary.folderCount, "folder").takeIf { summary.folderCount > 0 },
        countLabel(summary.attachmentCount, "attachment").takeIf { summary.attachmentCount > 0 }
    ).joinToString(" · ")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = pickedLabel,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = countsLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (summary.skippedNames.isNotEmpty()) {
            FadedLine(
                text = "${summary.skippedNames.size} skipped: ${summary.skippedNames.joinToString(", ")}",
                maxLines = 2
            )
        }
        FadedLine(text = "Choose different files")
    }
}

@Composable
private fun FadedLine(text: String, maxLines: Int = Int.MAX_VALUE) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = FADED_TEXT_ALPHA),
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SheetSectionTitle(title: String) {
    Box(modifier = Modifier.padding(start = 26.dp, top = 22.dp)) {
        SettingsGroupTitle(title = title)
    }
}

private fun countLabel(count: Int, singularWord: String): String =
    if (count == 1) "1 $singularWord" else "$count ${singularWord}s"
