package com.emberr.presentation.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.model.PendingShare
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrChoiceRow
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.NotePickerDialog
import com.emberr.presentation.shared.components.orHighContrastLine
import com.emberr.presentation.shared.components.shownSpaceName
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.daily
import emberr.shared.generated.resources.download3
import emberr.shared.generated.resources.floppy
import emberr.shared.generated.resources.folder
import org.jetbrains.compose.resources.painterResource
import emberr.shared.generated.resources.common_close
import emberr.shared.generated.resources.share_files_selected
import emberr.shared.generated.resources.share_images_selected
import emberr.shared.generated.resources.share_save_shared_content
import emberr.shared.generated.resources.share_save_to_a_note
import emberr.shared.generated.resources.share_save_to_overview
import emberr.shared.generated.resources.share_save_to_todays_daily
import emberr.shared.generated.resources.share_shared_image
import emberr.shared.generated.resources.share_space
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val ShareSheetSidePadding = 20.dp

@Composable
fun ShareReceiverSheet(
    share: PendingShare?,
    linkableNotes: List<NoteMetadataEntity>,
    spaces: List<SpaceEntity>,
    activeSpaceId: String,
    isDailyNotesEnabled: Boolean,
    onSpaceSelected: (String) -> Unit,
    onSaveToTodaysDailyNote: () -> Unit,
    onSaveToInbox: () -> Unit,
    onNoteSelected: (String) -> Unit,
    onCreateNote: (String) -> Unit,
    onCreateBlankNote: () -> Unit,
    onDismiss: () -> Unit
) {
    var lastShare by remember { mutableStateOf<PendingShare?>(null) }
    var showNotePicker by remember { mutableStateOf(false) }

    LaunchedEffect(share) {
        if (share != null) lastShare = share else showNotePicker = false
    }

    EmberrBottomSheet(
        expanded = share != null,
        onDismiss = onDismiss,
        title = stringResource(Res.string.share_save_shared_content),
        contentHorizontalPadding = 0.dp
    ) { _ ->
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            lastShare?.let { SharePreviewRow(it) }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = ShareSheetSidePadding),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f).orHighContrastLine()
            )

            if (isDailyNotesEnabled) {
                ShareTargetRow(
                    icon = painterResource(Res.drawable.daily),
                    label = stringResource(Res.string.share_save_to_todays_daily),
                    onClick = onSaveToTodaysDailyNote
                )
            }
            if (spaces.size > 1) {
                EmberrChoiceRow(
                    label = stringResource(Res.string.share_space),
                    value = shownSpaceName(spaces.firstOrNull { it.spaceId == activeSpaceId }?.displayName.orEmpty()),
                    icon = Res.drawable.folder,
                    outerHorizontalPadding = 0.dp,
                    innerHorizontalPadding = ShareSheetSidePadding
                ) { closeMenuAnd ->
                    spaces.forEach { space ->
                        SpaceMenuOption(
                            label = shownSpaceName(space.displayName),
                            isSelected = space.spaceId == activeSpaceId,
                            onClick = { closeMenuAnd { onSpaceSelected(space.spaceId) } }
                        )
                    }
                }
            }
            ShareTargetRow(
                icon = painterResource(Res.drawable.download3),
                label = stringResource(Res.string.share_save_to_overview),
                onClick = onSaveToInbox
            )
            ShareTargetRow(
                icon = painterResource(Res.drawable.floppy),
                label = stringResource(Res.string.share_save_to_a_note),
                onClick = { showNotePicker = true }
            )

            Spacer(Modifier.height(8.dp))

            EmberrButtonPrimary(
                text = stringResource(Res.string.common_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = ShareSheetSidePadding)
            )
        }
    }

    NotePickerDialog(
        expanded = showNotePicker,
        onDismiss = { showNotePicker = false },
        allLinkableNotes = linkableNotes,
        onNoteSelected = onNoteSelected,
        onCreateNote = onCreateNote,
        onCreateBlankNote = onCreateBlankNote
    )
}

@Composable
private fun SharePreviewRow(share: PendingShare) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = ShareSheetSidePadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (share) {
            is PendingShare.Link -> {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = share.url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            is PendingShare.Image -> {
                coil3.compose.AsyncImage(
                    model = share.uriString,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.share_shared_image),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            is PendingShare.Document -> {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = share.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            is PendingShare.Multiple -> {
                val allImages = share.items.all { it is PendingShare.Image }
                Icon(
                    if (allImages) Icons.Default.Image else Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = pluralStringResource(
                        if (allImages) Res.plurals.share_images_selected else Res.plurals.share_files_selected,
                        share.items.size,
                        share.items.size
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ShareTargetRow(
    icon: Painter,
    label: String,
    onClick: () -> Unit
) {
    EmberrBottomSheetOption(
        label = label,
        icon = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        },
        outerHorizontalPadding = 0.dp,
        innerHorizontalPadding = ShareSheetSidePadding,
        onClick = onClick
    )
}

@Composable
private fun SpaceMenuOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    if (isDesktopPlatform) {
        EmberrDesktopMenuOption(label = label, isSelected = isSelected, onClick = onClick)
    } else {
        EmberrBottomSheetOption(label = label, isSelected = isSelected, onClick = onClick)
    }
}
