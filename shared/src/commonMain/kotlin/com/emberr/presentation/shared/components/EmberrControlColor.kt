package com.emberr.presentation.shared.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.emberr.domain.util.system.isDesktopPlatform

val LocalSitsOnCard = staticCompositionLocalOf { false }

@Composable
internal fun emberrControlContainerColor(): Color =
    if (isDesktopPlatform || LocalSitsOnCard.current) MaterialTheme.colorScheme.surfaceContainerHigh
    else MaterialTheme.colorScheme.surfaceVariant