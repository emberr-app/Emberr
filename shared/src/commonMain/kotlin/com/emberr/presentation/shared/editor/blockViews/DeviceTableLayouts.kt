package com.emberr.presentation.shared.editor.blockViews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.emberr.domain.table.DeviceTableLayout
import com.emberr.domain.table.DeviceTableLayoutStore
import org.koin.compose.koinInject

@Composable
internal fun rememberDeviceTableLayout(blockId: String): DeviceTableLayout {
    val store = koinInject<DeviceTableLayoutStore>()
    val layout by remember(store, blockId) { store.layoutOf(blockId) }.collectAsState(initial = store.currentLayoutOf(blockId))
    return layout
}
