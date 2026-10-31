package com.emberr.domain.credentials

enum class OneTimeCodeAlgorithm { SHA1, SHA256, SHA512 }

data class OneTimeCode(val digits: String, val secondsLeft: Int)

expect fun oneTimeCodeHmac(algorithm: OneTimeCodeAlgorithm, key: ByteArray, message: ByteArray): ByteArray

private const val OTPAUTH_SCHEME = "otpauth://"
private const val OTPAUTH_TOTP_PREFIX = "otpauth://totp/"
private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
private const val DEFAULT_DIGITS = 6
private const val DEFAULT_PERIOD_SECONDS = 30
private val allowedDigitCounts = 6..8

private class OneTimeCodeSettings(
    val secret: ByteArray,
    val digits: Int,
    val periodSeconds: Int,
    val algorithm: OneTimeCodeAlgorithm
)

fun oneTimeCodeFor(codeKey: String, epochMillis: Long): OneTimeCode? {
    val settings = settingsFrom(codeKey.trim()) ?: return null
    val epochSeconds = epochMillis.floorDiv(1000L)
    val period = settings.periodSeconds.toLong()
    val hash = oneTimeCodeHmac(settings.algorithm, settings.secret, epochSeconds.floorDiv(period).toBigEndianBytes())
    val offset = hash.last().toInt() and 0x0f
    val truncatedHash = ((hash[offset].toInt() and 0x7f) shl 24) or
        ((hash[offset + 1].toInt() and 0xff) shl 16) or
        ((hash[offset + 2].toInt() and 0xff) shl 8) or
        (hash[offset + 3].toInt() and 0xff)
    var digitLimit = 1
    repeat(settings.digits) { digitLimit *= 10 }
    val code = (truncatedHash % digitLimit).toString().padStart(settings.digits, '0')
    return OneTimeCode(digits = code, secondsLeft = (period - epochSeconds.mod(period)).toInt())
}

private fun settingsFrom(codeKey: String): OneTimeCodeSettings? =
    if (codeKey.startsWith(OTPAUTH_SCHEME, ignoreCase = true)) {
        settingsFromLink(codeKey)
    } else {
        decodeBase32(codeKey)?.let { secret ->
            OneTimeCodeSettings(secret, DEFAULT_DIGITS, DEFAULT_PERIOD_SECONDS, OneTimeCodeAlgorithm.SHA1)
        }
    }

private fun settingsFromLink(link: String): OneTimeCodeSettings? {
    if (!link.startsWith(OTPAUTH_TOTP_PREFIX, ignoreCase = true)) return null
    val parameters = link.substringBefore('#')
        .substringAfter('?', missingDelimiterValue = "")
        .split('&')
        .filter { it.isNotEmpty() }
        .associate { pair -> pair.substringBefore('=').lowercase() to pair.substringAfter('=', missingDelimiterValue = "") }

    val secret = parameters["secret"]?.let(::decodeBase32) ?: return null
    val digits = parameters["digits"]?.let { it.toIntOrNull() ?: return null } ?: DEFAULT_DIGITS
    if (digits !in allowedDigitCounts) return null
    val periodSeconds = parameters["period"]?.let { it.toIntOrNull() ?: return null } ?: DEFAULT_PERIOD_SECONDS
    if (periodSeconds <= 0) return null
    val algorithm = parameters["algorithm"]?.let { name ->
        OneTimeCodeAlgorithm.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: return null
    } ?: OneTimeCodeAlgorithm.SHA1
    return OneTimeCodeSettings(secret, digits, periodSeconds, algorithm)
}

private fun decodeBase32(text: String): ByteArray? {
    val cleanedText = text.filterNot { it.isWhitespace() }.trimEnd('=').uppercase()
    val bytes = ArrayList<Byte>(cleanedText.length * 5 / 8)
    var buffer = 0
    var bitsInBuffer = 0
    for (character in cleanedText) {
        val value = BASE32_ALPHABET.indexOf(character)
        if (value < 0) return null
        buffer = (buffer shl 5) or value
        bitsInBuffer += 5
        if (bitsInBuffer >= 8) {
            bitsInBuffer -= 8
            bytes.add((buffer shr bitsInBuffer).toByte())
            buffer = buffer and ((1 shl bitsInBuffer) - 1)
        }
    }
    return bytes.toByteArray().takeIf { it.isNotEmpty() }
}

private fun Long.toBigEndianBytes(): ByteArray = ByteArray(8) { index -> (this shr (56 - index * 8)).toByte() }
