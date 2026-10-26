package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.EmberrDesktopSubmenu
import com.emberr.presentation.shared.components.emberrBorderColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val DesktopMenuMinWidth = 240.dp
private val DesktopMenuMaxWidth = 300.dp
private val DesktopMenuVerticalPadding = 4.dp
private val SheetBottomPadding = 16.dp
private val MenuTextInset = 20.dp
private val SheetActionRowShape = RoundedCornerShape(12.dp)
private val SheetActionRowSpacing = 2.dp
private val SheetActionRowIconGap = 12.dp
private val MenuContentInset = if (isDesktopPlatform) 8.dp else 20.dp

@Composable
internal fun MovieMenu(
    expanded: Boolean,
    title: String,
    onDismiss: () -> Unit,
    showsCloseButton: Boolean = true,
    content: @Composable ColumnScope.(closeAnd: (() -> Unit) -> Unit) -> Unit
) {
    if (isDesktopPlatform) {
        EmberrDesktopMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.widthIn(min = DesktopMenuMinWidth, max = DesktopMenuMaxWidth)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = DesktopMenuVerticalPadding)) {
                content { action ->
                    action()
                    onDismiss()
                }
            }
        }
    } else {
        EmberrBottomSheet(
            expanded = expanded,
            onDismiss = onDismiss,
            title = title,
            contentHorizontalPadding = 0.dp
        ) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = SheetBottomPadding)) {
                content(closeAnd)
                if (showsCloseButton) {
                    EmberrButtonPrimary(
                        text = "Close",
                        onClick = { closeAnd { } },
                        modifier = Modifier.fillMaxWidth().padding(start = MenuTextInset, end = MenuTextInset, top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun MovieMenuLayer(
    title: String,
    anchor: @Composable (openLayer: () -> Unit) -> Unit,
    showsCloseButton: Boolean = true,
    anchorIsMenuRow: Boolean = true,
    content: @Composable ColumnScope.(closeLayerAnd: (() -> Unit) -> Unit) -> Unit
) {
    EmberrDesktopSubmenu(
        opensOnHover = anchorIsMenuRow,
        menuWidthEstimate = DesktopMenuMaxWidth,
        anchor = { openLayer, _ -> anchor(openLayer) }
    ) { isOpen, closeLayer ->
        MovieMenu(
            expanded = isOpen,
            title = title,
            onDismiss = closeLayer,
            showsCloseButton = showsCloseButton,
            content = content
        )
    }
}

@Composable
internal fun MovieMenuContent(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = MenuContentInset), content = content)
}

@Composable
internal fun MovieMenuButtons(cancelText: String, onCancel: () -> Unit, confirmText: String, onConfirm: () -> Unit) {
    MovieMenuContent {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmberrButtonSecondary(text = cancelText, onClick = onCancel, modifier = Modifier.weight(1f))
            EmberrButtonPrimary(text = confirmText, onClick = onConfirm, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
internal fun MovieMenuOption(
    label: String,
    onClick: () -> Unit,
    isSelected: Boolean? = null,
    icon: DrawableResource? = null,
    labelColor: Color? = null
) {
    val iconContent: (@Composable () -> Unit)? = icon?.let {
        {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = labelColor ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
    when {
        isDesktopPlatform -> EmberrDesktopMenuOption(
            label = label,
            onClick = onClick,
            isSelected = isSelected == true,
            icon = iconContent,
            labelColor = labelColor
        )
        isSelected != null -> EmberrBottomSheetOption(
            label = label,
            onClick = onClick,
            isSelected = isSelected,
            icon = iconContent,
            labelColor = labelColor
        )
        else -> MovieSheetActionRow(label = label, onClick = onClick, icon = iconContent, labelColor = labelColor)
    }
}

@Composable
private fun MovieSheetActionRow(
    label: String,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)?,
    labelColor: Color?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MenuTextInset, vertical = SheetActionRowSpacing)
            .heightIn(min = 48.dp)
            .clip(SheetActionRowShape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(SheetActionRowIconGap))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun MovieMenuSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(start = MenuTextInset, end = MenuTextInset, top = 8.dp, bottom = if (isDesktopPlatform) 4.dp else 10.dp)
    )
}

@Composable
internal fun MovieMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = MenuTextInset, vertical = 8.dp),
        thickness = 0.5.dp,
        color = emberrBorderColor
    )
}
