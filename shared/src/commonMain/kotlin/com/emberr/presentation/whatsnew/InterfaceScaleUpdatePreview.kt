package com.emberr.presentation.whatsnew

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emberr.data.local.prefs.SyncConstants
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.settings.InterfaceScaleStepperRow
import com.emberr.presentation.settings.SettingsActionRow
import com.emberr.presentation.settings.SettingsChoiceRow
import com.emberr.presentation.settings.SettingsDivider
import com.emberr.presentation.settings.SettingsGroup
import com.emberr.presentation.settings.desktopSettingsSidePadding
import com.emberr.presentation.settings.label
import com.emberr.ui.theme.ColorThemePreference
import com.emberr.ui.theme.ProvideInterfaceScale
import com.emberr.ui.theme.interfaceScalePercentMovedBySteps
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.palette
import emberr.shared.generated.resources.sliders_horizontal
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.milliseconds
import emberr.shared.generated.resources.settings_font_size
import emberr.shared.generated.resources.settings_font_size_default
import emberr.shared.generated.resources.settings_group_appearance
import emberr.shared.generated.resources.settings_theme
import emberr.shared.generated.resources.settings_top_bar_fade
import emberr.shared.generated.resources.settings_top_bar_fade_blur
import org.jetbrains.compose.resources.stringResource

private val ScaleStepDelay = 300.milliseconds
private val ScalePause = 1300.milliseconds
private val ResetKeyShowTime = 800.milliseconds

@Composable
internal fun InterfaceScaleUpdatePreview() {
    var scalePercent by remember { mutableIntStateOf(SyncConstants.DEFAULT_INTERFACE_SCALE_PERCENT) }
    var shortcutKey by remember { mutableStateOf("+") }
    var isShortcutShown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        suspend fun stepScaleTo(targetPercent: Int) {
            val stepDirection = if (targetPercent > scalePercent) 1 else -1
            shortcutKey = if (stepDirection > 0) "+" else "−"
            isShortcutShown = true
            while (scalePercent != targetPercent) {
                scalePercent = interfaceScalePercentMovedBySteps(scalePercent, stepDirection)
                delay(ScaleStepDelay)
            }
            isShortcutShown = false
        }

        while (true) {
            delay(ScalePause)
            stepScaleTo(130)
            delay(ScalePause)
            stepScaleTo(85)
            delay(ScalePause)
            shortcutKey = "0"
            isShortcutShown = true
            scalePercent = SyncConstants.DEFAULT_INTERFACE_SCALE_PERCENT
            delay(ResetKeyShowTime)
            isShortcutShown = false
        }
    }

    UpdatePreviewFrame {
        ProvideInterfaceScale(interfaceScalePercent = scalePercent) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppearanceSettingsRows(scalePercent = scalePercent)
            }
        }
        if (isDesktopPlatform) {
            UpdatePreviewKeyCaps(
                keys = listOf("Ctrl", shortcutKey),
                isShown = isShortcutShown,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 14.dp)
            )
        }
    }
}

@Composable
private fun AppearanceSettingsRows(scalePercent: Int) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = desktopSettingsSidePadding(maxWidth))
        ) {
            SettingsGroup(title = stringResource(Res.string.settings_group_appearance)) {
                SettingsActionRow(
                    icon = painterResource(Res.drawable.palette),
                    title = stringResource(Res.string.settings_theme),
                    trailingLabel = ColorThemePreference.EMBERR.label(),
                    onClick = {}
                )
                SettingsDivider()
                if (isDesktopPlatform) {
                    SettingsChoiceRow(
                        icon = painterResource(Res.drawable.palette),
                        title = stringResource(Res.string.settings_top_bar_fade),
                        value = stringResource(Res.string.settings_top_bar_fade_blur),
                        choices = emptyList()
                    )
                } else {
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.palette),
                        title = stringResource(Res.string.settings_top_bar_fade),
                        trailingLabel = stringResource(Res.string.settings_top_bar_fade_blur),
                        onClick = {}
                    )
                }
                SettingsDivider()
                InterfaceScaleStepperRow(savedPercent = scalePercent, onPercentChosen = {})
                SettingsDivider()
                SettingsChoiceRow(
                    icon = painterResource(Res.drawable.sliders_horizontal),
                    title = stringResource(Res.string.settings_font_size),
                    value = stringResource(Res.string.settings_font_size_default),
                    choices = emptyList()
                )
            }
        }
    }
}
