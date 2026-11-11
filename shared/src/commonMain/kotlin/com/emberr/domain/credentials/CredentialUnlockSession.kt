package com.emberr.domain.credentials

import com.emberr.domain.util.system.DeviceLockConfirmation
import com.emberr.domain.util.system.DeviceLockPrompt
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_unlock_credentials_description
import emberr.shared.generated.resources.message_unlock_credentials_title
import org.jetbrains.compose.resources.getString

class CredentialUnlockSession {

    private var isUnlocked = false

    suspend fun unlock(deviceLockPrompt: DeviceLockPrompt): Boolean {
        if (isUnlocked) return true
        val confirmation = deviceLockPrompt.confirm(
            title = getString(Res.string.message_unlock_credentials_title),
            description = getString(Res.string.message_unlock_credentials_description)
        )
        if (confirmation == DeviceLockConfirmation.NOT_CONFIRMED) return false
        isUnlocked = true
        return true
    }

    fun lockAgain() {
        isUnlocked = false
    }
}
