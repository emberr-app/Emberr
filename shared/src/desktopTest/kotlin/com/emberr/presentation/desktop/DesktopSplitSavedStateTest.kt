package com.emberr.presentation.desktop

import androidx.compose.ui.geometry.Rect
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DesktopSplitSavedStateTest {

    private val today = LocalDate(2026, 10, 14)

    @Test
    fun savedSplitComesBackWithTheSamePanesFocusAndWidths() {
        val splitState = DesktopSplitState()
        splitState.focusedPane = DetailPane.Note("note-a")
        splitState.addPane(DetailPane.Daily(today), shouldFocusNewPane = false)
        splitState.addPane(DetailPane.Note("note-c"), shouldFocusNewPane = true)
        splitState.updateContentLayout(Rect(0f, 0f, 900f, 600f), minimumPaneWidthPx = 0f, addPaneDropZoneWidthPx = 0f)
        splitState.resizeDivider(dividerIndex = 0, deltaPx = 90f)

        val savedLayout = assertNotNull(decodeSavedSplitLayout(splitState.encodeOpenedState()!!, today))

        assertEquals(listOf(DetailPane.Note("note-a"), DetailPane.Daily(today), DetailPane.Note("note-c")), savedLayout.panes)
        assertEquals(2, savedLayout.focusedIndex)
        listOf(0.4333f, 0.2333f, 0.3333f).zip(savedLayout.widthFractions).forEach { (expectedFraction, savedFraction) ->
            assertEquals(expectedFraction, savedFraction, absoluteTolerance = 0.001f)
        }
    }

    @Test
    fun brokenOldOrSinglePaneSavedStateIsNotTreatedAsASplit() {
        assertNull(decodeSavedSplitLayout("SPLIT:1|0.5,0.5|NOTE:note-a", today))
        assertNull(decodeSavedSplitLayout("SPLIT:0.5|LEFT|NOTE:note-a|NOTE:note-b", today))
        assertNull(decodeSavedSplitLayout("NOTE:note-a", today))
    }
}
