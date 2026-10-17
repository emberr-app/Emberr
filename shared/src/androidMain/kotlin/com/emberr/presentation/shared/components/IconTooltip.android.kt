package com.emberr.presentation.shared.components

import androidx.compose.runtime.Composable

@Composable
actual fun IconTooltip(
    text: String?,
    position: IconTooltipPosition,
    content: @Composable () -> Unit
) {
    content()
}
