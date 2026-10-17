package com.emberr.presentation.shared.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

private const val TOOLTIP_DELAY_MILLIS = 400
private val TooltipGap = 6.dp
private val TooltipShape = RoundedCornerShape(50)

@OptIn(ExperimentalFoundationApi::class)
private val BelowPlacement = TooltipPlacement.ComponentRect(
    anchor = Alignment.BottomCenter,
    alignment = Alignment.BottomCenter,
    offset = DpOffset(0.dp, TooltipGap)
)

@OptIn(ExperimentalFoundationApi::class)
private val AbovePlacement = TooltipPlacement.ComponentRect(
    anchor = Alignment.TopCenter,
    alignment = Alignment.TopCenter,
    offset = DpOffset(0.dp, -TooltipGap)
)

@OptIn(ExperimentalFoundationApi::class)
private val StartPlacement = TooltipPlacement.ComponentRect(
    anchor = Alignment.CenterStart,
    alignment = Alignment.CenterStart,
    offset = DpOffset(-TooltipGap, 0.dp)
)

@OptIn(ExperimentalFoundationApi::class)
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
    TooltipArea(
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
        delayMillis = TOOLTIP_DELAY_MILLIS,
        tooltipPlacement = when (position) {
            IconTooltipPosition.Below -> BelowPlacement
            IconTooltipPosition.Above -> AbovePlacement
            IconTooltipPosition.Start -> StartPlacement
        },
        content = content
    )
}
