package com.emberr.domain.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmbeddingChunkLimitTest {

    @Test
    fun everyChunkFitsTheEmbedderEvenWithALongHeaderAndTextWithoutSpaces() {
        val longHeader = "[Source: Note]\nTitle: " + "h".repeat(300)
        val textWithoutSpaces = "https://example.com/" + "a1b2c3".repeat(500)

        val chunkTexts = embeddingSafeChunkTexts(longHeader, textWithoutSpaces)

        assertTrue(chunkTexts.all { chunkText -> chunkText.length <= MAX_EMBEDDING_INPUT_CHARACTERS })
        assertEquals(textWithoutSpaces, chunkTexts.joinToString("") { chunkText -> chunkText.substringAfter('\n').substringAfter('\n') })
    }
}
