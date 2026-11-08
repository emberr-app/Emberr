package com.emberr.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.emberr.domain.util.system.showNativeToast

const val SMALLEST_INTERFACE_SCALE_PERCENT = 80
const val LARGEST_INTERFACE_SCALE_PERCENT = 200
const val INTERFACE_SCALE_STEP_PERCENT = 5

fun interfaceScalePercentWithin(percent: Int): Int =
    percent.coerceIn(SMALLEST_INTERFACE_SCALE_PERCENT, LARGEST_INTERFACE_SCALE_PERCENT)

fun interfaceScalePercentMovedBySteps(percent: Int, stepCount: Int): Int =
    interfaceScalePercentWithin(percent + stepCount * INTERFACE_SCALE_STEP_PERCENT)

fun showInterfaceScaleToast(percent: Int) {
    showNativeToast("Interface scale $percent%")
}

@Composable
fun ScaleInterface(interfaceScalePercent: Int, content: @Composable () -> Unit) {
    val baseDensity = LocalDensity.current
    val scaleFactor = interfaceScalePercentWithin(interfaceScalePercent) / 100f
    val scaledDensity = remember(baseDensity, scaleFactor) {
        Density(baseDensity.density * scaleFactor, baseDensity.fontScale)
    }
    CompositionLocalProvider(LocalDensity provides scaledDensity, content = content)
}
