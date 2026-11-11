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
import com.emberr.domain.util.language.AppLanguage
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
import emberr.shared.generated.resources.clock_circle
import emberr.shared.generated.resources.globe
import emberr.shared.generated.resources.palette
import emberr.shared.generated.resources.sliders_horizontal
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
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

private class PreviewRowWords(
    val appearance: String,
    val language: String,
    val theme: String,
    val timeFormat: String,
    val timeFormatSystem: String,
    val fontSize: String,
    val fontSizeDefault: String
)

private val PreviewRowWordsByLanguage = mapOf(
    AppLanguage.ENGLISH to PreviewRowWords("Appearance", "Language", "Theme", "Time Format", "System default", "Font Size", "Default"),
    AppLanguage.JAPANESE to PreviewRowWords("外観", "言語", "テーマ", "時刻の形式", "システムのデフォルト", "文字サイズ", "標準"),
    AppLanguage.SPANISH to PreviewRowWords("Apariencia", "Idioma", "Tema", "Formato de hora", "Predeterminado del sistema", "Tamaño de letra", "Predeterminado"),
    AppLanguage.HINDI to PreviewRowWords("रूप-रंग", "भाषा", "थीम", "समय का फ़ॉर्मैट", "सिस्टम डिफ़ॉल्ट", "फ़ॉन्ट साइज़", "डिफ़ॉल्ट"),
    AppLanguage.GERMAN to PreviewRowWords("Darstellung", "Sprache", "Design", "Zeitformat", "Systemstandard", "Schriftgröße", "Standard"),
    AppLanguage.KOREAN to PreviewRowWords("모양", "언어", "테마", "시간 형식", "시스템 기본값", "글자 크기", "기본")
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
    val rowWords = PreviewRowWordsByLanguage.getValue(shownLanguage)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = desktopSettingsSidePadding(maxWidth))
        ) {
            SettingsGroup(title = rowWords.appearance) {
                SettingsChoiceRow(
                    icon = painterResource(Res.drawable.globe),
                    title = rowWords.language,
                    value = shownLanguage.label(),
                    choices = emptyList()
                )
                SettingsDivider()
                SettingsActionRow(
                    icon = painterResource(Res.drawable.palette),
                    title = rowWords.theme,
                    trailingLabel = ColorThemePreference.EMBERR.label(),
                    onClick = {}
                )
                SettingsDivider()
                SettingsChoiceRow(
                    icon = painterResource(Res.drawable.clock_circle),
                    title = rowWords.timeFormat,
                    value = rowWords.timeFormatSystem,
                    choices = emptyList()
                )
                SettingsDivider()
                SettingsChoiceRow(
                    icon = painterResource(Res.drawable.sliders_horizontal),
                    title = rowWords.fontSize,
                    value = rowWords.fontSizeDefault,
                    choices = emptyList()
                )
            }
        }
    }
}
