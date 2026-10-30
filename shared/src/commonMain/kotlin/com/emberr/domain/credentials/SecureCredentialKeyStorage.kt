package com.emberr.domain.credentials

import com.emberr.domain.model.CredentialKey

expect class SecureCredentialKeyStorage : CredentialKeyStore {
    override fun listKeyIndex(): List<CredentialKeyIndexEntry>
    override fun readKey(keyId: String): String?
    override fun addKey(key: CredentialKey)
}
