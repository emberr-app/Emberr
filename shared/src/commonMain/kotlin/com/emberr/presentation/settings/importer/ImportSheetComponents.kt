package com.emberr.presentation.settings.importer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.upload2
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

internal val ImportSheetContentPadding = 20.dp
internal val ImportChooseBoxShape = RoundedCornerShape(14.dp)
private val ChooseBoxMinHeight = 132.dp
private val ChooseBoxCornerRadius = 14.dp
private val ChooseBoxBorderWidth = 1.5.dp
private val ChooseBoxDashLength = 8.dp
private val ChooseBoxDashGap = 6.dp
private const val CHOOSE_BOX_BORDER_ALPHA = 0.25f
private const val UNAVAILABLE_OPTION_ALPHA = 0.38f
private const val FADED_TEXT_ALPHA = 0.6f
private const val PROGRESS_TRACK_ALPHA = 0.12f

@Composable
internal fun importChooseBorderColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = CHOOSE_BOX_BORDER_ALPHA)

internal fun Modifier.importDashedBorder(color: Color): Modifier =
    drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(ChooseBoxCornerRadius.toPx()),
            style = Stroke(
                width = ChooseBoxBorderWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(ChooseBoxDashLength.toPx(), ChooseBoxDashGap.toPx()))
            )
        )
    }

@Composable
internal fun ImportChooseBox(
    isClickable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ChooseBoxMinHeight)
            .importDashedBorder(importChooseBorderColor())
            .clip(ImportChooseBoxShape)
            .clickable(enabled = isClickable, onClick = onClick)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
internal fun ImportChoosePrompt(title: String, hint: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            painter = painterResource(Res.drawable.upload2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = FADED_TEXT_ALPHA),
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        ImportFadedLine(text = hint)
    }
}

@Composable
internal fun ImportReadingMessage(text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        ImportFadedLine(text = text)
    }
}

@Composable
internal fun ImportPickedSummary(pickedLabel: String, countsLine: String, extraLines: List<String>, changeHint: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = pickedLabel,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = countsLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        extraLines.forEach { line -> ImportFadedLine(text = line, maxLines = 2) }
        ImportFadedLine(text = changeHint)
    }
}

@Composable
internal fun ImportFadedLine(text: String, maxLines: Int = Int.MAX_VALUE) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = FADED_TEXT_ALPHA),
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun ImportMenuOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
    isAvailable: Boolean = true
) {
    val availabilityModifier = if (isAvailable) Modifier else Modifier.alpha(UNAVAILABLE_OPTION_ALPHA)
    if (isDesktopPlatform) {
        EmberrDesktopMenuOption(
            label = label,
            subtitle = subtitle,
            isSelected = isSelected,
            onClick = onClick,
            modifier = availabilityModifier
        )
    } else {
        EmberrBottomSheetOption(
            label = label,
            subtitle = subtitle,
            isSelected = isSelected,
            onClick = onClick,
            modifier = availabilityModifier
        )
    }
}

@Composable
internal fun ImportStartButtons(isImportEnabled: Boolean, onImportClick: () -> Unit, onCloseClick: () -> Unit) {
    EmberrButtonPrimary(
        text = "Import",
        onClick = onImportClick,
        enabled = isImportEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = ImportSheetContentPadding, end = ImportSheetContentPadding)
    )
    EmberrButtonSecondary(
        text = "Close",
        onClick = onCloseClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = ImportSheetContentPadding, end = ImportSheetContentPadding)
    )
}

@Composable
internal fun ImportProgressMessage(doneFraction: Float, progressText: String, onStopClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = ImportSheetContentPadding, vertical = 8.dp)
    ) {
        LinearProgressIndicator(
            progress = { doneFraction },
            color = MaterialTheme.colorScheme.onSurface,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = PROGRESS_TRACK_ALPHA),
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = progressText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(doneFraction * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        EmberrButtonSecondary(
            text = "Stop import",
            onClick = onStopClick,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

@Composable
internal fun ImportWorkingMessage(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ImportSheetContentPadding, vertical = 8.dp)
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun ImportOutcomeMessage(
    title: String,
    detailLines: List<String>,
    errorLine: String?,
    onDoneClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = ImportSheetContentPadding, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        detailLines.forEach { line ->
            Text(text = line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        if (errorLine != null) {
            Text(
                text = errorLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
        EmberrButtonPrimary(
            text = "Done",
            onClick = onDoneClick,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

internal fun countLabel(count: Int, singularWord: String): String =
    if (count == 1) "1 $singularWord" else "$count ${singularWord}s"
