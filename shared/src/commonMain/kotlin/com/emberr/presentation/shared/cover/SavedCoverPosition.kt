package com.emberr.presentation.shared.cover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import com.emberr.domain.cover.DeviceCoverPositionStore
import org.koin.compose.koinInject

@Composable
fun rememberSavedCoverPosition(noteId: String, coverImagePath: String): State<Float> {
    val coverPositionStore = koinInject<DeviceCoverPositionStore>()
    val positionFlow = remember(noteId, coverImagePath) { coverPositionStore.positionOf(noteId, coverImagePath) }
    return positionFlow.collectAsState(initial = coverPositionStore.currentPositionOf(noteId, coverImagePath))
}

fun coverAlignmentFor(position: Float): Alignment =
    BiasAlignment(horizontalBias = 0f, verticalBias = position * 2f - 1f)
