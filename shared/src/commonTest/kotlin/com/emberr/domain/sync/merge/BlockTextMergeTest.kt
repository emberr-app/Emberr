package com.emberr.domain.sync.merge

import com.emberr.domain.model.InlineSpan
import com.emberr.domain.model.TextBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlockTextMergeTest {

    private fun block(text: String, updatedAt: Long, spans: List<InlineSpan> = emptyList()) =
        TextBlock(id = "block", text = text, inlineSpans = spans, updatedAt = updatedAt)

    private fun textAfterSync(base: String, local: String, localTime: Long, remote: String, remoteTime: Long): String {
        val merged = BlockTextMerge.mergeTextBlock(block(base, 1), block(local, localTime), block(remote, remoteTime)) as TextBlock?
        return merged?.text ?: if (remoteTime > localTime) remote else local
    }

    @Test
    fun editsAtStartAndEndBothSurvive() {
        val merged = BlockTextMerge.mergeTextBlock(
            block("Buy milk and bread", 1),
            block("Buy oat milk and bread", 2),
            block("Buy milk and bread, and eggs", 3)
        ) as TextBlock
        assertEquals("Buy oat milk and bread, and eggs", merged.text)
        assertEquals(4, merged.updatedAt)
    }

    @Test
    fun differentAdditionsAtSameSpotAreBothKeptOlderFirst() {
        assertEquals("Buy milk today please", textAfterSync("Buy milk", "Buy milk today", 2, "Buy milk please", 3))
    }

    @Test
    fun additionThatContainsTheOtherIsKeptOnce() {
        assertEquals("Buy milk and eggs please", textAfterSync("Buy milk", "Buy milk and eggs", 2, "Buy milk and eggs please", 3))
    }

    @Test
    fun deletedPassageKeepsTextTypedInsideItOnTheOtherDevice() {
        assertEquals(
            "Buy milk and a big list",
            textAfterSync("Buy milk and eggs please", "Buy milk", 3, "Buy milk and eggs and a big list please", 2)
        )
    }

    @Test
    fun sameWordRewrittenOnBothSidesTakesTheNewerWord() {
        assertEquals("Buy cheese today", textAfterSync("Buy milk today", "Buy bread today", 2, "Buy cheese today", 3))
    }

    @Test
    fun onlyOneSideChangedSinceBaseIsLeftToNewerWins() {
        assertNull(BlockTextMerge.mergeTextBlock(block("Buy milk", 2), block("Buy milk", 2), block("Buy oat milk", 3)))
    }

    @Test
    fun swappingLocalAndRemoteGivesTheSameBlock() {
        val base = block("Buy milk and bread", 1)
        val phone = block("Buy oat milk and bread", 2)
        val laptop = block("Buy milk and bread, and eggs", 2)
        assertEquals(BlockTextMerge.mergeTextBlock(base, phone, laptop), BlockTextMerge.mergeTextBlock(base, laptop, phone))
    }

    @Test
    fun boldAddedOnOlderSideFollowsItsWord() {
        val merged = BlockTextMerge.mergeTextBlock(
            block("Buy milk", 1),
            block("Buy milk now", 2, listOf(InlineSpan(start = 9, end = 12, bold = true))),
            block("Hey, Buy milk", 3)
        ) as TextBlock
        assertEquals("Hey, Buy milk now", merged.text)
        assertEquals(listOf(InlineSpan(start = 14, end = 17, bold = true)), merged.inlineSpans)
    }

    @Test
    fun deviceReceivingTheMergedParagraphDoesNotMergeItAgain() {
        val base = block("Buy milk and bread", 1)
        val laptop = block("Buy milk and bread, and eggs", 3)
        val merged = BlockTextMerge.mergeTextBlock(base, block("Buy oat milk and bread", 2), laptop) as TextBlock
        assertNull(BlockTextMerge.mergeTextBlock(base, laptop, merged))
    }
}
