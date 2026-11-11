package com.emberr.domain.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EmojiNamesFileTest {

    @Test
    fun androidIndonesianAndPortugalPortugueseFindTheirEmojiNames() {
        assertEquals("id", EmojiRepository.emojiNamesFileFor("in", "ID"))
        assertEquals("pt-PT", EmojiRepository.emojiNamesFileFor("pt", "PT"))
        assertEquals("pt", EmojiRepository.emojiNamesFileFor("pt", "BR"))
    }

    @Test
    fun englishHasNoEmojiNamesFile() {
        assertNull(EmojiRepository.emojiNamesFileFor("en", "US"))
    }
}
