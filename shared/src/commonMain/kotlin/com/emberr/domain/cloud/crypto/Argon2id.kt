package com.emberr.domain.cloud.crypto

internal expect fun runArgon2id(
    passwordBytes: ByteArray,
    salt: ByteArray,
    opsLimit: Int,
    memLimitBytes: Long,
    algorithm: Int,
    keySize: Int
): ByteArray
