package com.emberr.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CredentialFieldKind {
    USERNAME,
    PASSWORD,
    NOTE
}

@Immutable
@Serializable
data class CredentialField(
    val id: String,
    val kind: CredentialFieldKind,
    val label: String,
    val plainValue: String = "",
    val encryptedValue: String? = null,
    val keyId: String? = null
)

@Serializable
data class CredentialKey(
    val id: String,
    val key: String,
    val createdAt: Long
)

@Immutable
@Serializable
@SerialName("credentials")
data class CredentialsBlock(
    override val id: String,
    val serviceName: String = "",
    val loginUrl: String = "",
    val iconUrl: String? = null,
    val fields: List<CredentialField> = emptyList(),
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isHighlighted: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

fun CredentialsBlock.searchableText(): String =
    (listOf(serviceName, loginUrl) + fields.filter { it.kind == CredentialFieldKind.USERNAME }.map { it.plainValue })
        .filter { it.isNotBlank() }
        .joinToString(" ")

fun CredentialsBlock.hasNothingFilledIn(): Boolean =
    serviceName.isBlank() && loginUrl.isBlank() && fields.none { it.plainValue.isNotBlank() || it.encryptedValue != null }

data class ChangedCredentialField(
    val field: CredentialField,
    val labelChanged: Boolean,
    val valueChanged: Boolean
)

data class CredentialsEdit(
    val serviceName: String? = null,
    val loginUrl: String? = null,
    val changedFields: List<ChangedCredentialField> = emptyList(),
    val addedFields: List<CredentialField> = emptyList(),
    val removedFieldIds: Set<String> = emptySet()
)

fun applyCredentialsEdit(latest: CredentialsBlock, edit: CredentialsEdit): CredentialsBlock {
    val changesByFieldId = edit.changedFields.associateBy { change -> change.field.id }
    val keptFields = latest.fields
        .filterNot { field -> field.id in edit.removedFieldIds }
        .map { field ->
            val change = changesByFieldId[field.id] ?: return@map field
            val withNewLabel = if (change.labelChanged) field.copy(label = change.field.label) else field
            if (change.valueChanged) {
                withNewLabel.copy(
                    kind = change.field.kind,
                    plainValue = change.field.plainValue,
                    encryptedValue = change.field.encryptedValue,
                    keyId = change.field.keyId
                )
            } else {
                withNewLabel
            }
        }
    val latestFieldIds = latest.fields.map { field -> field.id }.toSet()
    val changedValuesOfFieldsRemovedElsewhere = edit.changedFields
        .filter { change -> change.valueChanged && change.field.id !in latestFieldIds }
        .map { change -> change.field }
    val loginUrlChanged = edit.loginUrl != null && edit.loginUrl != latest.loginUrl
    return latest.copy(
        serviceName = edit.serviceName ?: latest.serviceName,
        loginUrl = edit.loginUrl ?: latest.loginUrl,
        iconUrl = if (loginUrlChanged) null else latest.iconUrl,
        fields = keptFields + changedValuesOfFieldsRemovedElsewhere + edit.addedFields
    )
}

private val UrlSchemePattern = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):")

fun openableLoginUrl(loginUrl: String): String? {
    val trimmedUrl = loginUrl.trim()
    if (trimmedUrl.isEmpty()) return null
    val schemeMatch = UrlSchemePattern.find(trimmedUrl) ?: return "https://$trimmedUrl"
    val restAfterScheme = trimmedUrl.substring(schemeMatch.value.length)
    val isHostWithPort = !restAfterScheme.startsWith("//") && restAfterScheme.firstOrNull()?.isDigit() == true
    if (isHostWithPort) return "https://$trimmedUrl"
    val scheme = schemeMatch.groupValues[1].lowercase()
    return trimmedUrl.takeIf { scheme == "http" || scheme == "https" }
}
