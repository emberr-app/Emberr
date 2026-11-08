package com.emberr.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

const val SMALLEST_INTERFACE_SCALE_PERCENT = 80
const val LARGEST_INTERFACE_SCALE_PERCENT = 200
private const val INTERFACE_SCALE_STEP_PERCENT = 5

private fun clampedInterfaceScalePercent(percent: Int): Int =
    percent.coerceIn(SMALLEST_INTERFACE_SCALE_PERCENT, LARGEST_INTERFACE_SCALE_PERCENT)

fun interfaceScalePercentMovedBySteps(percent: Int, stepCount: Int): Int =
    clampedInterfaceScalePercent(percent + stepCount * INTERFACE_SCALE_STEP_PERCENT)

@Composable
fun ProvideInterfaceScale(interfaceScalePercent: Int, content: @Composable () -> Unit) {
    val baseDensity = LocalDensity.current
    val scaleFactor = clampedInterfaceScalePercent(interfaceScalePercent) / 100f
    val scaledDensity = remember(baseDensity, scaleFactor) {
        Density(baseDensity.density * scaleFactor, baseDensity.fontScale)
    }
    CompositionLocalProvider(LocalDensity provides scaledDensity, content = content)
}
