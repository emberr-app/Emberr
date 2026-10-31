package com.emberr.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CredentialFieldKind {
    USERNAME,
    PASSWORD,
    ONE_TIME_CODE,
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
