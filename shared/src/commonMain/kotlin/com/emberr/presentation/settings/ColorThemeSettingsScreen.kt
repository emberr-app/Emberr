package com.emberr.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrCheckboxOption
import com.emberr.presentation.shared.components.EmberrChoiceValueMenu
import com.emberr.presentation.shared.components.EmberrSlider
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.EmberrVerticalScrollbar
import com.emberr.presentation.shared.components.NoRippleIndicationNodeFactory
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.topHeaderBarPadding
import com.emberr.ui.theme.ColorThemePreference
import com.emberr.ui.theme.FULL_STRENGTH_PERCENT
import com.emberr.ui.theme.LocalAppIsDark
import com.emberr.ui.theme.PaperTextureArea
import com.emberr.ui.theme.ThemePreference
import com.emberr.ui.theme.availableColorThemes
import com.emberr.ui.theme.colorThemePreferenceNamed
import com.emberr.ui.theme.isPaperTextureAreaChecked
import com.emberr.ui.theme.paperTextureAreasNamed
import com.emberr.ui.theme.paperTextureAreasSavedValue
import com.emberr.ui.theme.paperTextureAreasWithToggled
import com.emberr.ui.theme.sourceColorSchemes
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.common_back
import emberr.shared.generated.resources.common_close
import emberr.shared.generated.resources.settings_paper_texture
import emberr.shared.generated.resources.settings_paper_texture_hint
import emberr.shared.generated.resources.settings_paper_texture_show_on
import emberr.shared.generated.resources.settings_texture_strength
import emberr.shared.generated.resources.settings_theme
import emberr.shared.generated.resources.settings_theme_group_colors
import emberr.shared.generated.resources.settings_theme_group_strength
import emberr.shared.generated.resources.settings_theme_group_text_and_icons
import emberr.shared.generated.resources.settings_theme_group_texture
import emberr.shared.generated.resources.settings_theme_mode
import emberr.shared.generated.resources.settings_theme_strength
import emberr.shared.generated.resources.settings_theme_text_and_icons
import emberr.shared.generated.resources.settings_theme_text_and_icons_hint
import emberr.shared.generated.resources.check
import emberr.shared.generated.resources.moon_cloud
import emberr.shared.generated.resources.paper_texture
import emberr.shared.generated.resources.show_on
import emberr.shared.generated.resources.sun
import emberr.shared.generated.resources.text_type
import emberr.shared.generated.resources.texture_strength
import emberr.shared.generated.resources.theme_strength
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

private const val DESKTOP_THEME_COLUMNS = 3
private const val MOBILE_THEME_COLUMNS = 2
private const val PREVIEW_ASPECT_RATIO = 1.25f
private val PreviewCardShape = RoundedCornerShape(14.dp)
private val PreviewLineShape = RoundedCornerShape(50)
private val PreviewSurfaceShape = RoundedCornerShape(6.dp)
private val SelectedPreviewBorderWidth = 2.dp
private val ThemeCardSpacing = 14.dp

@Composable
fun ColorThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }
    val colorThemePreferenceName by viewModel.colorThemePreference.collectAsState()
    val selectedColorTheme = colorThemePreferenceNamed(colorThemePreferenceName)
    val themeModeName by viewModel.themePreference.collectAsState()
    val selectedThemeMode = ThemePreference.entries.firstOrNull { it.name == themeModeName } ?: ThemePreference.SYSTEM
    val themedTextAndIcons by viewModel.themedTextAndIcons.collectAsState()
    val colorThemeStrength by viewModel.colorThemeStrength.collectAsState()
    val paperTextureEnabled by viewModel.paperTextureEnabled.collectAsState()
    val paperTextureStrength by viewModel.paperTextureStrength.collectAsState()
    val savedPaperTextureAreas by viewModel.paperTextureAreas.collectAsState()
    val paperTextureAreas = paperTextureAreasNamed(savedPaperTextureAreas)
    val columnCount = if (isDesktopPlatform) DESKTOP_THEME_COLUMNS else MOBILE_THEME_COLUMNS
    val colorThemes = availableColorThemes()
    val colorThemeRows = remember(columnCount, colorThemes) { colorThemes.chunked(columnCount) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sidePadding = desktopSettingsSidePadding(maxWidth)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = sidePadding,
                top = topBarHeightDp,
                end = sidePadding,
                bottom = 48.dp
            )
        ) {
            item(key = "mode") {
                SettingsGroup(title = stringResource(Res.string.settings_theme_mode)) {
                    SettingsChoiceRow(
                        icon = painterResource(if (LocalAppIsDark.current) Res.drawable.moon_cloud else Res.drawable.sun),
                        title = stringResource(Res.string.settings_theme_mode),
                        value = selectedThemeMode.label(),
                        choices = ThemePreference.entries.map { option ->
                            SettingsChoice(label = option.label(), isSelected = option == selectedThemeMode) {
                                viewModel.setThemePreference(option.name)
                            }
                        }
                    )
                }
            }

            if (selectedColorTheme != ColorThemePreference.EMBERR) {
                item(key = "strength") {
                    SettingsGroup(title = stringResource(Res.string.settings_theme_group_strength)) {
                        PercentSliderRow(
                            icon = painterResource(Res.drawable.theme_strength),
                            title = stringResource(Res.string.settings_theme_strength),
                            percent = colorThemeStrength,
                            onPercentChange = { newStrength ->
                                if (newStrength != colorThemeStrength) viewModel.setColorThemeStrength(newStrength)
                            }
                        )
                    }
                }
            }

            item(key = "text_and_icons") {
                SettingsGroup(title = stringResource(Res.string.settings_theme_group_text_and_icons)) {
                    SettingsToggleRow(
                        icon = painterResource(Res.drawable.text_type),
                        title = stringResource(Res.string.settings_theme_text_and_icons),
                        isChecked = themedTextAndIcons,
                        subtitle = stringResource(Res.string.settings_theme_text_and_icons_hint),
                        onCheckedChange = { viewModel.setThemedTextAndIcons(it) }
                    )
                }
            }

            item(key = "texture") {
                SettingsGroup(title = stringResource(Res.string.settings_theme_group_texture)) {
                    SettingsToggleRow(
                        icon = painterResource(Res.drawable.paper_texture),
                        title = stringResource(Res.string.settings_paper_texture),
                        isChecked = paperTextureEnabled,
                        subtitle = stringResource(Res.string.settings_paper_texture_hint),
                        onCheckedChange = { viewModel.setPaperTextureEnabled(it) }
                    )
                    if (paperTextureEnabled) {
                        SettingsDivider()
                        PaperTextureAreasRow(
                            areas = paperTextureAreas,
                            onToggleArea = { tappedArea ->
                                viewModel.setPaperTextureAreas(
                                    paperTextureAreasSavedValue(paperTextureAreasWithToggled(paperTextureAreas, tappedArea))
                                )
                            }
                        )
                        SettingsDivider()
                        PercentSliderRow(
                            icon = painterResource(Res.drawable.texture_strength),
                            title = stringResource(Res.string.settings_texture_strength),
                            percent = paperTextureStrength,
                            onPercentChange = { newStrength ->
                                if (newStrength != paperTextureStrength) viewModel.setPaperTextureStrength(newStrength)
                            }
                        )
                    }
                }
            }

            item(key = "colors_title") {
                Box(modifier = Modifier.padding(start = 16.dp, top = 22.dp, end = 16.dp)) {
                    SettingsGroupTitle(title = stringResource(Res.string.settings_theme_group_colors))
                }
            }

            items(colorThemeRows, key = { row -> row.first().name }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = ThemeCardSpacing, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(ThemeCardSpacing)
                ) {
                    row.forEach { colorTheme ->
                        Box(modifier = Modifier.weight(1f)) {
                            ColorThemePreviewCard(
                                colorTheme = colorTheme,
                                themedTextAndIcons = themedTextAndIcons,
                                strengthPercent = colorThemeStrength,
                                isSelected = colorTheme == selectedColorTheme,
                                onSelect = { viewModel.setColorThemePreference(colorTheme.name) }
                            )
                        }
                    }
                    repeat(columnCount - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        EmberrVerticalScrollbar(
            listState = listState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = topBarHeightDp, bottom = 8.dp)
        )

        if (showTopBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(10f)
                    .onGloballyPositioned { coordinates -> topBarHeightPx = coordinates.size.height.toFloat() }
            ) {
                EmberrTopHeaderBar(
                    title = stringResource(Res.string.settings_theme),
                    hazeState = hazeState,
                    contentPadding = topHeaderBarPadding(bottom = 16.dp),
                    backTooltip = stringResource(Res.string.common_back),
                    onBackClick = onNavigateBack
                )
            }
        }
    }
}

@Composable
internal fun PaperTextureAreasRow(
    areas: Set<PaperTextureArea>,
    onToggleArea: (PaperTextureArea) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val areaOptions: @Composable () -> Unit = {
        PaperTextureArea.entries.forEach { area ->
            EmberrCheckboxOption(
                label = area.label(),
                isChecked = isPaperTextureAreaChecked(areas, area),
                onClick = { onToggleArea(area) }
            )
        }
    }

    if (isDesktopPlatform) {
        Row(modifier = Modifier.fillMaxWidth().settingsRowSize(), verticalAlignment = Alignment.CenterVertically) {
            SettingsRowIcon(icon = painterResource(Res.drawable.show_on))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(Res.string.settings_paper_texture_show_on),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            EmberrChoiceValueMenu(
                value = paperTextureAreasLabel(areas),
                isMenuOpen = isMenuOpen,
                onMenuOpenChange = { isOpen -> isMenuOpen = isOpen }
            ) { areaOptions() }
        }
    } else {
        SettingsActionRow(
            icon = painterResource(Res.drawable.show_on),
            title = stringResource(Res.string.settings_paper_texture_show_on),
            trailingLabel = paperTextureAreasLabel(areas),
            onClick = { isMenuOpen = true }
        )
        EmberrBottomSheet(
            expanded = isMenuOpen,
            onDismiss = { isMenuOpen = false },
            title = stringResource(Res.string.settings_paper_texture_show_on),
            contentHorizontalPadding = 0.dp
        ) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                areaOptions()
                EmberrButtonPrimary(
                    text = stringResource(Res.string.common_close),
                    onClick = { closeAnd {} },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                )
            }
        }
    }
}

@Composable
internal fun PercentSliderRow(
    icon: Painter,
    title: String,
    percent: Int,
    onPercentChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().settingsRowSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingsRowIcon(icon = icon)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        EmberrSlider(
            value = percent.toFloat(),
            onValueChange = { newValue -> onPercentChange(newValue.roundToInt()) },
            valueRange = 0f..FULL_STRENGTH_PERCENT.toFloat()
        )
    }
}

@Composable
private fun ColorThemePreviewCard(
    colorTheme: ColorThemePreference,
    themedTextAndIcons: Boolean,
    strengthPercent: Int,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val sourceColorSchemes = colorTheme.sourceColorSchemes()
    val previewBorderModifier = if (isSelected) {
        Modifier.border(SelectedPreviewBorderWidth, MaterialTheme.colorScheme.primary, PreviewCardShape)
    } else {
        Modifier.emberrBorder(PreviewCardShape)
    }

    Column(
        modifier = Modifier
            .clip(PreviewCardShape)
            .selectable(
                selected = isSelected,
                interactionSource = remember { MutableInteractionSource() },
                indication = NoRippleIndicationNodeFactory,
                role = Role.RadioButton,
                onClick = onSelect
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PREVIEW_ASPECT_RATIO)
                .clip(PreviewCardShape)
                .then(previewBorderModifier)
        ) {
            ColorThemePreviewHalf(
                colors = colorTheme.colorSchemeFor(
                    darkTheme = false,
                    themedTextAndIcons = themedTextAndIcons,
                    strengthPercent = strengthPercent,
                    sourceColorSchemes = sourceColorSchemes
                ),
                modifier = Modifier.weight(1f)
            )
            ColorThemePreviewHalf(
                colors = colorTheme.colorSchemeFor(
                    darkTheme = true,
                    themedTextAndIcons = themedTextAndIcons,
                    strengthPercent = strengthPercent,
                    sourceColorSchemes = sourceColorSchemes
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 8.dp, end = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = colorTheme.label(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    painter = painterResource(Res.drawable.check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorThemePreviewHalf(colors: ColorScheme, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colors.background)
            .padding(10.dp)
    ) {
        PreviewLine(color = colors.onBackground, widthFraction = 0.7f, height = 6.dp)
        Spacer(modifier = Modifier.height(8.dp))
        PreviewLine(color = colors.onBackground.copy(alpha = 0.35f), widthFraction = 0.9f, height = 4.dp)
        Spacer(modifier = Modifier.height(4.dp))
        PreviewLine(color = colors.onBackground.copy(alpha = 0.35f), widthFraction = 0.6f, height = 4.dp)
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(PreviewSurfaceShape)
                .background(colors.surface)
                .padding(6.dp)
        ) {
            PreviewLine(color = colors.onSurface.copy(alpha = 0.5f), widthFraction = 0.8f, height = 4.dp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(8.dp)
                .clip(PreviewLineShape)
                .background(colors.secondary)
        )
    }
}

@Composable
private fun PreviewLine(color: Color, widthFraction: Float, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(PreviewLineShape)
            .background(color)
    )
}
