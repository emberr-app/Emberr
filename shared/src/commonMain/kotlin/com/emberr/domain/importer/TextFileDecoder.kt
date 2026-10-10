package com.emberr.domain.importer

private const val FIRST_WINDOWS_1252_SPECIAL_BYTE = 0x80
private const val LAST_WINDOWS_1252_SPECIAL_BYTE = 0x9F
private val windows1252SpecialCharacterCodes = intArrayOf(
    0x20AC, 0x0081, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021,
    0x02C6, 0x2030, 0x0160, 0x2039, 0x0152, 0x008D, 0x017D, 0x008F,
    0x0090, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014,
    0x02DC, 0x2122, 0x0161, 0x203A, 0x0153, 0x009D, 0x017E, 0x0178
)

object TextFileDecoder {

    fun decode(bytes: ByteArray): String = when {
        bytes.startsWithBytes(0xEF, 0xBB, 0xBF) -> bytes.decodeToString(startIndex = 3, endIndex = bytes.size)
        bytes.startsWithBytes(0xFF, 0xFE) -> decodeUtf16(bytes, isLittleEndian = true)
        bytes.startsWithBytes(0xFE, 0xFF) -> decodeUtf16(bytes, isLittleEndian = false)
        else -> strictUtf8OrNull(bytes) ?: decodeWindows1252(bytes)
    }

    private fun strictUtf8OrNull(bytes: ByteArray): String? =
        try {
            bytes.decodeToString(throwOnInvalidSequence = true)
        } catch (_: CharacterCodingException) {
            null
        }

    private fun decodeUtf16(bytes: ByteArray, isLittleEndian: Boolean): String {
        val characters = CharArray((bytes.size - 2) / 2) { index ->
            val firstByte = bytes[2 + index * 2].toInt() and 0xFF
            val secondByte = bytes[3 + index * 2].toInt() and 0xFF
            val characterCode = if (isLittleEndian) firstByte or (secondByte shl 8) else (firstByte shl 8) or secondByte
            characterCode.toChar()
        }
        return characters.concatToString()
    }

    private fun decodeWindows1252(bytes: ByteArray): String {
        val characters = CharArray(bytes.size) { index ->
            val byteValue = bytes[index].toInt() and 0xFF
            val characterCode = if (byteValue in FIRST_WINDOWS_1252_SPECIAL_BYTE..LAST_WINDOWS_1252_SPECIAL_BYTE) {
                windows1252SpecialCharacterCodes[byteValue - FIRST_WINDOWS_1252_SPECIAL_BYTE]
            } else {
                byteValue
            }
            characterCode.toChar()
        }
        return characters.concatToString()
    }

    private fun ByteArray.startsWithBytes(vararg expectedBytes: Int): Boolean =
        size >= expectedBytes.size && expectedBytes.indices.all { index -> (this[index].toInt() and 0xFF) == expectedBytes[index] }
}
