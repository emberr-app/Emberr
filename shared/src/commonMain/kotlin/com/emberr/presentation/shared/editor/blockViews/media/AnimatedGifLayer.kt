package com.emberr.presentation.shared.editor.blockViews.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@Composable
expect fun AnimatedGifLayer(
    absolutePath: String,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center
)
