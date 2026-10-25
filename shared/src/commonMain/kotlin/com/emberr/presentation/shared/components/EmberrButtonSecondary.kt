package com.emberr.presentation.shared.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emberr.ui.theme.LocalAppIsDark

@Composable
fun EmberrButtonSecondary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showsWavingDots: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (LocalAppIsDark.current) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFD8D8D8),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = EmberrShadowElevation.None),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (showsWavingDots) {
            WavingDots(style = MaterialTheme.typography.bodyLarge)
        }
    }
}