package com.emberr.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.backup.manual.BackupContentChoice
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrChoiceRow
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.EmberrSpaceCheckboxOptions
import com.emberr.presentation.shared.components.EmberrSwitchRow
import com.emberr.presentation.shared.components.checkedSpacesLabel
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.calendar
import emberr.shared.generated.resources.common_close
import emberr.shared.generated.resources.daily
import emberr.shared.generated.resources.file_text
import emberr.shared.generated.resources.folder
import emberr.shared.generated.resources.settings_export_backup
import emberr.shared.generated.resources.settings_export_backup_calendar
import emberr.shared.generated.resources.settings_export_backup_daily_notes
import emberr.shared.generated.resources.settings_export_backup_export
import emberr.shared.generated.resources.settings_export_backup_select_spaces
import emberr.shared.generated.resources.settings_export_backup_subtitle
import emberr.shared.generated.resources.settings_export_format
import emberr.shared.generated.resources.settings_export_format_backup
import emberr.shared.generated.resources.settings_export_format_backup_subtitle
import emberr.shared.generated.resources.settings_export_format_vault
import emberr.shared.generated.resources.settings_export_format_vault_subtitle
import org.jetbrains.compose.resources.stringResource

private val ExportSheetButtonPadding = 20.dp

@Composable
fun ExportSpacesSheet(
    spaces: List<SpaceEntity>,
    onExportClick: (choice: BackupContentChoice) -> Unit,
    onExportMarkdownVaultClick: (spaceIds: Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var exportsMarkdownVault by remember { mutableStateOf(false) }
    var uncheckedSpaceIds by remember { mutableStateOf(emptySet<String>()) }
    var includesDailyNotes by remember { mutableStateOf(true) }
    var includesCalendar by remember { mutableStateOf(true) }
    val allSpaceIds = spaces.map { it.spaceId }.toSet()
    val checkedSpaceIds = allSpaceIds - uncheckedSpaceIds

    EmberrBottomSheet(
        expanded = true,
        onDismiss = onDismiss,
        title = stringResource(Res.string.settings_export_backup),
        subtitle = stringResource(Res.string.settings_export_backup_subtitle),
        contentHorizontalPadding = 0.dp
    ) { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            val backupLabel = stringResource(Res.string.settings_export_format_backup)
            val markdownVaultLabel = stringResource(Res.string.settings_export_format_vault)
            EmberrChoiceRow(
                label = stringResource(Res.string.settings_export_format),
                value = if (exportsMarkdownVault) markdownVaultLabel else backupLabel,
                icon = Res.drawable.file_text
            ) { closeMenuAnd ->
                ExportFormatOption(
                    label = backupLabel,
                    subtitle = stringResource(Res.string.settings_export_format_backup_subtitle),
                    isSelected = !exportsMarkdownVault,
                    onClick = { closeMenuAnd { exportsMarkdownVault = false } }
                )
                ExportFormatOption(
                    label = markdownVaultLabel,
                    subtitle = stringResource(Res.string.settings_export_format_vault_subtitle),
                    isSelected = exportsMarkdownVault,
                    onClick = { closeMenuAnd { exportsMarkdownVault = true } }
                )
            }
            EmberrChoiceRow(
                label = stringResource(Res.string.settings_export_backup_select_spaces),
                value = checkedSpacesLabel(spaces, checkedSpaceIds),
                icon = Res.drawable.folder
            ) {
                EmberrSpaceCheckboxOptions(
                    spaces = spaces,
                    checkedSpaceIds = checkedSpaceIds,
                    onCheckedSpaceIdsChange = { newCheckedSpaceIds -> uncheckedSpaceIds = allSpaceIds - newCheckedSpaceIds }
                )
            }
            AnimatedVisibility(visible = !exportsMarkdownVault, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    EmberrSwitchRow(
                        label = stringResource(Res.string.settings_export_backup_daily_notes),
                        icon = Res.drawable.daily,
                        isOn = includesDailyNotes,
                        onClick = { includesDailyNotes = !includesDailyNotes }
                    )
                    EmberrSwitchRow(
                        label = stringResource(Res.string.settings_export_backup_calendar),
                        icon = Res.drawable.calendar,
                        isOn = includesCalendar,
                        onClick = { includesCalendar = !includesCalendar }
                    )
                }
            }
            EmberrButtonPrimary(
                text = stringResource(Res.string.settings_export_backup_export),
                onClick = {
                    closeAnd {
                        if (exportsMarkdownVault) onExportMarkdownVaultClick(checkedSpaceIds)
                        else onExportClick(BackupContentChoice(checkedSpaceIds, includesDailyNotes, includesCalendar))
                    }
                },
                enabled = if (exportsMarkdownVault) checkedSpaceIds.isNotEmpty() else checkedSpaceIds.isNotEmpty() || includesDailyNotes || includesCalendar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = ExportSheetButtonPadding, end = ExportSheetButtonPadding)
            )
            EmberrButtonSecondary(
                text = stringResource(Res.string.common_close),
                onClick = { closeAnd {} },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = ExportSheetButtonPadding, end = ExportSheetButtonPadding)
            )
        }
    }
}

@Composable
private fun ExportFormatOption(label: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    if (isDesktopPlatform) {
        EmberrDesktopMenuOption(label = label, subtitle = subtitle, isSelected = isSelected, onClick = onClick)
    } else {
        EmberrBottomSheetOption(label = label, subtitle = subtitle, isSelected = isSelected, onClick = onClick)
    }
}
