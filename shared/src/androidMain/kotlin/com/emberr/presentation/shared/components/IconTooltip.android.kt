package com.emberr.presentation.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val TooltipGap = 6.dp
private val TooltipShape = RoundedCornerShape(50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun IconTooltip(
    text: String?,
    position: IconTooltipPosition,
    content: @Composable () -> Unit
) {
    if (text == null) {
        content()
        return
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = when (position) {
                IconTooltipPosition.Below -> TooltipAnchorPosition.Below
                IconTooltipPosition.Above -> TooltipAnchorPosition.Above
                IconTooltipPosition.Start -> TooltipAnchorPosition.Start
            },
            spacingBetweenTooltipAndAnchor = TooltipGap
        ),
        tooltip = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, TooltipShape)
                    .emberrBorder(TooltipShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        },
        state = rememberTooltipState(),
        content = content
    )
}
