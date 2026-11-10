package com.emberr.presentation.shared.editor.blockViews.database

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseColorRule
import com.emberr.domain.model.DatabaseColorRuleTarget
import com.emberr.domain.model.DatabaseColumnTarget
import com.emberr.domain.model.DatabaseFilter
import com.emberr.domain.model.DatabaseFilterCondition
import com.emberr.domain.model.PropertyValueType
import com.emberr.domain.model.valueTypeOf
import com.emberr.presentation.shared.components.displayName
import com.emberr.presentation.shared.components.rememberPeopleToPick
import com.emberr.presentation.shared.editor.blockViews.property.formatPropertyDate
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.palette
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.database_add_rule
import emberr.shared.generated.resources.database_color
import emberr.shared.generated.resources.database_color_rule
import emberr.shared.generated.resources.database_color_rule_summary
import emberr.shared.generated.resources.database_color_rule_summary_without_value
import emberr.shared.generated.resources.database_color_rules
import emberr.shared.generated.resources.database_color_rules_explanation
import emberr.shared.generated.resources.database_delete_rule
import emberr.shared.generated.resources.database_quoted_value
import emberr.shared.generated.resources.database_when
import emberr.shared.generated.resources.database_when_this_property_matches
import emberr.shared.generated.resources.editor_table_background_color
import emberr.shared.generated.resources.editor_table_text_color
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DatabaseColorRulesOption(block: DatabaseBlock, editor: DatabaseBlockEditor) {
    DatabaseMenuLayer(
        title = stringResource(Res.string.database_color_rules),
        anchor = { openLayer ->
            DatabaseMenuOption(
                label = stringResource(Res.string.database_color_rules),
                icon = { DatabaseOptionIcon(Res.drawable.palette) },
                trailing = if (block.colorRules.isEmpty()) null else {
                    {
                        Text(
                            text = block.colorRules.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1
                        )
                    }
                },
                onClick = openLayer
            )
        }
    ) { _ ->
        if (block.colorRules.isEmpty()) {
            DatabaseMenuMessage(
                text = stringResource(Res.string.database_color_rules_explanation),
                color = MaterialTheme.colorScheme.outline
            )
        }
        block.colorRules.forEach { rule ->
            key(rule.id) {
                DatabaseColorRuleOption(block = block, rule = rule, editor = editor)
            }
        }
        DatabaseMenuLayer(
            title = stringResource(Res.string.database_add_rule),
            anchor = { openLayer ->
                DatabaseMenuOption(label = stringResource(Res.string.database_add_rule), icon = { DatabaseOptionIcon(Res.drawable.plus) }, onClick = openLayer)
            }
        ) { closeLayerAnd ->
            DatabaseMenuSectionLabel(text = stringResource(Res.string.database_when_this_property_matches))
            (listOf<DatabaseColumnTarget>(DatabaseColumnTarget.NotesTitle) + block.columns).forEach { column ->
                DatabaseMenuOption(
                    label = block.displayLabelOf(column),
                    icon = { DatabaseOptionIcon(block.iconOf(column)) },
                    onClick = { closeLayerAnd { editor.addColorRule(block.id, column) } }
                )
            }
        }
    }
}

@Composable
private fun DatabaseColorRuleOption(block: DatabaseBlock, rule: DatabaseColorRule, editor: DatabaseBlockEditor) {
    fun changeRule(change: (DatabaseColorRule) -> DatabaseColorRule) = editor.changeColorRule(block.id, rule.id, change)
    val people = rememberPeopleToPick().orEmpty()

    DatabaseMenuLayer(
        title = stringResource(Res.string.database_color_rule),
        anchor = { openLayer ->
            DatabaseMenuOption(
                label = block.summaryOf(rule.condition) { personNoteId -> people.firstOrNull { it.id == personNoteId }?.displayName() },
                icon = { DatabaseColorRuleSwatch(rule) },
                onClick = openLayer
            )
        }
    ) { closeLayerAnd ->
        DatabaseMenuSectionLabel(text = stringResource(Res.string.database_when))
        DatabaseFilterEditor(
            block = block,
            filter = rule.condition,
            editor = editor,
            onChange = { change -> changeRule { it.copy(condition = change(it.condition)) } },
            onRemove = { closeLayerAnd { editor.removeColorRule(block.id, rule.id) } },
            removeLabel = stringResource(Res.string.database_delete_rule)
        )

        DatabaseMenuSectionDivider()
        DatabaseMenuSectionLabel(text = stringResource(Res.string.database_color))
        DatabaseColorRuleTarget.entries.forEach { target ->
            DatabaseMenuOption(
                label = target.displayName(),
                isSelected = target == rule.target,
                onClick = { changeRule { it.copy(target = target) } }
            )
        }

        DatabaseMenuSectionDivider()
        DatabaseMenuSectionLabel(text = stringResource(Res.string.editor_table_text_color))
        DatabaseStyleColorChoices(
            selectedColorName = rule.textColorName,
            colorOf = { databaseTextColorNamed(it.storageName) },
            onColorChosen = { colorName -> changeRule { it.copy(textColorName = colorName) } }
        )

        DatabaseMenuSectionDivider()
        DatabaseMenuSectionLabel(text = stringResource(Res.string.editor_table_background_color))
        DatabaseStyleColorChoices(
            selectedColorName = rule.backgroundColorName,
            colorOf = { databaseBackgroundColorNamed(it.storageName) },
            onColorChosen = { colorName -> changeRule { it.copy(backgroundColorName = colorName) } }
        )
    }
}

@Composable
private fun DatabaseColorRuleSwatch(rule: DatabaseColorRule) {
    val color = databaseBackgroundColorNamed(rule.backgroundColorName)
        ?: databaseTextColorNamed(rule.textColorName)
        ?: MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun DatabaseBlock.summaryOf(condition: DatabaseFilter, personNameOf: (personNoteId: String) -> String?): String {
    val valueText = filterValueText(condition, valueTypeOf(condition.target) ?: PropertyValueType.TEXT, personNameOf)
    val columnLabel = displayLabelOf(condition.target)
    val conditionLabel = condition.condition.displayName()
    return if (valueText == null) {
        stringResource(Res.string.database_color_rule_summary_without_value, columnLabel, conditionLabel)
    } else {
        stringResource(Res.string.database_color_rule_summary, columnLabel, conditionLabel, valueText)
    }
}

@Composable
private fun filterValueText(filter: DatabaseFilter, valueType: PropertyValueType, personNameOf: (personNoteId: String) -> String?): String? = when {
    !filter.condition.needsValue -> null
    valueType.holdsDate && filter.condition == DatabaseFilterCondition.IS_WITHIN -> filter.dateRange?.displayName()?.lowercase()
    valueType.holdsDate -> filter.relativeDate?.displayName()?.lowercase() ?: filter.date?.let { formatPropertyDate(it) }
    valueType.holdsTags -> filter.tagName
    valueType.holdsPeople -> filter.personNoteId?.let(personNameOf)
    else -> filter.text.trim().takeIf { it.isNotEmpty() }?.let { stringResource(Res.string.database_quoted_value, it) }
}
