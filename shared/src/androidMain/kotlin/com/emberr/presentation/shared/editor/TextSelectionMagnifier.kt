package com.emberr.presentation.shared.editor

import androidx.compose.foundation.magnifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

actual fun Modifier.textSelectionMagnifier(sourceCenter: Density.() -> Offset): Modifier =
    magnifier(
        sourceCenter = sourceCenter,
        zoom = 1.25f,
        size = DpSize(100.dp, 48.dp),
        cornerRadius = 24.dp,
        elevation = 4.dp
    )
