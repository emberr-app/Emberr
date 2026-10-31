package com.emberr.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CredentialsEditTest {

    @Test
    fun editKeepsPasswordChangedOnAnotherDeviceAndAppliesOnlyWhatChanged() {
        val username = CredentialField(id = "user", kind = CredentialFieldKind.USERNAME, label = "Username", plainValue = "old@example.com")
        val newerPassword = CredentialField(
            id = "password",
            kind = CredentialFieldKind.PASSWORD,
            label = "Password",
            encryptedValue = "password-from-phone",
            keyId = "key-1"
        )
        val removedNote = CredentialField(id = "old-note", kind = CredentialFieldKind.NOTE, label = "Note", encryptedValue = "old-note-value", keyId = "key-1")
        val latest = CredentialsBlock(id = "card", serviceName = "Mail", fields = listOf(username, newerPassword, removedNote))
        val addedNote = CredentialField(id = "new-note", kind = CredentialFieldKind.NOTE, label = "Note", encryptedValue = "new-note-value", keyId = "key-1")
        val edit = CredentialsEdit(
            changedFields = listOf(
                ChangedCredentialField(
                    field = username.copy(plainValue = "new@example.com"),
                    labelChanged = false,
                    valueChanged = true
                )
            ),
            addedFields = listOf(addedNote),
            removedFieldIds = setOf("old-note")
        )

        val result = applyCredentialsEdit(latest, edit)

        assertEquals(listOf(username.copy(plainValue = "new@example.com"), newerPassword, addedNote), result.fields)
        assertEquals("Mail", result.serviceName)
    }
}
