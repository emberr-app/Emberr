package com.emberr.domain.cloud.api

import com.emberr.domain.cloud.keys.CloudAccountKeys
import com.emberr.domain.cloud.keys.CloudRecoveryKeys

enum class CloudEmailCodePurpose(internal val wireValue: String) {
    SIGNUP("signup"),
    RECOVER("recover")
}

class CloudPasswordParams(
    val salt: ByteArray,
    val memLimit: Long,
    val opsLimit: Int
)

class CloudSignedInDevice(
    val token: String,
    val keys: CloudAccountKeys
)

class CloudRecoveryStart(
    val encryptedToken: ByteArray,
    val keys: CloudRecoveryKeys
)

class CloudSession(
    val id: String,
    val deviceName: String,
    val createdAtMicroseconds: Long,
    val lastUsedAtMicroseconds: Long,
    val isCurrent: Boolean
)

class CloudAccountDetails(
    val email: String,
    val storageUsedBytes: Long,
    val keys: CloudAccountKeys
)
