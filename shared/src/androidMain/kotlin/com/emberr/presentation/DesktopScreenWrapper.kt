package com.emberr.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.emberr.domain.backup.manual.BackupContentChoice
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.util.export.MarkdownExport
import com.emberr.presentation.ai.RagViewModel

@Composable
actual fun DesktopMainScreenWrapper(
    isSidebarVisible: Boolean,
    sidebarWidth: Dp,
    onToggleSidebar: () -> Unit,
    onSelectionModeChange: (Boolean) -> Unit,
    onPickImage: ((String) -> Unit) -> Unit,
    onTakePhoto: ((String) -> Unit) -> Unit,
    onPickDocument: ((String) -> Unit) -> Unit,
    onOpenFile: (String, String) -> Unit,
    onExportMarkdown: (MarkdownExport) -> Unit,
    onExportPdf: (String, String, List<NoteBlock>) -> Unit,
    onExportBackup: (choice: BackupContentChoice) -> Unit,
    onAiIconTap: () -> Unit,
    isAiChatVisible: Boolean,
    ragViewModel: RagViewModel?,
    onDismissAiChat: () -> Unit,
    isUpdatePeekDue: Boolean
) {}