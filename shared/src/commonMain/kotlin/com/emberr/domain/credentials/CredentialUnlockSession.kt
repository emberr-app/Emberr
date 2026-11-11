package com.emberr.domain.credentials

import com.emberr.domain.util.system.DeviceLockConfirmation
import com.emberr.domain.util.system.DeviceLockPrompt

class CredentialUnlockSession {

    private var isUnlocked = false

    suspend fun unlock(deviceLockPrompt: DeviceLockPrompt, promptTitle: String, promptDescription: String): Boolean {
        if (isUnlocked) return true
        val confirmation = deviceLockPrompt.confirm(title = promptTitle, description = promptDescription)
        if (confirmation == DeviceLockConfirmation.NOT_CONFIRMED) return false
        isUnlocked = true
        return true
    }

    fun lockAgain() {
        isUnlocked = false
    }
}
