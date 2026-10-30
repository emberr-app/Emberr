package com.emberr.domain.credentials

import com.emberr.domain.model.CredentialKey
import kotlinx.serialization.Serializable

@Serializable
data class CredentialKeyIndexEntry(val id: String, val createdAt: Long)

interface CredentialKeyStore {
    fun listKeyIndex(): List<CredentialKeyIndexEntry>
    fun readKey(keyId: String): String?
    fun addKey(key: CredentialKey)
}
