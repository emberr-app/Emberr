package com.emberr.presentation.whatsnew

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import com.emberr.data.local.prefs.SyncConstants
import com.emberr.domain.util.language.AppLanguage
import com.emberr.presentation.settings.InterfaceScaleStepperRow
import com.emberr.presentation.settings.SettingsActionRow
import com.emberr.presentation.settings.SettingsChoiceRow
import com.emberr.presentation.settings.SettingsDivider
import com.emberr.presentation.settings.SettingsGroup
import com.emberr.presentation.settings.desktopSettingsSidePadding
import com.emberr.presentation.settings.label
import com.emberr.presentation.shared.LanguageNameRollLayer
import com.emberr.presentation.shared.LanguageNameRollState
import com.emberr.ui.theme.ColorThemePreference
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.globe
import emberr.shared.generated.resources.palette
import emberr.shared.generated.resources.settings_font_size
import emberr.shared.generated.resources.settings_font_size_default
import emberr.shared.generated.resources.settings_group_appearance
import emberr.shared.generated.resources.settings_language
import emberr.shared.generated.resources.settings_theme
import emberr.shared.generated.resources.sliders_horizontal
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

private val LanguagePause = 1600.milliseconds
private const val NAMES_ROLLED_PAST = 8
private const val NAMES_SHOWN_BELOW = 2
private val PreviewLanguageOrder = listOf(
    AppLanguage.ENGLISH,
    AppLanguage.JAPANESE,
    AppLanguage.SPANISH,
    AppLanguage.HINDI,
    AppLanguage.GERMAN,
    AppLanguage.KOREAN
)

@Composable
internal fun LanguagesUpdatePreview() {
    val rollState = remember { LanguageNameRollState() }
    var shownLanguage by remember { mutableStateOf(PreviewLanguageOrder.first()) }

    LaunchedEffect(Unit) {
        var nextLanguageIndex = 1
        while (true) {
            delay(LanguagePause)
            val chosenLanguage = PreviewLanguageOrder[nextLanguageIndex]
            val otherLanguageNames = AppLanguage.entries
                .filter { other -> other != AppLanguage.SYSTEM && other != shownLanguage && other != chosenLanguage }
                .mapNotNull { other -> other.nativeName }
            rollState.blurAndRollTo(
                chosenLanguageName = chosenLanguage.nativeName.orEmpty(),
                namesRolledPast = listOfNotNull(shownLanguage.nativeName) + otherLanguageNames.take(NAMES_ROLLED_PAST),
                namesBelow = otherLanguageNames.drop(NAMES_ROLLED_PAST).take(NAMES_SHOWN_BELOW)
            )
            shownLanguage = chosenLanguage
            withFrameNanos { }
            rollState.sharpen()
            nextLanguageIndex = (nextLanguageIndex + 1) % PreviewLanguageOrder.size
        }
    }

    UpdatePreviewFrame {
        LanguageNameRollLayer(rollState = rollState) {
            AppearanceSettingsRows(shownLanguage = shownLanguage)
        }
    }
}

@Composable
private fun AppearanceSettingsRows(shownLanguage: AppLanguage) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = desktopSettingsSidePadding(maxWidth))
        ) {
            SettingsGroup(title = stringResource(Res.string.settings_group_appearance)) {
                SettingsChoiceRow(
                    icon = painterResource(Res.drawable.globe),
                    title = stringResource(Res.string.settings_language),
                    value = shownLanguage.label(),
                    choices = emptyList()
                )
                SettingsDivider()
                SettingsActionRow(
                    icon = painterResource(Res.drawable.palette),
                    title = stringResource(Res.string.settings_theme),
                    trailingLabel = ColorThemePreference.EMBERR.label(),
                    onClick = {}
                )
                SettingsDivider()
                InterfaceScaleStepperRow(savedPercent = SyncConstants.DEFAULT_INTERFACE_SCALE_PERCENT, onPercentChosen = {})
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
