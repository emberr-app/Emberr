package com.emberr.presentation.shared.editor.slash

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource

val SlashMenuIconSlot = 18.dp
val SlashMenuIconSize = 16.dp

sealed interface SlashMenuIcon {
    data class Drawable(val resource: DrawableResource, val size: Dp = SlashMenuIconSize) : SlashMenuIcon
    data class Vector(val image: ImageVector, val size: Dp = SlashMenuIconSlot) : SlashMenuIcon
    data class Label(val text: String) : SlashMenuIcon
}

data class SlashMenuItemData(
    val label: String,
    val icon: SlashMenuIcon,
    val action: () -> Unit,
    val submenuSections: List<SlashMenuSectionData>?,
    val searchWords: List<String> = emptyList()
) {
    constructor(label: String, icon: SlashMenuIcon, action: () -> Unit) : this(label, icon, action, null)
}

fun SlashMenuItemData(label: String, icon: DrawableResource, size: Dp = SlashMenuIconSize, action: () -> Unit) =
    SlashMenuItemData(label, SlashMenuIcon.Drawable(icon, size), action)

fun SlashMenuItemData(label: String, icon: ImageVector, size: Dp = SlashMenuIconSlot, action: () -> Unit) =
    SlashMenuItemData(label, SlashMenuIcon.Vector(icon, size), action)

data class SlashMenuSectionData(
    val title: String,
    val items: List<SlashMenuItemData>
)
