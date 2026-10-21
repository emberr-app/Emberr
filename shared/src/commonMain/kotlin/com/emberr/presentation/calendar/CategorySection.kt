package com.emberr.presentation.calendar

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.settings.SettingsActionRow
import com.emberr.presentation.settings.SettingsDivider
import com.emberr.presentation.settings.SettingsGroup
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetAction
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.MenuAtTap
import com.emberr.presentation.shared.components.menuTapAnchor
import com.emberr.presentation.shared.components.rememberMenuTapAnchor
import com.emberr.presentation.shared.components.EmberrTextField
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_right
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.trash
import org.jetbrains.compose.resources.painterResource

private val CategoryMenuWidth = 320.dp
private val CategoryMenuHorizontalPadding = 14.dp
private val CategoryMenuVerticalPadding = 12.dp
private val CategoryMenuHeaderHeight = 32.dp
private val CategoryMenuContentInset = 6.dp
private val CategoryEditorSectionSpacing = 16.dp
private val CategoryEditorLabelSpacing = 8.dp
private val CategoryEditorButtonsSpacing = 24.dp
private val CategorySwatchSize = 26.dp
private val CategorySwatchRingRoom = 5.dp
private val SwatchBounceEasing = CubicBezierEasing(0.3f, 1.5f, 0.6f, 1f)

private data class CategoryEditorState(
    val categoryId: String?,
    val name: String,
    val colorHex: String
)

@Composable
fun CategorySection(
    categories: List<CalendarCategory>,
    onAddCategory: (name: String, colorHex: String) -> Unit,
    onUpdateCategory: (id: String, name: String, colorHex: String) -> Unit,
    onDeleteCategory: (id: String) -> Unit,
    hiddenCategoryVisibilityKeys: Set<String>,
    onToggleCategoryVisibility: (visibilityKey: String) -> Unit,
    spaces: List<SpaceEntity>,
    cardColor: Color = MaterialTheme.colorScheme.surface
) {
    val noCategoryVisibilityKey = noCategoryVisibilityKeyFor(ACCOUNT_SPACE_ID)
    var editorState by remember { mutableStateOf<CategoryEditorState?>(null) }

    fun isNameTakenByAnotherCategory(state: CategoryEditorState?): Boolean =
        state != null && categories.any { category ->
            category.id != state.categoryId && category.name.trim().lowercase() == state.name.trim().lowercase()
        }

    fun performSave() {
        val state = editorState
        val trimmedName = state?.name?.trim() ?: return
        if (trimmedName.isNotEmpty() && !isNameTakenByAnotherCategory(state)) {
            val id = state.categoryId
            if (id == null) {
                onAddCategory(trimmedName, state.colorHex)
            } else {
                onUpdateCategory(id, trimmedName, state.colorHex)
            }
        }
    }

    SettingsGroup(title = "Categories", cardColor = cardColor) {
        categories.forEach { category ->
            val state = editorState
            CategoryRow(
                category = category,
                isShown = category.id !in hiddenCategoryVisibilityKeys,
                onToggleShown = { onToggleCategoryVisibility(category.id) },
                isEditorOpen = isDesktopPlatform && state?.categoryId == category.id,
                editorState = if (state?.categoryId == category.id) state else null,
                isNameTaken = state?.categoryId == category.id && isNameTakenByAnotherCategory(state),
                onClick = {
                    editorState = CategoryEditorState(
                        categoryId = category.id,
                        name = category.name,
                        colorHex = category.colorHex
                    )
                },
                onDismissEditor = { editorState = null },
                onNameChange = { editorState = editorState?.copy(name = it) },
                onColorChange = { editorState = editorState?.copy(colorHex = it) },
                onCancel = { editorState = null },
                onSave = { performSave(); editorState = null },
                onDelete = { onDeleteCategory(category.id); editorState = null }
            )
            SettingsDivider()
        }

        VisibilityToggleRow(
            label = "No Category",
            isShown = noCategoryVisibilityKey !in hiddenCategoryVisibilityKeys,
            onToggleShown = { onToggleCategoryVisibility(noCategoryVisibilityKey) }
        )
        SettingsDivider()

        val addState = editorState
        AddCategoryRow(
            isEditorOpen = isDesktopPlatform && addState?.categoryId == null && addState != null,
            editorState = if (addState?.categoryId == null) addState else null,
            isNameTaken = addState?.categoryId == null && isNameTakenByAnotherCategory(addState),
            onClick = {
                editorState = CategoryEditorState(
                    categoryId = null,
                    name = "",
                    colorHex = CategoryPastelPalette.first()
                )
            },
            onDismissEditor = { editorState = null },
            onNameChange = { editorState = editorState?.copy(name = it) },
            onColorChange = { editorState = editorState?.copy(colorHex = it) },
            onCancel = { editorState = null },
            onSave = { performSave(); editorState = null }
        )
    }

    val isAnySpaceHidden = spaces.any { space -> spaceVisibilityKeyFor(space.spaceId) in hiddenCategoryVisibilityKeys }
    var keepsSpacesGroupShown by remember { mutableStateOf(false) }
    if (spaces.size >= 2 || isAnySpaceHidden) keepsSpacesGroupShown = true
    if (keepsSpacesGroupShown && spaces.isNotEmpty()) {
        SettingsGroup(title = "Spaces", cardColor = cardColor) {
            spaces.forEachIndexed { index, space ->
                if (index > 0) SettingsDivider()
                val spaceVisibilityKey = spaceVisibilityKeyFor(space.spaceId)
                VisibilityToggleRow(
                    label = space.displayName.trim().ifBlank { "Untitled space" },
                    isShown = spaceVisibilityKey !in hiddenCategoryVisibilityKeys,
                    onToggleShown = { onToggleCategoryVisibility(spaceVisibilityKey) }
                )
            }
        }
    }

    if (!isDesktopPlatform) {
        val state = editorState
        EmberrBottomSheet(
            expanded = state != null,
            onDismiss = { editorState = null },
            title = if (state?.categoryId == null) "Add Category" else "Edit Category",
            headerAction = state?.categoryId?.let { id ->
                EmberrBottomSheetAction(
                    icon = painterResource(Res.drawable.trash),
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    tooltip = "Delete category",
                    onClick = {
                        onDeleteCategory(id)
                        editorState = null
                    }
                )
            }
        ) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                if (state != null) {
                    CategoryEditor(
                        state = state,
                        isNew = state.categoryId == null,
                        isNameTaken = isNameTakenByAnotherCategory(state),
                        onNameChange = { editorState = state.copy(name = it) },
                        onColorChange = { editorState = state.copy(colorHex = it) },
                        onCancel = { closeAnd { editorState = null } },
                        onSave = { closeAnd { performSave(); editorState = null } }
                    )
                }
            }
        }
    }
}

@Composable
private fun keepShowingWhileClosing(editorState: CategoryEditorState?): CategoryEditorState? {
    val lastOpenEditorState = remember { arrayOfNulls<CategoryEditorState>(1) }
    if (editorState != null) lastOpenEditorState[0] = editorState
    return editorState ?: lastOpenEditorState[0]
}

@Composable
private fun CategoryRow(
    category: CalendarCategory,
    isShown: Boolean,
    onToggleShown: () -> Unit,
    isEditorOpen: Boolean,
    editorState: CategoryEditorState?,
    isNameTaken: Boolean,
    onClick: () -> Unit,
    onDismissEditor: () -> Unit,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val tapAnchor = rememberMenuTapAnchor()
    val shownEditorState = keepShowingWhileClosing(editorState)

    Box(modifier = Modifier.fillMaxWidth().menuTapAnchor(tapAnchor)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 4.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryVisibilityDot(
                color = category.colorHex.toCategoryColor(),
                isShown = isShown,
                onToggleShown = onToggleShown
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Icon(
                painter = painterResource(Res.drawable.chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                modifier = Modifier.size(18.dp)
            )
        }

        MenuAtTap(tapAnchor, menuWidth = CategoryMenuWidth) {
            EmberrDesktopMenu(
                expanded = isEditorOpen,
                onDismissRequest = onDismissEditor,
                modifier = Modifier.width(CategoryMenuWidth)
            ) {
                if (shownEditorState != null) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = CategoryMenuHorizontalPadding,
                            vertical = CategoryMenuVerticalPadding
                        )
                    ) {
                        CategoryMenuHeader(title = "Edit Category", onDelete = onDelete)
                        CategoryEditor(
                            state = shownEditorState,
                            isNew = false,
                            isNameTaken = isNameTaken,
                            onNameChange = onNameChange,
                            onColorChange = onColorChange,
                            onCancel = onCancel,
                            onSave = onSave
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisibilityToggleRow(
    label: String,
    isShown: Boolean,
    onToggleShown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleShown)
            .padding(start = 4.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryVisibilityDot(
            color = MaterialTheme.colorScheme.outline,
            isShown = isShown,
            onToggleShown = onToggleShown
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CategoryVisibilityDot(
    color: Color,
    isShown: Boolean,
    onToggleShown: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleShown
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .then(
                    if (isShown) Modifier.background(color, CircleShape)
                    else Modifier.border(2.dp, color, CircleShape)
                )
        )
    }
}

@Composable
private fun AddCategoryRow(
    isEditorOpen: Boolean,
    editorState: CategoryEditorState?,
    isNameTaken: Boolean,
    onClick: () -> Unit,
    onDismissEditor: () -> Unit,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val tapAnchor = rememberMenuTapAnchor()
    val shownEditorState = keepShowingWhileClosing(editorState)

    Box(modifier = Modifier.fillMaxWidth().menuTapAnchor(tapAnchor)) {
        SettingsActionRow(
            icon = painterResource(Res.drawable.plus),
            title = "Add Category",
            onClick = onClick
        )

        MenuAtTap(tapAnchor, menuWidth = CategoryMenuWidth) {
            EmberrDesktopMenu(
                expanded = isEditorOpen,
                onDismissRequest = onDismissEditor,
                modifier = Modifier.width(CategoryMenuWidth)
            ) {
                if (shownEditorState != null) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = CategoryMenuHorizontalPadding,
                            vertical = CategoryMenuVerticalPadding
                        )
                    ) {
                        CategoryMenuHeader(title = "Add Category", onDelete = null)
                        CategoryEditor(
                            state = shownEditorState,
                            isNew = true,
                            isNameTaken = isNameTaken,
                            onNameChange = onNameChange,
                            onColorChange = onColorChange,
                            onCancel = onCancel,
                            onSave = onSave
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryMenuHeader(
    title: String,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CategoryMenuHeaderHeight)
            .padding(horizontal = CategoryMenuContentInset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (onDelete != null) {
            IconTooltip(text = "Delete category") {
                Box(
                    modifier = Modifier
                        .size(CategoryMenuHeaderHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDelete
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.trash),
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(CategoryEditorSectionSpacing))
}

@Composable
private fun CategoryEditor(
    state: CategoryEditorState,
    isNew: Boolean,
    isNameTaken: Boolean,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        EmberrTextField(
            value = state.name,
            onValueChange = onNameChange,
            placeholder = "Category name",
            modifier = Modifier.fillMaxWidth(),
            onSubmit = { if (!isNameTaken) onSave() }
        )

        if (isNameTaken) {
            Text(
                text = "Category already exists",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = CategoryMenuContentInset, top = CategoryEditorLabelSpacing)
            )
        }

        Text(
            text = "Color",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(
                start = CategoryMenuContentInset,
                top = CategoryEditorSectionSpacing,
                bottom = CategoryEditorLabelSpacing
            )
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CategoryMenuContentInset, vertical = CategorySwatchRingRoom)
        ) {
            CategoryPastelPalette.forEach { colorHex ->
                CategoryColorSwatch(
                    color = colorHex.toCategoryColor(),
                    isSelected = colorHex == state.colorHex,
                    onClick = { onColorChange(colorHex) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CategoryEditorButtonsSpacing),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmberrButtonSecondary(
                text = "Cancel",
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            )
            EmberrButtonPrimary(
                text = if (isNew) "Add" else "Save",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !isNameTaken
            )
        }
    }
}

@Composable
private fun CategoryColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val circleScale by animateFloatAsState(
        targetValue = when {
            isSelected -> 0.95f
            isHovered -> 1.1f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 180, easing = SwatchBounceEasing)
    )
    val selectedRingAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 180)
    )

    Box(
        modifier = Modifier
            .size(CategorySwatchSize)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .drawBehind {
                val circleRadius = size.minDimension / 2f * circleScale
                drawCircle(color = color, radius = circleRadius)
                drawCircle(
                    color = color.copy(alpha = selectedRingAlpha),
                    radius = circleRadius + 4.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
    )
}
