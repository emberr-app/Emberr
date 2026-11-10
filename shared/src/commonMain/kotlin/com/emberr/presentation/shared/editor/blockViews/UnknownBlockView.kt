package com.emberr.presentation.shared.editor.blockViews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.editor.DefaultBlockShape
import emberr.shared.generated.resources.editor_block_needs_newer_version
import emberr.shared.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
fun UnknownBlockView() {
    Text(
        text = stringResource(Res.string.editor_block_needs_newer_version),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(DefaultBlockShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
