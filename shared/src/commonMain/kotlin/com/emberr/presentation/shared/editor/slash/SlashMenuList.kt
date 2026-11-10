package com.emberr.presentation.shared.editor.slash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.CustomPropertyEntity
import com.emberr.domain.model.TextAlignment
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopSubmenu
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import com.emberr.presentation.shared.components.orHighContrastLine
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_right
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.image
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt
import emberr.shared.generated.resources.editor_no_results_found
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopSlashMenuContent(
    query: String,
    onChangeBlockType: (String) -> Unit,
    onToggleFormat: (String) -> Unit,
    onAdjustIndentation: (Boolean) -> Unit,
    onSetAlignment: (TextAlignment) -> Unit,
    onInsertMediaBlock: (String) -> Unit,
    onInsertText: (String) -> Unit,
    selectedIndex: Int = -1,
    customProperties: List<CustomPropertyEntity> = emptyList()
) {
    val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current
    val slashMenuLabels = rememberSlashMenuLabels()
    val filteredSections = remember(
        slashMenuLabels,
        query,
        onChangeBlockType,
        onToggleFormat,
        onAdjustIndentation,
        onSetAlignment,
        onInsertMediaBlock,
        onInsertText,
        usesTwentyFourHourClock,
        customProperties
    ) {
        filteredSlashMenuSections(
            slashMenuLabels,
            query,
            onChangeBlockType,
            onToggleFormat,
            onAdjustIndentation,
            onSetAlignment,
            onInsertMediaBlock,
            onInsertText,
            usesTwentyFourHourClock,
            customProperties
        )
    }

    SlashMenuList(sections = filteredSections, selectedIndex = selectedIndex)
}

@Composable
fun SlashMenuList(sections: List<SlashMenuSectionData>, selectedIndex: Int = -1, maxHeight: Dp = 320.dp, bottomPadding: Dp = 0.dp) {
    val rows = remember(sections) {
        buildList {
            var flatIndex = 0
            sections.forEachIndexed { sectionIndex, section ->
                if (section.title.isNotEmpty()) {
                    add(SlashMenuRow.SectionHeader(section.title))
                }
                section.items.forEach { menuItem ->
                    add(SlashMenuRow.Entry(menuItem, flatIndex))
                    flatIndex++
                }
                val nextSection = sections.getOrNull(sectionIndex + 1)
                if (nextSection != null) {
                    add(if (nextSection.title.isEmpty()) SlashMenuRow.SectionDivider else SlashMenuRow.SectionSpacer)
                }
            }
        }
    }

    val scrollState = rememberScrollState()
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }
    val itemBoundsByFlatIndex = remember(sections) { mutableStateMapOf<Int, ClosedFloatingPointRange<Float>>() }

    LaunchedEffect(selectedIndex, sections) {
        val bounds = itemBoundsByFlatIndex[selectedIndex] ?: return@LaunchedEffect
        val viewportStart = scrollState.value.toFloat()
        val viewportEnd = viewportStart + viewportHeightPx

        if (bounds.endInclusive > viewportEnd) {
            scrollState.animateScrollTo((scrollState.value + (bounds.endInclusive - viewportEnd)).roundToInt())
        } else if (bounds.start < viewportStart) {
            scrollState.animateScrollTo(bounds.start.roundToInt())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .onGloballyPositioned { viewportHeightPx = it.size.height.toFloat() }
            .verticalScroll(scrollState)
            .padding(top = 4.dp, bottom = 4.dp + bottomPadding)
    ) {
        if (sections.isEmpty()) {
            Text(
                text = stringResource(Res.string.editor_no_results_found),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            rows.forEach { row ->
                when (row) {
                    is SlashMenuRow.SectionHeader -> SlashMenuHeader(row.title)
                    is SlashMenuRow.Entry -> {
                        val boundsModifier = Modifier.onGloballyPositioned { coordinates ->
                            val top = coordinates.positionInParent().y
                            val bottom = top + coordinates.size.height
                            itemBoundsByFlatIndex[row.flatIndex] = top..bottom
                        }
                        val submenuSections = row.item.submenuSections
                        if (submenuSections == null) {
                            SlashMenuItem(
                                text = row.item.label,
                                icon = row.item.icon,
                                isSelected = row.flatIndex == selectedIndex,
                                onClick = row.item.action,
                                modifier = boundsModifier
                            )
                        } else {
                            SlashMenuItemWithSubmenu(
                                item = row.item,
                                submenuSections = submenuSections,
                                isSelected = row.flatIndex == selectedIndex,
                                submenuMaxHeight = maxHeight,
                                modifier = boundsModifier
                            )
                        }
                    }
                    SlashMenuRow.SectionSpacer -> Spacer(modifier = Modifier.height(12.dp))
                    SlashMenuRow.SectionDivider -> HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f).orHighContrastLine()
                    )
                }
            }
        }
    }
}

private sealed interface SlashMenuRow {
    data class SectionHeader(val title: String) : SlashMenuRow
    data class Entry(val item: SlashMenuItemData, val flatIndex: Int) : SlashMenuRow
    data object SectionSpacer : SlashMenuRow
    data object SectionDivider : SlashMenuRow
}

private val SlashSubmenuWidth = 260.dp

@Composable
private fun SlashMenuItemWithSubmenu(
    item: SlashMenuItemData,
    submenuSections: List<SlashMenuSectionData>,
    isSelected: Boolean,
    submenuMaxHeight: Dp,
    modifier: Modifier = Modifier
) {
    EmberrDesktopSubmenu(
        modifier = modifier,
        menuWidthEstimate = SlashSubmenuWidth,
        anchor = { openSubmenu, isSubmenuOpen ->
            SlashMenuItem(
                text = item.label,
                icon = item.icon,
                isSelected = isSelected || isSubmenuOpen,
                onClick = openSubmenu,
                showsSubmenuChevron = true
            )
        }
    ) { isSubmenuOpen, closeSubmenu ->
        EmberrDesktopMenu(
            expanded = isSubmenuOpen,
            onDismissRequest = closeSubmenu,
            modifier = Modifier.width(SlashSubmenuWidth)
        ) {
            SlashMenuList(sections = submenuSections, maxHeight = submenuMaxHeight)
        }
    }
}

@Composable
private fun SlashMenuItem(
    text: String,
    icon: SlashMenuIcon,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showsSubmenuChevron: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(SlashMenuIconSlot),
            contentAlignment = Alignment.Center
        ) {
            when (icon) {
                is SlashMenuIcon.Drawable -> Icon(
                    painterResource(icon.resource),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(icon.size)
                )
                is SlashMenuIcon.Vector -> Icon(
                    icon.image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(icon.size)
                )
                is SlashMenuIcon.Label -> Text(
                    icon.text,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = if (showsSubmenuChevron) Modifier.weight(1f) else Modifier
        )
        if (showsSubmenuChevron) {
            Icon(
                painterResource(Res.drawable.chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SlashMenuHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    )
}
