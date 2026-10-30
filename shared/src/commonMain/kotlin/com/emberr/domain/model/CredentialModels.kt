package com.emberr.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
enum class CredentialFieldKind {
    USERNAME,
    PASSWORD,
    ONE_TIME_CODE,
    NOTE
}

@Immutable
@Serializable
data class CredentialField(
    val id: String,
    val kind: CredentialFieldKind,
    val label: String,
    val plainValue: String = "",
    val encryptedValue: String? = null,
    val keyId: String? = null
)

@Serializable
data class CredentialKey(
    val id: String,
    val key: String,
    val createdAt: Long
)
