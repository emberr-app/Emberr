package com.emberr.presentation.shared.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.emberr.data.local.prefs.SettingsManager
import org.koin.compose.koinInject

class PaperTextureToggle(val isOn: Boolean, val toggle: () -> Unit)

@Composable
fun collectPaperTextureToggle(): PaperTextureToggle {
    val settingsManager: SettingsManager = koinInject()
    val isPaperTextureOn by settingsManager.paperTextureEnabledFlow.collectAsState(
        initial = settingsManager.isPaperTextureEnabled()
    )
    return PaperTextureToggle(isOn = isPaperTextureOn) { settingsManager.savePaperTextureEnabled(!isPaperTextureOn) }
}
