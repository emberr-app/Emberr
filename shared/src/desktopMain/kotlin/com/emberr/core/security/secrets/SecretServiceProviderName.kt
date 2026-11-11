// Asks D-Bus which program is answering as the keyring so Settings can name it properly.
package com.emberr.core.security.secrets

import java.io.File
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBus
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_secrets_system_keyring

object SecretServiceProviderName {

    fun unrecognisedProviderName(): String = secretStorePhrase(Res.string.message_secrets_system_keyring)

    fun detectOrNull(): String? {
        val processName = runCatching { readNameOfOwningProcess() }.getOrNull() ?: return null
        return friendlyNameFor(processName)
    }

    private fun readNameOfOwningProcess(): String? {
        DBusConnectionBuilder.forSessionBus().withShared(false).build().use { connection ->
            val busDaemon = connection.getRemoteObject(
                DBUS_DAEMON_SERVICE,
                DBUS_DAEMON_OBJECT_PATH,
                DBus::class.java
            )
            val ownerBusName = busDaemon.GetNameOwner(SECRET_SERVICE_NAME) ?: return null
            val processId = busDaemon.GetConnectionUnixProcessID(ownerBusName).toInt()
            val processNameFile = File("/proc/$processId/comm")
            if (!processNameFile.exists()) return null
            return processNameFile.readText().trim().lowercase()
        }
    }

    private fun friendlyNameFor(processName: String): String = when {
        processName.startsWith("oo7") -> "oo7 Keyring"
        processName.startsWith("gnome-keyring") -> "GNOME Keyring"
        processName.startsWith("kwalletd") || processName.startsWith("ksecretd") -> "KWallet"
        processName.contains("keepassxc") -> "KeePassXC"
        processName.contains("bitwarden") || processName.contains("goldwarden") -> "Bitwarden"
        processName.contains("seahorse") -> "GNOME Keyring"
        else -> unrecognisedProviderName()
    }

    private const val DBUS_DAEMON_SERVICE = "org.freedesktop.DBus"
    private const val DBUS_DAEMON_OBJECT_PATH = "/org/freedesktop/DBus"
    private const val SECRET_SERVICE_NAME = "org.freedesktop.secrets"
}
