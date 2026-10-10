package com.emberr.domain.importer

import kotlin.test.Test
import kotlin.test.assertEquals

class TextFileDecoderTest {

    private fun bytesOf(vararg values: Int): ByteArray = ByteArray(values.size) { index -> values[index].toByte() }

    @Test
    fun utf8TextIsReadAsItIs() {
        assertEquals("Plain text and café", TextFileDecoder.decode("Plain text and café".encodeToByteArray()))
    }

    @Test
    fun theUtf8MarkAtTheStartIsLeftOut() {
        assertEquals("Hi", TextFileDecoder.decode(bytesOf(0xEF, 0xBB, 0xBF, 0x48, 0x69)))
    }

    @Test
    fun oldWindowsTextFallsBackToWindows1252() {
        assertEquals(
            "café “quote” € – done",
            TextFileDecoder.decode(
                bytesOf(
                    0x63, 0x61, 0x66, 0xE9, 0x20,
                    0x93, 0x71, 0x75, 0x6F, 0x74, 0x65, 0x94, 0x20,
                    0x80, 0x20, 0x96, 0x20,
                    0x64, 0x6F, 0x6E, 0x65
                )
            )
        )
    }

    @Test
    fun utf16TextIsRecognisedByItsStartMark() {
        assertEquals("Hi é", TextFileDecoder.decode(bytesOf(0xFF, 0xFE, 0x48, 0x00, 0x69, 0x00, 0x20, 0x00, 0xE9, 0x00)))
        assertEquals("Hi é", TextFileDecoder.decode(bytesOf(0xFE, 0xFF, 0x00, 0x48, 0x00, 0x69, 0x00, 0x20, 0x00, 0xE9)))
    }
}
