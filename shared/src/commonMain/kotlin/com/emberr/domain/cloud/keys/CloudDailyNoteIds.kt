package com.emberr.domain.cloud.keys

import com.emberr.domain.cloud.crypto.CloudCrypto

class CloudDailyNoteIds(private val cloudCrypto: CloudCrypto) {

    suspend fun dailyNoteId(spaceKey: ByteArray, date: String): String {
        val dailyIdKey = cloudCrypto.deriveSubKey(spaceKey, DAILY_ID_KEY_ID, DAILY_ID_KEY_CONTEXT, CloudCrypto.KEY_SIZE)
        val dateHash = cloudCrypto.keyedHash(date.encodeToByteArray(), dailyIdKey, DATE_HASH_SIZE)
        return DAILY_ID_PREFIX + dateHash.joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
    }

    companion object {
        private const val DAILY_ID_KEY_ID = 1
        private const val DAILY_ID_KEY_CONTEXT = "dailyids"
        private const val DATE_HASH_SIZE = 16
        private const val DAILY_ID_PREFIX = "daily_"
    }
}
