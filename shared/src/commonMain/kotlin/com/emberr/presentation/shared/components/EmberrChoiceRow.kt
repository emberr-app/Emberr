package com.emberr.presentation.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.util.system.isDesktopPlatform
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_down
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val ChoiceSheetButtonPadding = 20.dp
private val ChoiceMenuWidth = 280.dp
private val ChoiceMenuVerticalPadding = 6.dp
private val ChoiceMenuGap = 6.dp
private val ChoiceRowIconSize = 18.dp
private val ValueButtonShape = RoundedCornerShape(8.dp)
private val ValueButtonIconSize = 14.dp
private val ValueButtonMaxTextWidth = 160.dp
private const val VALUE_BUTTON_BACKGROUND_ALPHA = 0.08f
private const val VALUE_BUTTON_CONTENT_ALPHA = 0.7f

@Composable
fun EmberrChoiceRow(
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
                modifier = Modifier.size(ChoiceRowIconSize)
            )
        },
        trailing = {
            Box {
                ChoiceValueButton(value = value, isMenuOpen = isMenuOpen, onClick = { isMenuOpen = true })
                if (isDesktopPlatform) {
                    EmberrDesktopMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false },
                        offset = DpOffset(x = 0.dp, y = ChoiceMenuGap),
                        modifier = Modifier.width(ChoiceMenuWidth)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = ChoiceMenuVerticalPadding)) {
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
                        .padding(top = 12.dp, start = ChoiceSheetButtonPadding, end = ChoiceSheetButtonPadding)
                )
            }
        }
    }
}

@Composable
private fun ChoiceValueButton(value: String, isMenuOpen: Boolean, onClick: () -> Unit) {
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
fun EmberrCheckboxOption(label: String, isChecked: Boolean, onClick: () -> Unit) {
    val checkbox: @Composable () -> Unit = {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = null,
                modifier = Modifier.scale(0.9f).size(16.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.surface,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
    if (isDesktopPlatform) {
        EmberrDesktopMenuOption(label = label, icon = checkbox, onClick = onClick)
    } else {
        EmberrBottomSheetOption(label = label, icon = checkbox, onClick = onClick)
    }
}

@Composable
fun EmberrSpaceCheckboxOptions(
    spaces: List<SpaceEntity>,
    checkedSpaceIds: Set<String>,
    onCheckedSpaceIdsChange: (Set<String>) -> Unit
) {
    val allSpaceIds = spaces.map { it.spaceId }.toSet()
    EmberrCheckboxOption(
        label = "Select All",
        isChecked = checkedSpaceIds == allSpaceIds,
        onClick = { onCheckedSpaceIdsChange(if (checkedSpaceIds == allSpaceIds) emptySet() else allSpaceIds) }
    )
    spaces.forEach { space ->
        EmberrCheckboxOption(
            label = space.displayName,
            isChecked = space.spaceId in checkedSpaceIds,
            onClick = {
                onCheckedSpaceIdsChange(
                    if (space.spaceId in checkedSpaceIds) checkedSpaceIds - space.spaceId else checkedSpaceIds + space.spaceId
                )
            }
        )
    }
}

fun checkedSpacesLabel(spaces: List<SpaceEntity>, checkedSpaceIds: Set<String>): String = when {
    spaces.isEmpty() -> "None yet"
    checkedSpaceIds.isEmpty() -> "None"
    checkedSpaceIds.size == spaces.size && spaces.size == 1 -> spaces.single().displayName
    checkedSpaceIds.size == spaces.size -> "All ${spaces.size} spaces"
    else -> "${checkedSpaceIds.size} of ${spaces.size} spaces"
}
