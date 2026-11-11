// Tells the user how to fix a missing credential manager on their particular desktop.
package com.emberr.core.security.secrets

import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_secrets_remedy_kde
import emberr.shared.generated.resources.message_secrets_remedy_linux
import emberr.shared.generated.resources.message_secrets_remedy_other

object CredentialManagerAdvice {

    fun remedyForCurrentDesktop(): String {
        if (isRunningOnKdePlasma()) return secretStorePhrase(Res.string.message_secrets_remedy_kde)
        if (isRunningOnLinux()) return secretStorePhrase(Res.string.message_secrets_remedy_linux)
        return secretStorePhrase(Res.string.message_secrets_remedy_other)
    }

    private fun isRunningOnKdePlasma(): Boolean {
        val desktopName = System.getenv("XDG_CURRENT_DESKTOP").orEmpty().lowercase()
        return desktopName.contains("kde") || desktopName.contains("plasma")
    }

    private fun isRunningOnLinux(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("linux")
}
