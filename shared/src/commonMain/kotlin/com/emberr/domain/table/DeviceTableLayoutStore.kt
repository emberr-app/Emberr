package com.emberr.domain.table

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.model.DatabaseView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DeviceTableLayout(
    val columnWidths: Map<String, Int> = emptyMap(),
    val freezesTitleColumnByViewId: Map<String, Boolean> = emptyMap()
) {
    fun freezesTitleColumn(view: DatabaseView): Boolean = freezesTitleColumnByViewId[view.id] ?: view.freezesTitleColumn
}

class DeviceTableLayoutStore(private val settingsManager: SettingsManager) {

    private val json = Json { ignoreUnknownKeys = true }
    private val layoutsByBlockId = MutableStateFlow<Map<String, DeviceTableLayout>>(emptyMap())

    fun currentLayoutOf(blockId: String): DeviceTableLayout {
        loadIfNeeded(blockId)
        return layoutsByBlockId.value[blockId] ?: DeviceTableLayout()
    }

    fun layoutOf(blockId: String): Flow<DeviceTableLayout> {
        loadIfNeeded(blockId)
        return layoutsByBlockId.map { it[blockId] ?: DeviceTableLayout() }.distinctUntilChanged()
    }

    fun setColumnWidth(blockId: String, columnKey: String, width: Int) =
        change(blockId) { it.copy(columnWidths = it.columnWidths + (columnKey to width)) }

    fun setFreezesTitleColumn(blockId: String, viewId: String, freezes: Boolean) =
        change(blockId) { it.copy(freezesTitleColumnByViewId = it.freezesTitleColumnByViewId + (viewId to freezes)) }

    private fun change(blockId: String, update: (DeviceTableLayout) -> DeviceTableLayout) {
        loadIfNeeded(blockId)
        val updatedLayouts = layoutsByBlockId.updateAndGet { layouts ->
            layouts + (blockId to update(layouts[blockId] ?: DeviceTableLayout()))
        }
        settingsManager.saveTableLayoutJson(blockId, json.encodeToString(updatedLayouts.getValue(blockId)))
    }

    private fun loadIfNeeded(blockId: String) {
        if (blockId in layoutsByBlockId.value) return
        val savedLayout = settingsManager.getTableLayoutJson(blockId)?.let { rawJson ->
            try {
                json.decodeFromString<DeviceTableLayout>(rawJson)
            } catch (_: Exception) {
                null
            }
        } ?: DeviceTableLayout()
        layoutsByBlockId.update { layouts -> if (blockId in layouts) layouts else layouts + (blockId to savedLayout) }
    }
}
