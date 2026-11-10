package com.emberr.presentation.shared.components

import androidx.compose.runtime.Composable
import com.emberr.ui.theme.HighlightColor
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.common_color_blue
import emberr.shared.generated.resources.common_color_green
import emberr.shared.generated.resources.common_color_grey
import emberr.shared.generated.resources.common_color_lime
import emberr.shared.generated.resources.common_color_orange
import emberr.shared.generated.resources.common_color_pink
import emberr.shared.generated.resources.common_color_purple
import emberr.shared.generated.resources.common_color_red
import emberr.shared.generated.resources.common_color_yellow
import org.jetbrains.compose.resources.stringResource

@Composable
fun HighlightColor.label(): String = stringResource(
    when (this) {
        HighlightColor.YELLOW -> Res.string.common_color_yellow
        HighlightColor.LIME -> Res.string.common_color_lime
        HighlightColor.GREY -> Res.string.common_color_grey
        HighlightColor.RED -> Res.string.common_color_red
        HighlightColor.BLUE -> Res.string.common_color_blue
        HighlightColor.GREEN -> Res.string.common_color_green
        HighlightColor.ORANGE -> Res.string.common_color_orange
        HighlightColor.PINK -> Res.string.common_color_pink
        HighlightColor.PURPLE -> Res.string.common_color_purple
    }
)
