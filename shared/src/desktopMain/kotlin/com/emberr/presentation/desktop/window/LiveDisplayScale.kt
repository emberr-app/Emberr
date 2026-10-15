package com.emberr.presentation.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import java.awt.Toolkit
import java.beans.PropertyChangeListener

private const val GnomeWindowScaleProperty = "gnome.Gdk/WindowScalingFactor"

@Composable
fun FollowLiveDisplayScale(windowState: WindowState, content: @Composable () -> Unit) {
    val toolkit = remember { Toolkit.getDefaultToolkit() }
    val startupScale = remember { toolkit.gnomeWindowScale() }
    var currentScale by remember { mutableStateOf(startupScale) }

    DisposableEffect(toolkit) {
        val listener = PropertyChangeListener { currentScale = toolkit.gnomeWindowScale() }
        toolkit.addPropertyChangeListener(GnomeWindowScaleProperty, listener)
        onDispose { toolkit.removePropertyChangeListener(GnomeWindowScaleProperty, listener) }
    }

    val liveScale = currentScale
    val scaleChangeSinceStartup =
        if (startupScale != null && liveScale != null) liveScale.toFloat() / startupScale else 1f

    var windowSizedForScaleChange by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(scaleChangeSinceStartup) {
        val resizeFactor = scaleChangeSinceStartup / windowSizedForScaleChange
        if (windowState.placement == WindowPlacement.Floating && resizeFactor != 1f) {
            windowState.size = DpSize(windowState.size.width * resizeFactor, windowState.size.height * resizeFactor)
        }
        windowSizedForScaleChange = scaleChangeSinceStartup
    }

    val windowDensity = LocalDensity.current
    @Suppress("RestrictedApi")
    val windowFontScale = windowDensity.fontScale
    val liveDensity = remember(windowDensity, scaleChangeSinceStartup) {
        Density(windowDensity.density * scaleChangeSinceStartup, windowFontScale)
    }
    CompositionLocalProvider(LocalDensity provides liveDensity, content = content)
}

private fun Toolkit.gnomeWindowScale(): Int? =
    (getDesktopProperty(GnomeWindowScaleProperty) as? Int)?.takeIf { it > 0 }
