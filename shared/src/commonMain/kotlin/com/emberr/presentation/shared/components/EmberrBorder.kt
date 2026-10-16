package com.emberr.presentation.shared.components

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.ui.theme.LocalAppIsDark

val EmberrBorderWidth = 0.5.dp

val emberrBorderColor: Color
    @Composable get() =
        when {
            !LocalAppIsDark.current -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            isDesktopPlatform -> Color.White.copy(alpha = 0.15f)
            else -> Color.White.copy(alpha = 0.12f)
        }

val emberrSoftBorderColor: Color
    @Composable get() = emberrBorderColor.copy(alpha = emberrBorderColor.alpha * 0.6f)

@Composable
fun Modifier.emberrBorder(shape: Shape, color: Color = emberrBorderColor): Modifier =
    border(width = EmberrBorderWidth, color = color, shape = shape)
