package com.emberr.domain.credentials

import com.emberr.domain.util.system.DeviceLockConfirmation
import com.emberr.domain.util.system.DeviceLockPrompt

class CredentialUnlockSession {

    private var isUnlocked = false

    suspend fun unlock(deviceLockPrompt: DeviceLockPrompt): Boolean {
        if (isUnlocked) return true
        val confirmation = deviceLockPrompt.confirm(
            title = "Unlock logins",
            description = "Confirm it's you to see your saved logins."
        )
        if (confirmation == DeviceLockConfirmation.NOT_CONFIRMED) return false
        isUnlocked = true
        return true
    }

    fun lockAgain() {
        isUnlocked = false
    }
}
