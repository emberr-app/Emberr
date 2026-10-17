package com.emberr.presentation.shared.components

import androidx.compose.runtime.Composable

enum class IconTooltipPosition { Below, Above, Start }

@Composable
expect fun IconTooltip(
    text: String?,
    position: IconTooltipPosition = IconTooltipPosition.Below,
    content: @Composable () -> Unit
)
