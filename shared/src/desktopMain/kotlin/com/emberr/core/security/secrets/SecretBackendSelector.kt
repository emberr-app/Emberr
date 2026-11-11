// Tries every credential manager this operating system offers and picks the first that passes the probe.
package com.emberr.core.security.secrets

import com.github.javakeyring.KeyringStorageType
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_secrets_manager_rejected_test_write
import emberr.shared.generated.resources.message_secrets_no_manager_running
import emberr.shared.generated.resources.message_secrets_plain_text_file

class SecretBackendSelector(
    private val probe: SecretBackendProbe,
    private val plaintextBackend: PlaintextSecretBackend
) {

    fun selectUsableBackend(): SecretBackendSelection {
        var atLeastOneManagerAnswered = false

        candidatesForCurrentOperatingSystem().forEach { candidate ->
            val backend = probe.withTimeout(null) { candidate.openBackend() }
            if (backend != null) {
                atLeastOneManagerAnswered = true
                if (probe.isBackendUsable(backend)) {
                    return SecretBackendSelection(
                        backend = backend,
                        displayName = resolveDisplayNameFor(candidate),
                        unavailableReason = null
                    )
                }
            }
        }

        return SecretBackendSelection(
            backend = plaintextBackend,
            displayName = secretStorePhrase(Res.string.message_secrets_plain_text_file),
            unavailableReason = if (atLeastOneManagerAnswered) {
                secretStorePhrase(Res.string.message_secrets_manager_rejected_test_write)
            } else {
                secretStorePhrase(Res.string.message_secrets_no_manager_running)
            }
        )
    }

    private fun resolveDisplayNameFor(candidate: KeyringCandidate): String =
        probe.withTimeout(null) { candidate.detectDisplayName() } ?: candidate.fallbackDisplayName

    private fun candidatesForCurrentOperatingSystem(): List<KeyringCandidate> {
        val operatingSystemName = System.getProperty("os.name").orEmpty().lowercase()
        return when {
            operatingSystemName.contains("mac") || operatingSystemName.contains("darwin") -> listOf(
                KeyringCandidate("macOS Keychain") { KeyringSecretBackend.openOrNull(KeyringStorageType.OSX_KEYCHAIN) },
                KeyringCandidate("macOS Keychain") { KeyringSecretBackend.openOrNull(KeyringStorageType.LEGACY_OSX_KEYCHAIN) }
            )

            operatingSystemName.contains("windows") -> listOf(
                KeyringCandidate("Windows Credential Manager") {
                    KeyringSecretBackend.openOrNull(KeyringStorageType.WINDOWS_CREDENTIAL_STORE)
                }
            )

            else -> listOf(
                KeyringCandidate(
                    fallbackDisplayName = SecretServiceProviderName.unrecognisedProviderName(),
                    detectDisplayName = { SecretServiceProviderName.detectOrNull() },
                    openBackend = { LibsecretSecretBackend.openOrNull() }
                )
            )
        }
    }

    private class KeyringCandidate(
        val fallbackDisplayName: String,
        val detectDisplayName: () -> String? = { null },
        val openBackend: () -> SecretBackend?
    )
}

class SecretBackendSelection(
    val backend: SecretBackend,
    val displayName: String,
    val unavailableReason: String?
)
