package com.emberr.presentation.shared.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun EmberrCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    tickColor: Color = MaterialTheme.colorScheme.primary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val fillColor by animateColorAsState(if (checked) MaterialTheme.colorScheme.surface else Color.Transparent, tween(120))
    val borderColor by animateColorAsState(if (checked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.outline, tween(120))
    val tickAlpha by animateFloatAsState(if (checked) 1f else 0f, tween(120))
    val boxSize by animateDpAsState(if (checked) 21.dp else 19.dp, tween(120))

    Box(
        modifier = modifier
            .requiredSize(24.dp)
            .then(
                if (onCheckedChange == null) Modifier else Modifier.toggleable(
                    value = checked,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(boxSize)) {
            val borderWidth = 1.25.dp.toPx()
            val cornerRadius = 5.dp.toPx()
            drawRoundRect(color = fillColor, cornerRadius = CornerRadius(cornerRadius))
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(borderWidth / 2, borderWidth / 2),
                size = Size(size.width - borderWidth, size.height - borderWidth),
                cornerRadius = CornerRadius(cornerRadius - borderWidth / 2),
                style = Stroke(width = borderWidth)
            )
            val tickPath = Path().apply {
                moveTo(size.width * 0.31f, size.height * 0.52f)
                lineTo(size.width * 0.44f, size.height * 0.65f)
                lineTo(size.width * 0.70f, size.height * 0.37f)
            }
            drawPath(
                path = tickPath,
                color = tickColor.copy(alpha = tickAlpha),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
