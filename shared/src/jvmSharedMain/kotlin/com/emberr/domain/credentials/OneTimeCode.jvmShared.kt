package com.emberr.domain.credentials

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual fun oneTimeCodeHmac(algorithm: OneTimeCodeAlgorithm, key: ByteArray, message: ByteArray): ByteArray {
    val macName = when (algorithm) {
        OneTimeCodeAlgorithm.SHA1 -> "HmacSHA1"
        OneTimeCodeAlgorithm.SHA256 -> "HmacSHA256"
        OneTimeCodeAlgorithm.SHA512 -> "HmacSHA512"
    }
    val mac = Mac.getInstance(macName)
    mac.init(SecretKeySpec(key, macName))
    return mac.doFinal(message)
}
