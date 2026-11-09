package com.emberr.presentation.shared.components.updatepreviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emberr.presentation.settings.PaperTextureAreasRow
import com.emberr.presentation.settings.PercentSliderRow
import com.emberr.presentation.settings.SettingsDivider
import com.emberr.presentation.settings.SettingsGroup
import com.emberr.presentation.settings.SettingsToggleRow
import com.emberr.presentation.settings.desktopSettingsSidePadding
import com.emberr.ui.theme.LocalPaperTextureAreas
import com.emberr.ui.theme.LocalPaperTextureStrength
import com.emberr.ui.theme.PaperTextureArea
import com.emberr.ui.theme.PaperTextureBox
import com.emberr.ui.theme.SeparatePaperGrainBox
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.paper_texture
import emberr.shared.generated.resources.texture_strength
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private val WholeAppOnly = setOf(PaperTextureArea.WHOLE_APP)

@Composable
internal fun PaperTextureUpdatePreview() {
    var isTextureOn by remember { mutableStateOf(false) }
    var textureStrength by remember { mutableIntStateOf(40) }

    LaunchedEffect(Unit) {
        while (true) {
            isTextureOn = false
            textureStrength = 40
            delay(1500)
            isTextureOn = true
            delay(1650)
            textureStrength = 90
            delay(1000)
            textureStrength = 15
            delay(1100)
            textureStrength = 60
            delay(2600)
        }
    }

    val shownTextureStrength by animateFloatAsState(
        targetValue = if (isTextureOn) textureStrength.toFloat() else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "texture_preview_strength"
    )

    UpdatePreviewFrame {
        CompositionLocalProvider(LocalPaperTextureStrength provides 0) {
            SeparatePaperGrainBox(shape = UpdatePreviewShape, modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalPaperTextureStrength provides shownTextureStrength.roundToInt(),
                    LocalPaperTextureAreas provides WholeAppOnly
                ) {
                    PaperTextureBox(modifier = Modifier.fillMaxSize()) {
                        TextureSettingsGroup(isTextureOn = isTextureOn, textureStrength = textureStrength)
                    }
                }
            }
        }
    }
}

@Composable
private fun TextureSettingsGroup(isTextureOn: Boolean, textureStrength: Int) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = desktopSettingsSidePadding(maxWidth))
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.Center
        ) {
            SettingsGroup(title = "Texture") {
                SettingsToggleRow(
                    icon = painterResource(Res.drawable.paper_texture),
                    title = "Paper Texture",
                    isChecked = isTextureOn,
                    subtitle = "Adds a soft grain, like paper.",
                    onCheckedChange = {}
                )
                AnimatedVisibility(
                    visible = isTextureOn,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        SettingsDivider()
                        PaperTextureAreasRow(areas = WholeAppOnly, onToggleArea = {})
                        SettingsDivider()
                        PercentSliderRow(
                            icon = painterResource(Res.drawable.texture_strength),
                            title = "Texture Strength",
                            percent = textureStrength,
                            onPercentChange = {}
                        )
                    }
                }
            }
        }
    }
}
