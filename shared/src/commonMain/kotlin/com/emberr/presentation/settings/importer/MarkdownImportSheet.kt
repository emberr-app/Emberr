package com.emberr.presentation.settings.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.importer.ImportProgress
import com.emberr.domain.importer.ImportResult
import com.emberr.domain.importer.ImportSummary
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_down
import emberr.shared.generated.resources.file_text
import emberr.shared.generated.resources.folder
import emberr.shared.generated.resources.upload2
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private val SheetContentPadding = 20.dp
private val ChooseFilesBoxMinHeight = 132.dp
private val ChooseFilesBoxCornerRadius = 14.dp
private val ChooseFilesBoxShape = RoundedCornerShape(ChooseFilesBoxCornerRadius)
private val ChooseFilesBoxBorderWidth = 1.5.dp
private val ChooseFilesBoxDashLength = 8.dp
private val ChooseFilesBoxDashGap = 6.dp
private const val CHOOSE_FILES_BOX_BORDER_ALPHA = 0.25f
private val ChooseFolderButtonHeight = 46.dp
private const val UNAVAILABLE_OPTION_ALPHA = 0.38f
private const val FADED_TEXT_ALPHA = 0.6f
private const val PROGRESS_TRACK_ALPHA = 0.12f
private const val EMBERR_NOTES_LABEL = "Emberr Notes"
private val ImportMenuWidth = 280.dp
private val ImportMenuVerticalPadding = 6.dp
private val ImportMenuGap = 6.dp
private val ImportRowIconSize = 18.dp
private val ValueButtonShape = RoundedCornerShape(8.dp)
private val ValueButtonIconSize = 14.dp
private val ValueButtonMaxTextWidth = 160.dp
private const val VALUE_BUTTON_BACKGROUND_ALPHA = 0.08f
private const val VALUE_BUTTON_CONTENT_ALPHA = 0.7f

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
                is ImportProgress.Importing -> ImportingMessage(importProgress, onStopClick = onStopClick)
                ImportProgress.Stopping -> StoppingMessage()
                ImportProgress.Stopped -> ImportStoppedMessage(onDoneClick = onDismiss)
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
    ChooseFilesBox(
        pickedFilesState = pickedFilesState,
        onClick = onChooseFilesClick,
        modifier = Modifier.padding(horizontal = SheetContentPadding)
    )
    if (onChooseFolderClick != null) {
        ChooseFolderButton(
            onClick = onChooseFolderClick,
            modifier = Modifier.padding(top = 12.dp, start = SheetContentPadding, end = SheetContentPadding)
        )
    }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        ImportChoiceRow(
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
        ImportChoiceRow(label = "Import As", value = EMBERR_NOTES_LABEL, icon = Res.drawable.file_text) { closeMenuAnd ->
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

    EmberrButtonPrimary(
        text = "Import",
        onClick = onImportClick,
        enabled = pickedFilesState is PickedFilesState.Ready && pickedFilesState.summary.noteCount > 0,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = SheetContentPadding, end = SheetContentPadding)
    )
    EmberrButtonSecondary(
        text = "Close",
        onClick = onCloseClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = SheetContentPadding, end = SheetContentPadding)
    )
}

@Composable
private fun ImportingMessage(importing: ImportProgress.Importing, onStopClick: () -> Unit) {
    val progressFraction = importing.doneFraction
    val progressText = importing.copyingFileName?.let { fileName -> "Copying $fileName…" }
        ?: "Importing ${importing.handledNoteCount} of ${countLabel(importing.totalNoteCount, "note")}…"

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = SheetContentPadding, vertical = 8.dp)
    ) {
        LinearProgressIndicator(
            progress = { progressFraction },
            color = MaterialTheme.colorScheme.onSurface,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = PROGRESS_TRACK_ALPHA),
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = progressText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(progressFraction * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        EmberrButtonSecondary(
            text = "Stop import",
            onClick = onStopClick,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

@Composable
private fun StoppingMessage() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = SheetContentPadding, vertical = 8.dp)
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        Text(
            text = "Stopping and removing what was imported…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ImportStoppedMessage(onDoneClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = SheetContentPadding, vertical = 8.dp)
    ) {
        Text(
            text = "Import stopped",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(text = "Nothing was added.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        EmberrButtonPrimary(
            text = "Done",
            onClick = onDoneClick,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
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
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = CHOOSE_FILES_BOX_BORDER_ALPHA)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ChooseFilesBoxMinHeight)
            .dashedChooseBorder(borderColor)
            .clip(ChooseFilesBoxShape)
            .clickable(enabled = pickedFilesState != PickedFilesState.Reading, onClick = onClick)
            .padding(16.dp)
    ) {
        when (pickedFilesState) {
            PickedFilesState.NothingPicked -> ChooseFilesPrompt()
            PickedFilesState.Reading -> ReadingFilesMessage()
            PickedFilesState.CouldNotRead -> FadedLine(text = "Couldn't read those files. Tap to choose again.")
            is PickedFilesState.Ready -> PickedFilesSummary(pickedFilesState.summary)
        }
    }
}

@Composable
private fun ChooseFolderButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = CHOOSE_FILES_BOX_BORDER_ALPHA)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(ChooseFolderButtonHeight)
            .dashedChooseBorder(borderColor)
            .clip(ChooseFilesBoxShape)
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

private fun Modifier.dashedChooseBorder(color: Color): Modifier =
    drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(ChooseFilesBoxCornerRadius.toPx()),
            style = Stroke(
                width = ChooseFilesBoxBorderWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(ChooseFilesBoxDashLength.toPx(), ChooseFilesBoxDashGap.toPx())
                )
            )
        )
    }

@Composable
private fun ChooseFilesPrompt() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            painter = painterResource(Res.drawable.upload2),
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
        FadedLine(text = ".md, .markdown, .txt, .canvas or .zip")
    }
}

@Composable
private fun ReadingFilesMessage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
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
private fun ImportChoiceRow(
    label: String,
    value: String,
    icon: DrawableResource,
    menuContent: @Composable (closeMenuAnd: (() -> Unit) -> Unit) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    EmberrBottomSheetOption(
        label = label,
        icon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(ImportRowIconSize)
            )
        },
        trailing = {
            Box {
                ImportValueButton(value = value, isMenuOpen = isMenuOpen, onClick = { isMenuOpen = true })
                if (isDesktopPlatform) {
                    EmberrDesktopMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false },
                        offset = DpOffset(x = 0.dp, y = ImportMenuGap),
                        modifier = Modifier.width(ImportMenuWidth)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = ImportMenuVerticalPadding)) {
                            menuContent { action ->
                                action()
                                isMenuOpen = false
                            }
                        }
                    }
                }
            }
        },
        onClick = { isMenuOpen = true }
    )

    if (!isDesktopPlatform) {
        EmberrBottomSheet(
            expanded = isMenuOpen,
            onDismiss = { isMenuOpen = false },
            title = label,
            contentHorizontalPadding = 0.dp
        ) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                menuContent(closeAnd)
                EmberrButtonPrimary(
                    text = "Close",
                    onClick = { closeAnd {} },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = SheetContentPadding, end = SheetContentPadding)
                )
            }
        }
    }
}

@Composable
private fun ImportValueButton(value: String, isMenuOpen: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val showsBackground = isHovered || isMenuOpen || !isDesktopPlatform
    val contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = VALUE_BUTTON_CONTENT_ALPHA)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(ValueButtonShape)
            .background(
                if (showsBackground) MaterialTheme.colorScheme.onSurface.copy(alpha = VALUE_BUTTON_BACKGROUND_ALPHA) else Color.Transparent
            )
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = ValueButtonMaxTextWidth)
        )
        Icon(
            painter = painterResource(Res.drawable.chevron_down),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.padding(start = 4.dp).size(ValueButtonIconSize)
        )
    }
}

@Composable
private fun ImportMenuOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
    isAvailable: Boolean = true
) {
    val availabilityModifier = if (isAvailable) Modifier else Modifier.alpha(UNAVAILABLE_OPTION_ALPHA)
    if (isDesktopPlatform) {
        EmberrDesktopMenuOption(
            label = label,
            subtitle = subtitle,
            isSelected = isSelected,
            onClick = onClick,
            modifier = availabilityModifier
        )
    } else {
        EmberrBottomSheetOption(
            label = label,
            subtitle = subtitle,
            isSelected = isSelected,
            onClick = onClick,
            modifier = availabilityModifier
        )
    }
}

private fun countLabel(count: Int, singularWord: String): String =
    if (count == 1) "1 $singularWord" else "$count ${singularWord}s"
