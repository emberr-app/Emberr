package com.emberr.domain.selfhost.sync

import com.emberr.domain.selfhost.crypto.SecureSyncKeyStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SelfHostConnectionState(private val secureSyncKeyStorage: SecureSyncKeyStorage) {
    private val _isConnected by lazy { MutableStateFlow(hasSavedVault()) }
    val isConnected: StateFlow<Boolean> by lazy { _isConnected.asStateFlow() }

    fun refresh() {
        _isConnected.value = hasSavedVault()
    }

    private fun hasSavedVault(): Boolean =
        secureSyncKeyStorage.getServerCredentials() != null && secureSyncKeyStorage.getEncryptionKey() != null
}
