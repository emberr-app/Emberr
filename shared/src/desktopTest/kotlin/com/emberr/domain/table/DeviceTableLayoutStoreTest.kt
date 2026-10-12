package com.emberr.domain.table

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.model.DatabaseView
import com.emberr.domain.model.DatabaseViewType
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceTableLayoutStoreTest {

    private val savedJsonByBlockId = mutableMapOf<String, String>()

    private val settings: SettingsManager =
        Proxy.newProxyInstance(SettingsManager::class.java.classLoader, arrayOf(SettingsManager::class.java)) { stub, method, arguments ->
            when (method.name) {
                "getTableLayoutJson" -> savedJsonByBlockId[arguments!![0] as String]
                "saveTableLayoutJson" -> {
                    savedJsonByBlockId[arguments!![0] as String] = arguments[1] as String
                    Unit
                }
                "toString" -> "settings stub"
                "hashCode" -> System.identityHashCode(stub)
                "equals" -> stub === arguments?.firstOrNull()
                else -> error("These tests do not expect SettingsManager.${method.name} to be called")
            }
        } as SettingsManager

    private val tableView = DatabaseView(id = "table-view", name = "Table", type = DatabaseViewType.TABLE)

    @Test
    fun widthsAndFreezeSavedOnThisDeviceAreThereAfterARestart() {
        val store = DeviceTableLayoutStore(settings)
        store.setColumnWidth("block-1", "STATUS", 120)
        store.setFreezesTitleColumn("block-1", "table-view", freezes = true)

        val layoutAfterRestart = DeviceTableLayoutStore(settings).currentLayoutOf("block-1")

        assertEquals(mapOf("STATUS" to 120), layoutAfterRestart.columnWidths)
        assertTrue(layoutAfterRestart.freezesTitleColumn(tableView))
        assertEquals(emptyMap(), DeviceTableLayoutStore(settings).currentLayoutOf("block-2").columnWidths)
    }

    @Test
    fun theFreezeSettingFromTheNoteIsUsedUntilThisDeviceChangesIt() {
        val store = DeviceTableLayoutStore(settings)
        val frozenInTheNote = tableView.copy(freezesTitleColumn = true)

        assertTrue(store.currentLayoutOf("block-1").freezesTitleColumn(frozenInTheNote))

        store.setFreezesTitleColumn("block-1", "table-view", freezes = false)

        assertFalse(store.currentLayoutOf("block-1").freezesTitleColumn(frozenInTheNote))
    }

    @Test
    fun aDamagedSavedLayoutIsIgnored() {
        savedJsonByBlockId["block-1"] = "{not json"

        assertEquals(DeviceTableLayout(), DeviceTableLayoutStore(settings).currentLayoutOf("block-1"))
    }
}
