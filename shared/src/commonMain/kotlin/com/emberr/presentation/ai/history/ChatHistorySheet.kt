package com.emberr.presentation.ai.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.ai.RagViewModel
import com.emberr.presentation.ai.components.DesktopMenuRowHorizontalPadding
import com.emberr.presentation.ai.components.RagDesktopMenuItem
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrSearchSheetBody
import com.emberr.presentation.shared.components.EmberrTextField
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.ai_chat_history_title
import emberr.shared.generated.resources.ai_new_chat
import emberr.shared.generated.resources.ai_no_chats_match
import emberr.shared.generated.resources.ai_no_chats_yet
import emberr.shared.generated.resources.ai_recent_chats
import emberr.shared.generated.resources.ai_search_chats
import org.jetbrains.compose.resources.stringResource

private val SheetEdgePadding = 20.dp

@Composable
internal fun ChatHistorySheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    viewModel: RagViewModel
) {
    EmberrBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = stringResource(Res.string.ai_chat_history_title),
        contentHorizontalPadding = 0.dp,
        scrollsContent = false
    ) { closeAnd ->
        ChatHistoryMenuContent(viewModel = viewModel, closeAnd = closeAnd)
    }
}

@Composable
internal fun ChatHistoryMenuContent(
    viewModel: RagViewModel,
    closeAnd: (() -> Unit) -> Unit
) {
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val rowHorizontalPadding = if (isDesktopPlatform) DesktopMenuRowHorizontalPadding else SheetEdgePadding
    val sectionLabelHorizontalPadding = if (isDesktopPlatform) 0.dp else SheetEdgePadding

    val filteredSessions = remember(sessions, searchQuery) {
        if (searchQuery.isBlank()) sessions
        else sessions.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val historyRows = @Composable {
        Text(
            text = stringResource(Res.string.ai_recent_chats),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = sectionLabelHorizontalPadding)
        )
        Spacer(Modifier.height(8.dp))

        if (filteredSessions.isEmpty()) {
            Text(
                text = stringResource(if (searchQuery.isBlank()) Res.string.ai_no_chats_yet else Res.string.ai_no_chats_match),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 12.dp, horizontal = rowHorizontalPadding)
            )
        } else {
            Column {
                filteredSessions.forEach { session ->
                    ChatSessionRow(
                        session = session,
                        isActive = session.id == currentSessionId,
                        onClick = { closeAnd { viewModel.loadSession(session.id) } },
                        onRename = { newTitle -> viewModel.renameSession(session.id, newTitle) },
                        onDelete = { viewModel.deleteSession(session.id) }
                    )
                }
            }
        }

        val newChatIcon = @Composable {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        if (isDesktopPlatform) {
            RagDesktopMenuItem(icon = newChatIcon, text = stringResource(Res.string.ai_new_chat), onClick = { closeAnd { viewModel.clearChat() } })
        } else {
            EmberrBottomSheetOption(label = stringResource(Res.string.ai_new_chat), icon = newChatIcon, onClick = { closeAnd { viewModel.clearChat() } })
        }
    }

    if (isDesktopPlatform) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Spacer(Modifier.height(10.dp))
            EmberrTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(Res.string.ai_search_chats),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = rowHorizontalPadding)
            )
            Spacer(Modifier.height(10.dp))
            historyRows()
        }
    } else {
        EmberrSearchSheetBody(
            searchText = searchQuery,
            onSearchTextChange = { searchQuery = it },
            placeholder = stringResource(Res.string.ai_search_chats)
        ) {
            historyRows()
        }
    }
}
