package com.emberr.domain.cloud.keys

import com.emberr.domain.cloud.crypto.CloudCrypto
import emberr.shared.generated.resources.Res

class CloudRecoveryWords(private val cloudCrypto: CloudCrypto) {

    suspend fun wordsFromRecoveryKey(recoveryKey: ByteArray): String {
        require(recoveryKey.size == CloudCrypto.KEY_SIZE) { "A recovery key must be ${CloudCrypto.KEY_SIZE} bytes" }
        val wordList = readWordList()
        val keyWithChecksum = recoveryKey + cloudCrypto.sha256(recoveryKey).first()
        return (0 until WORD_COUNT).joinToString(" ") { wordPosition ->
            wordList[readWordIndex(keyWithChecksum, wordPosition)]
        }
    }

    suspend fun recoveryKeyFromWords(typedWords: String): ByteArray {
        val words = typedWords.trim().lowercase().split(Regex("\\s+"))
        if (words.size != WORD_COUNT) throw InvalidRecoveryWordsException()
        val indexByWord = readWordList().withIndex().associate { (index, word) -> word to index }
        val keyWithChecksum = ByteArray(CloudCrypto.KEY_SIZE + 1)
        words.forEachIndexed { wordPosition, word ->
            val wordIndex = indexByWord[word] ?: throw InvalidRecoveryWordsException()
            writeWordIndex(keyWithChecksum, wordPosition, wordIndex)
        }
        val recoveryKey = keyWithChecksum.copyOf(CloudCrypto.KEY_SIZE)
        if (cloudCrypto.sha256(recoveryKey).first() != keyWithChecksum.last()) throw InvalidRecoveryWordsException()
        return recoveryKey
    }

    private suspend fun readWordList(): List<String> =
        Res.readBytes(WORD_LIST_FILE_PATH).decodeToString().lines().filter { it.isNotEmpty() }

    private fun readWordIndex(bytes: ByteArray, wordPosition: Int): Int {
        var wordIndex = 0
        for (bitInWord in 0 until BITS_PER_WORD) {
            val bitPosition = wordPosition * BITS_PER_WORD + bitInWord
            val bit = (bytes[bitPosition / 8].toInt() shr (7 - bitPosition % 8)) and 1
            wordIndex = (wordIndex shl 1) or bit
        }
        return wordIndex
    }

    private fun writeWordIndex(bytes: ByteArray, wordPosition: Int, wordIndex: Int) {
        for (bitInWord in 0 until BITS_PER_WORD) {
            val bit = (wordIndex shr (BITS_PER_WORD - 1 - bitInWord)) and 1
            if (bit == 0) continue
            val bitPosition = wordPosition * BITS_PER_WORD + bitInWord
            bytes[bitPosition / 8] = (bytes[bitPosition / 8].toInt() or (1 shl (7 - bitPosition % 8))).toByte()
        }
    }

    companion object {
        private const val WORD_LIST_FILE_PATH = "files/bip39-english.txt"
        private const val WORD_COUNT = 24
        private const val BITS_PER_WORD = 11
    }
}
