package com.emberr.domain.cloud.crypto

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.sun.jna.Library
import com.sun.jna.Native
import java.lang.reflect.Proxy

internal actual fun runArgon2id(
    passwordBytes: ByteArray,
    salt: ByteArray,
    opsLimit: Int,
    memLimitBytes: Long,
    algorithm: Int,
    keySize: Int
): ByteArray {
    val loadedLibsodium = (Proxy.getInvocationHandler(LibsodiumInitializer.sodiumJna) as Library.Handler).nativeLibrary
    val memLimitAsSizeT: Any = if (Native.SIZE_T_SIZE == Long.SIZE_BYTES) memLimitBytes else memLimitBytes.toInt()
    val key = ByteArray(keySize)
    val result = loadedLibsodium.getFunction("crypto_pwhash").invokeInt(
        arrayOf(
            key,
            keySize.toLong(),
            passwordBytes,
            passwordBytes.size.toLong(),
            salt,
            opsLimit.toLong(),
            memLimitAsSizeT,
            algorithm
        )
    )
    check(result == 0) { "Argon2id failed, most likely because there was not enough memory" }
    return key
}
