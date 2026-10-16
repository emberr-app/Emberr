package com.emberr.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.settings.SettingsActionRow
import com.emberr.presentation.settings.SettingsDivider
import com.emberr.presentation.settings.SettingsGroup
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.MenuAtTap
import com.emberr.presentation.shared.components.menuTapAnchor
import com.emberr.presentation.shared.components.rememberMenuTapAnchor
import com.emberr.presentation.shared.components.EmberrTextField
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_right
import emberr.shared.generated.resources.plus
import org.jetbrains.compose.resources.painterResource

private val CategoryMenuWidth = 300.dp

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
    cardColor: Color = MaterialTheme.colorScheme.surface
) {
    var editorState by remember { mutableStateOf<CategoryEditorState?>(null) }

    fun performSave() {
        val state = editorState
        val trimmedName = state?.name?.trim() ?: return
        if (trimmedName.isNotEmpty()) {
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
                isEditorOpen = isDesktopPlatform && state?.categoryId == category.id,
                editorState = if (state?.categoryId == category.id) state else null,
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

        val addState = editorState
        AddCategoryRow(
            isEditorOpen = isDesktopPlatform && addState?.categoryId == null && addState != null,
            editorState = if (addState?.categoryId == null) addState else null,
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

    if (!isDesktopPlatform) {
        val state = editorState
        EmberrBottomSheet(
            expanded = state != null,
            onDismiss = { editorState = null },
            title = if (state?.categoryId == null) "Add Category" else "Edit Category"
        ) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                if (state != null) {
                    CategoryEditor(
                        state = state,
                        isNew = state.categoryId == null,
                        onNameChange = { editorState = state.copy(name = it) },
                        onColorChange = { editorState = state.copy(colorHex = it) },
                        onCancel = { closeAnd { editorState = null } },
                        onSave = { closeAnd { performSave(); editorState = null } },
                        onDelete = state.categoryId?.let { id ->
                            {
                                onDeleteCategory(id)
                                closeAnd { editorState = null }
                            }
                        }
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
    isEditorOpen: Boolean,
    editorState: CategoryEditorState?,
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
                .padding(horizontal = 14.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(category.colorHex.toCategoryColor(), CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

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
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "Edit Category",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 18.dp)
                        )
                        CategoryEditor(
                            state = shownEditorState,
                            isNew = false,
                            onNameChange = onNameChange,
                            onColorChange = onColorChange,
                            onCancel = onCancel,
                            onSave = onSave,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCategoryRow(
    isEditorOpen: Boolean,
    editorState: CategoryEditorState?,
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
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "Add Category",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 18.dp)
                        )
                        CategoryEditor(
                            state = shownEditorState,
                            isNew = true,
                            onNameChange = onNameChange,
                            onColorChange = onColorChange,
                            onCancel = onCancel,
                            onSave = onSave,
                            onDelete = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryEditor(
    state: CategoryEditorState,
    isNew: Boolean,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        EmberrTextField(
            value = state.name,
            onValueChange = onNameChange,
            placeholder = "Category name",
            modifier = Modifier.fillMaxWidth(),
            onSubmit = onSave
        )

        Text(
            text = "Color",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CategoryPastelPalette.forEach { colorHex ->
                val isSelected = colorHex == state.colorHex
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(colorHex.toCategoryColor(), CircleShape)
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            shape = CircleShape
                        )
                        .clickable { onColorChange(colorHex) }
                )
            }
        }

        if (onDelete != null) {
            TextButton(
                onClick = onDelete,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "Delete category",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (onDelete != null) 4.dp else 20.dp),
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
                modifier = Modifier.weight(1f)
            )
        }
    }
}
