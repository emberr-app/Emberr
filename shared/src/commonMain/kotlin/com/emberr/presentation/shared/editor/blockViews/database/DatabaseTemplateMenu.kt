package com.emberr.presentation.shared.editor.blockViews.database

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseTemplateRepeat
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrSearchSheetBody
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.common_delete
import emberr.shared.generated.resources.common_off
import emberr.shared.generated.resources.common_untitled
import emberr.shared.generated.resources.database_edit
import emberr.shared.generated.resources.database_named_template_options
import emberr.shared.generated.resources.database_new_template
import emberr.shared.generated.resources.database_no_templates_match
import emberr.shared.generated.resources.database_search_templates
import emberr.shared.generated.resources.database_search_templates_with_dots
import emberr.shared.generated.resources.database_template_default
import emberr.shared.generated.resources.database_template_default_for_new_rows
import emberr.shared.generated.resources.database_template_empty
import emberr.shared.generated.resources.database_template_options
import emberr.shared.generated.resources.database_template_repeat
import emberr.shared.generated.resources.database_template_set_as_default
import emberr.shared.generated.resources.database_templates
import emberr.shared.generated.resources.doc_text
import emberr.shared.generated.resources.ellipsis
import emberr.shared.generated.resources.file_text
import emberr.shared.generated.resources.pen
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.refresh_cw
import emberr.shared.generated.resources.star
import emberr.shared.generated.resources.star_outline
import emberr.shared.generated.resources.trash
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DatabaseTemplateMenu(
    expanded: Boolean,
    block: DatabaseBlock,
    editor: DatabaseBlockEditor,
    onOpenTemplate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val templates by remember(editor) { editor.databaseTemplates }.collectAsState(initial = emptyList())
    val defaultTemplateId = block.defaultTemplateId?.takeIf { templateId -> templates.any { it.noteId == templateId } }
    var searchQuery by remember(expanded) { mutableStateOf("") }
    val untitledText = stringResource(Res.string.common_untitled)
    val matchingTemplates = templates.filter { templateLabel(it, untitledText).contains(searchQuery.trim(), ignoreCase = true) }

    DatabaseMenu(expanded = expanded, title = stringResource(Res.string.database_templates), onDismiss = onDismiss, hasSearchBar = true) { closeAnd ->
        val templateChoices = @Composable {
            if (searchQuery.isBlank()) {
                DatabaseTemplateOption(
                    label = stringResource(Res.string.database_template_empty),
                    icon = Res.drawable.doc_text,
                    isDefault = defaultTemplateId == null,
                    onUse = { closeAnd { editor.addRowFromTemplate(block.id, null) } },
                    onSetDefault = { editor.setDefaultTemplate(block.id, null) },
                    repeat = null,
                    onRepeatChange = null,
                    onEdit = null,
                    onDelete = null
                )
            } else if (matchingTemplates.isEmpty()) {
                DatabaseMenuMessage(text = stringResource(Res.string.database_no_templates_match, searchQuery.trim()), color = MaterialTheme.colorScheme.outline)
            }

            matchingTemplates.forEach { template ->
                key(template.noteId) {
                    DatabaseTemplateOption(
                        label = templateLabel(template, untitledText),
                        icon = Res.drawable.file_text,
                        isDefault = template.noteId == defaultTemplateId,
                        onUse = { closeAnd { editor.addRowFromTemplate(block.id, template.noteId) } },
                        onSetDefault = { editor.setDefaultTemplate(block.id, template.noteId) },
                        repeat = block.repeatingTemplates[template.noteId],
                        onRepeatChange = { repeat -> editor.setTemplateRepeat(block.id, template.noteId, repeat) },
                        onEdit = { closeAnd { onOpenTemplate(template.noteId) } },
                        onDelete = { editor.deleteTemplate(template.noteId) }
                    )
                }
            }

            DatabaseMenuOption(
                label = stringResource(Res.string.database_new_template),
                icon = { DatabaseOptionIcon(Res.drawable.plus) },
                onClick = { closeAnd { editor.createTemplate(block.id, onOpenTemplate) } }
            )
        }

        if (isDesktopPlatform) {
            DatabaseMenuContent {
                EmberrTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = stringResource(Res.string.database_search_templates_with_dots),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                )
            }
            templateChoices()
        } else {
            EmberrSearchSheetBody(
                searchText = searchQuery,
                onSearchTextChange = { searchQuery = it },
                placeholder = stringResource(Res.string.database_search_templates)
            ) {
                templateChoices()
            }
        }
    }
}

private fun templateLabel(template: NoteMetadataEntity, untitledText: String): String = template.title.ifBlank { untitledText }

@Composable
private fun DatabaseTemplateOption(
    label: String,
    icon: DrawableResource,
    isDefault: Boolean,
    onUse: () -> Unit,
    onSetDefault: () -> Unit,
    repeat: DatabaseTemplateRepeat?,
    onRepeatChange: ((DatabaseTemplateRepeat?) -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    DatabaseMenuLayer(
        title = label,
        anchor = { openLayer ->
            DatabaseMenuOption(
                label = label,
                icon = { DatabaseOptionIcon(icon) },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (repeat != null) {
                            Icon(
                                painter = painterResource(Res.drawable.refresh_cw),
                                contentDescription = repeat.summary(LocalUsesTwentyFourHourClock.current),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 10.dp).size(14.dp)
                            )
                        }
                        if (isDefault) {
                            Text(
                                text = stringResource(Res.string.database_template_default),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                        }
                        IconTooltip(text = stringResource(Res.string.database_template_options)) {
                            Icon(
                                painter = painterResource(Res.drawable.ellipsis),
                                contentDescription = stringResource(Res.string.database_named_template_options, label),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(onClick = openLayer)
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                },
                onClick = onUse
            )
        },
        anchorIsMenuRow = false
    ) { closeLayerAnd ->
        DatabaseMenuOption(
            label = stringResource(if (isDefault) Res.string.database_template_default_for_new_rows else Res.string.database_template_set_as_default),
            icon = { DatabaseOptionIcon(if (isDefault) Res.drawable.star else Res.drawable.star_outline) },
            onClick = onSetDefault
        )
        if (onRepeatChange != null) {
            DatabaseMenuLayer(
                title = stringResource(Res.string.database_template_repeat),
                anchor = { openRepeatLayer ->
                    DatabaseMenuOption(
                        label = stringResource(Res.string.database_template_repeat),
                        icon = { DatabaseOptionIcon(Res.drawable.refresh_cw) },
                        trailing = {
                            Text(
                                text = repeat?.summary(LocalUsesTwentyFourHourClock.current) ?: stringResource(Res.string.common_off),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1
                            )
                        },
                        onClick = openRepeatLayer
                    )
                }
            ) { _ ->
                DatabaseTemplateRepeatChoices(repeat = repeat, onRepeatChange = onRepeatChange)
            }
        }
        if (onEdit != null) {
            DatabaseMenuOption(
                label = stringResource(Res.string.database_edit),
                icon = { DatabaseOptionIcon(Res.drawable.pen) },
                onClick = { closeLayerAnd(onEdit) }
            )
        }
        if (onDelete != null) {
            DatabaseMenuOption(
                label = stringResource(Res.string.common_delete),
                icon = { DatabaseOptionIcon(Res.drawable.trash, tint = MaterialTheme.colorScheme.error) },
                labelColor = MaterialTheme.colorScheme.error,
                onClick = { closeLayerAnd(onDelete) }
            )
        }
    }
}
