package com.emberr.presentation.shared.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.sidebar
import org.jetbrains.compose.resources.painterResource

val LocalDesktopSidebarToggle = compositionLocalOf<(() -> Unit)?> { null }

val DesktopSidebarToggleGap = 12.dp

@Composable
fun DesktopSidebarToggleButton(
    onClick: () -> Unit,
    hazeState: HazeState?,
    hazeStyle: HazeStyle = EmberrBlur.Regular,
    tint: Color = MaterialTheme.colorScheme.primary,
    bgColor: Color = Color.Transparent
) {
    val sidebarIcon = painterResource(Res.drawable.sidebar)
    TopBarIconButton(
        icon = remember(sidebarIcon) { sidebarIcon.flippedHorizontally() },
        contentDescription = "Show sidebar",
        bgColor = bgColor,
        tint = tint,
        hazeState = hazeState,
        hazeStyle = hazeStyle,
        size = TopHeaderBarButtonSize,
        tooltip = "Show sidebar",
        onClick = onClick
    )
}
