package com.emberr.presentation.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EmberrTab(
    label: String,
    isSelected: Boolean,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    selectedBackground: Color = contentColor.copy(alpha = 0.11f),
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) selectedBackground else Color.Transparent
    val textColor = if (isSelected) contentColor else contentColor.copy(alpha = 0.6f)
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = textColor,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
