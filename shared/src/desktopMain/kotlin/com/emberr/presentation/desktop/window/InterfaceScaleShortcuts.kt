package com.emberr.presentation.desktop.window

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.prefs.SyncConstants
import com.emberr.presentation.settings.showInterfaceScaleToast
import com.emberr.ui.theme.interfaceScalePercentMovedBySteps

private val ScaleUpKeys = setOf(Key.Equals, Key.Plus, Key.NumPadAdd)
private val ScaleDownKeys = setOf(Key.Minus, Key.NumPadSubtract)
private val ScaleResetKeys = setOf(Key.Zero, Key.NumPad0)

fun handleInterfaceScaleShortcut(event: KeyEvent, settingsManager: SettingsManager): Boolean {
    val isScaleShortcutModifier = (event.isCtrlPressed || event.isMetaPressed) && !event.isAltPressed
    if (event.type != KeyEventType.KeyDown || !isScaleShortcutModifier) return false
    val currentPercent = settingsManager.getInterfaceScalePercent()
    val newPercent = when (event.key) {
        in ScaleUpKeys -> interfaceScalePercentMovedBySteps(currentPercent, 1)
        in ScaleDownKeys -> interfaceScalePercentMovedBySteps(currentPercent, -1)
        in ScaleResetKeys -> SyncConstants.DEFAULT_INTERFACE_SCALE_PERCENT
        else -> return false
    }
    if (newPercent != currentPercent) {
        settingsManager.saveInterfaceScalePercent(newPercent)
        showInterfaceScaleToast(newPercent)
    }
    return true
}
