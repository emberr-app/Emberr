package com.emberr.presentation.ai.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.domain.ai.chat.ChatSession
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.ai.components.ModelOptionCard
import com.emberr.presentation.ai.components.clickableWithoutMobileRipple
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.EmberrShadowElevation
import com.emberr.presentation.shared.components.EmberrTextField
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.ai_chat_name
import emberr.shared.generated.resources.ai_chat_options
import emberr.shared.generated.resources.ai_chat_options_title
import emberr.shared.generated.resources.ai_delete_this_chat
import emberr.shared.generated.resources.ai_delete_this_chat_message
import emberr.shared.generated.resources.ai_rename
import emberr.shared.generated.resources.ai_rename_chat
import emberr.shared.generated.resources.common_cancel
import emberr.shared.generated.resources.common_close
import emberr.shared.generated.resources.common_delete
import emberr.shared.generated.resources.common_save
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ChatSessionRow(
    session: ChatSession,
    isActive: Boolean,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val chatOptionsButton: @Composable () -> Unit = {
        Box {
            IconTooltip(text = stringResource(Res.string.ai_chat_options)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(Res.string.ai_chat_options),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickableWithoutMobileRipple { showMenu = true }
                )
            }

            if (isDesktopPlatform) {
                EmberrDesktopMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.ai_rename)) },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.common_delete)) },
                        onClick = {
                            showMenu = false
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }

    if (isDesktopPlatform) {
        EmberrDesktopMenuOption(
            label = session.title,
            subtitle = session.messages.lastOrNull()?.text.orEmpty(),
            isSelected = isActive,
            trailing = chatOptionsButton,
            onClick = onClick
        )
    } else {
        EmberrBottomSheetOption(
            label = session.title,
            subtitle = session.messages.lastOrNull()?.text.orEmpty(),
            isSelected = isActive,
            labelMaxLines = 1,
            subtitleMaxLines = 1,
            trailing = chatOptionsButton,
            onClick = onClick
        )
    }

    if (!isDesktopPlatform) {
        ChatSessionOptionsBottomSheet(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            onRenameClick = { showRenameDialog = true },
            onDeleteClick = { showDeleteConfirm = true }
        )
    }

    if (showRenameDialog) {
        var titleInput by remember { mutableStateOf(session.title) }
        EmberrAlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = stringResource(Res.string.ai_rename_chat)
        ) {
            EmberrTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                placeholder = stringResource(Res.string.ai_chat_name),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                onSubmit = { onRename(titleInput); showRenameDialog = false }
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EmberrButtonSecondary(
                    text = stringResource(Res.string.common_cancel),
                    onClick = { showRenameDialog = false },
                    modifier = Modifier.weight(1f)
                )
                EmberrButtonPrimary(
                    text = stringResource(Res.string.common_save),
                    onClick = { onRename(titleInput); showRenameDialog = false },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        EmberrAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = stringResource(Res.string.ai_delete_this_chat)
        ) {
            Text(
                text = stringResource(Res.string.ai_delete_this_chat_message),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EmberrButtonSecondary(
                    text = stringResource(Res.string.common_cancel),
                    onClick = { showDeleteConfirm = false },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = EmberrShadowElevation.None)
                ) {
                    Text(stringResource(Res.string.common_delete), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun ChatSessionOptionsBottomSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    EmberrBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = stringResource(Res.string.ai_chat_options_title),
    ) { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            ModelOptionCard(
                icon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                title = stringResource(Res.string.ai_rename),
                subtitle = null,
                onClick = { closeAnd { onRenameClick() } }
            )
            Spacer(Modifier.height(10.dp))
            ModelOptionCard(
                icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) },
                title = stringResource(Res.string.common_delete),
                subtitle = null,
                onClick = { closeAnd { onDeleteClick() } }
            )

            EmberrButtonPrimary(
                text = stringResource(Res.string.common_close),
                onClick = { closeAnd { } },
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }
    }
}
