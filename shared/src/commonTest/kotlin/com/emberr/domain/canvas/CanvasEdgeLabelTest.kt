package com.emberr.domain.canvas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CanvasEdgeLabelTest {

    @Test
    fun aTypedLabelLosesSpacesAtItsEnds() {
        assertEquals("leads to", cleanedEdgeLabel("  leads to "))
    }

    @Test
    fun lineBreaksAndRepeatedSpacesBecomeOneSpaceSoTheLabelStaysOnOneLine() {
        assertEquals("leads to the launch", cleanedEdgeLabel("leads   to\nthe\r\n launch"))
    }

    @Test
    fun anEmptyOrBlankLabelIsRemoved() {
        assertNull(cleanedEdgeLabel(""))
        assertNull(cleanedEdgeLabel(" \n "))
        assertNull(cleanedEdgeLabel(null))
    }
}
