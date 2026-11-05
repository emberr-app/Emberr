package com.emberr.presentation.shared.editor.blockViews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SolidDividerBlockView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(1.5.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(1.dp))
    )
}

@Composable
fun ThreeDotDividerBlockView() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dotSize = 6.dp
        val dotColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(16.dp))
        Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(16.dp))
        Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
    }
}
