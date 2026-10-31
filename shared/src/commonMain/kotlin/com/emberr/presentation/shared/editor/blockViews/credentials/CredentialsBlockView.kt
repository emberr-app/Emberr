package com.emberr.presentation.shared.editor.blockViews.credentials

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.CredentialsBlock
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.editor.DefaultBlockShape

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CredentialsBlockView(
    block: CredentialsBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit
) {
    Text(
        text = block.serviceName.ifBlank { "Login" },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(DefaultBlockShape)
            .background(MaterialTheme.colorScheme.surface)
            .emberrBorder(DefaultBlockShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (inSelectionMode) onToggleSelection() },
                onLongClick = if (isDesktopPlatform) null else onToggleSelection
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
