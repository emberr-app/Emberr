package com.emberr.presentation.whatsnew

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.emberrBorder
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_left
import emberr.shared.generated.resources.ellipsis
import org.jetbrains.compose.resources.painterResource
import emberr.shared.generated.resources.common_back
import emberr.shared.generated.resources.whatsnew_preview_options
import org.jetbrains.compose.resources.stringResource

internal val UpdatePreviewShape = RoundedCornerShape(22.dp)
internal val UpdatePreviewNoteTopPadding = 20.dp
private val DesktopPreviewDrawnWidth = 960.dp
private val MobilePreviewDrawnWidth = 360.dp
private const val DESKTOP_PREVIEW_WIDTH_TO_HEIGHT_RATIO = 2f
private const val MOBILE_PREVIEW_WIDTH_TO_HEIGHT_RATIO = 3f / 4f
private val KeyCapShape = RoundedCornerShape(6.dp)

@Composable
internal fun UpdatePreviewFrame(
    fitsContentHeight: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val drawnWidth = if (isDesktopPlatform) DesktopPreviewDrawnWidth else MobilePreviewDrawnWidth
    val widthToHeightRatio = if (isDesktopPlatform) DESKTOP_PREVIEW_WIDTH_TO_HEIGHT_RATIO else MOBILE_PREVIEW_WIDTH_TO_HEIGHT_RATIO

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val baseDensity = LocalDensity.current
        val drawingScale = maxWidth / drawnWidth
        val scaledDensity = remember(baseDensity, drawingScale) {
            Density(baseDensity.density * drawingScale, baseDensity.fontScale)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fitsContentHeight) Modifier else Modifier.aspectRatio(widthToHeightRatio))
                .clip(UpdatePreviewShape)
                .background(MaterialTheme.colorScheme.background)
                .emberrBorder(UpdatePreviewShape)
                .clearAndSetSemantics {}
        ) {
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (fitsContentHeight) Modifier else Modifier.fillMaxHeight()),
                    content = content
                )
            }
            TouchBlocker()
        }
    }
}

@Composable
private fun BoxScope.TouchBlocker() {
    Box(modifier = Modifier.matchParentSize().pointerInput(Unit) {})
}

@Composable
internal fun UpdatePreviewNoteTopButtons(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TopBarIconButton(
            icon = painterResource(Res.drawable.chevron_left),
            contentDescription = stringResource(Res.string.common_back),
            bgColor = MaterialTheme.colorScheme.background,
            tint = MaterialTheme.colorScheme.primary,
            onClick = {}
        )
        TopBarIconButton(
            icon = painterResource(Res.drawable.ellipsis),
            contentDescription = stringResource(Res.string.whatsnew_preview_options),
            bgColor = MaterialTheme.colorScheme.background,
            tint = MaterialTheme.colorScheme.primary,
            onClick = {}
        )
    }
}

@Composable
internal fun UpdatePreviewKeyCaps(keys: List<String>, isShown: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = isShown,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = modifier
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            keys.forEach { key ->
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(KeyCapShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .emberrBorder(KeyCapShape)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
}
