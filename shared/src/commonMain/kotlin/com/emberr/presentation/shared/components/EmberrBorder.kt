package com.emberr.presentation.shared.components

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.ui.theme.ColorThemePreference
import com.emberr.ui.theme.LocalAppIsDark
import com.emberr.ui.theme.LocalColorThemePreference

private val NormalBorderWidth = 0.5.dp
private val HighContrastBorderWidth = 1.dp

private val isHighContrastTheme: Boolean
    @Composable get() = LocalColorThemePreference.current == ColorThemePreference.HIGH_CONTRAST

val emberrBorderWidth: Dp
    @Composable get() = if (isHighContrastTheme) HighContrastBorderWidth else NormalBorderWidth

val emberrBorderColor: Color
    @Composable get() =
        when {
            isHighContrastTheme -> MaterialTheme.colorScheme.outlineVariant
            !LocalAppIsDark.current -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            isDesktopPlatform -> Color.White.copy(alpha = 0.15f)
            else -> Color.White.copy(alpha = 0.12f)
        }

val emberrSoftBorderColor: Color
    @Composable get() = emberrBorderColor.copy(alpha = emberrBorderColor.alpha * 0.6f)

@Composable
fun Modifier.emberrBorder(shape: Shape, color: Color = emberrBorderColor): Modifier =
    border(width = emberrBorderWidth, color = color, shape = shape)

@Composable
fun Color.orHighContrastLine(): Color =
    if (isHighContrastTheme) MaterialTheme.colorScheme.outlineVariant else this
