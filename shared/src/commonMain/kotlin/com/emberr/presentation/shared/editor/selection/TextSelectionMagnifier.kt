package com.emberr.presentation.shared.editor.selection

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density

expect fun Modifier.textSelectionMagnifier(sourceCenter: Density.() -> Offset): Modifier
