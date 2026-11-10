package com.emberr.presentation.shared.editor.blockViews.credentials

import androidx.compose.runtime.Composable
import com.emberr.domain.model.CredentialFieldKind
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.credentials_add_a_note
import emberr.shared.generated.resources.credentials_copied_note
import emberr.shared.generated.resources.credentials_copied_password
import emberr.shared.generated.resources.credentials_copied_username
import emberr.shared.generated.resources.credentials_copy_note
import emberr.shared.generated.resources.credentials_copy_password
import emberr.shared.generated.resources.credentials_copy_username
import emberr.shared.generated.resources.credentials_field_note
import emberr.shared.generated.resources.credentials_field_password
import emberr.shared.generated.resources.credentials_field_username
import emberr.shared.generated.resources.credentials_username_or_email
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CredentialFieldKind.displayLabel(): String = when (this) {
    CredentialFieldKind.USERNAME -> stringResource(Res.string.credentials_field_username)
    CredentialFieldKind.PASSWORD -> stringResource(Res.string.credentials_field_password)
    CredentialFieldKind.NOTE -> stringResource(Res.string.credentials_field_note)
}

@Composable
internal fun CredentialFieldKind.menuLabel(): String =
    if (this == CredentialFieldKind.USERNAME) stringResource(Res.string.credentials_username_or_email) else displayLabel()

@Composable
internal fun CredentialFieldKind.valuePlaceholder(): String = when (this) {
    CredentialFieldKind.USERNAME -> stringResource(Res.string.credentials_username_or_email)
    CredentialFieldKind.PASSWORD -> stringResource(Res.string.credentials_field_password)
    CredentialFieldKind.NOTE -> stringResource(Res.string.credentials_add_a_note)
}

@Composable
internal fun CredentialFieldKind.copiedMessage(): String = when (this) {
    CredentialFieldKind.USERNAME -> stringResource(Res.string.credentials_copied_username)
    CredentialFieldKind.PASSWORD -> stringResource(Res.string.credentials_copied_password)
    CredentialFieldKind.NOTE -> stringResource(Res.string.credentials_copied_note)
}

@Composable
internal fun CredentialFieldKind.copyDescription(): String = when (this) {
    CredentialFieldKind.USERNAME -> stringResource(Res.string.credentials_copy_username)
    CredentialFieldKind.PASSWORD -> stringResource(Res.string.credentials_copy_password)
    CredentialFieldKind.NOTE -> stringResource(Res.string.credentials_copy_note)
}
