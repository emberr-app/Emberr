// Settings row naming the credential manager that holds the keys, with details on tap.
package com.emberr.presentation.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emberr.core.security.secrets.DesktopSecretStore
import com.emberr.core.security.secrets.SecretStorageState
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.settings_credential_storage
import emberr.shared.generated.resources.settings_credential_storage_checking
import emberr.shared.generated.resources.settings_credential_storage_checking_detail
import emberr.shared.generated.resources.settings_credential_storage_got_it
import emberr.shared.generated.resources.settings_credential_storage_plain_text
import emberr.shared.generated.resources.settings_credential_storage_plain_text_detail
import emberr.shared.generated.resources.settings_credential_storage_protected_detail
import emberr.shared.generated.resources.settings_group_security
import emberr.shared.generated.resources.shield_alert
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
actual fun SecretStorageSettingsSection() {
    val secretStore = koinInject<DesktopSecretStore>()
    val storageState by secretStore.storageState.collectAsState()
    var isDetailDialogVisible by remember { mutableStateOf(false) }

    SettingsGroup(title = stringResource(Res.string.settings_group_security)) {
        SettingsActionRow(
            icon = painterResource(Res.drawable.shield_alert),
            title = stringResource(Res.string.settings_credential_storage),
            trailingLabel = shortSummaryFor(storageState),
            isDestructive = storageState is SecretStorageState.StoredInPlainText,
            onClick = { isDetailDialogVisible = true }
        )
    }

    if (isDetailDialogVisible) {
        SecretStorageDetailDialog(
            storageState = storageState,
            onDismiss = { isDetailDialogVisible = false }
        )
    }
}

@Composable
private fun SecretStorageDetailDialog(
    storageState: SecretStorageState,
    onDismiss: () -> Unit
) {
    EmberrAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.settings_credential_storage)
    ) {
        Text(
            text = detailedExplanationFor(storageState),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(20.dp))
        EmberrButtonPrimary(
            text = stringResource(Res.string.settings_credential_storage_got_it),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun shortSummaryFor(storageState: SecretStorageState): String = when (storageState) {
    is SecretStorageState.Starting -> stringResource(Res.string.settings_credential_storage_checking)
    is SecretStorageState.ProtectedByCredentialManager -> storageState.backendDisplayName
    is SecretStorageState.StoredInPlainText -> stringResource(Res.string.settings_credential_storage_plain_text)
}

@Composable
private fun detailedExplanationFor(storageState: SecretStorageState): String = when (storageState) {
    is SecretStorageState.Starting ->
        stringResource(Res.string.settings_credential_storage_checking_detail)

    is SecretStorageState.ProtectedByCredentialManager ->
        stringResource(Res.string.settings_credential_storage_protected_detail, storageState.backendDisplayName)

    is SecretStorageState.StoredInPlainText ->
        stringResource(Res.string.settings_credential_storage_plain_text_detail, storageState.reason, storageState.remedy)
}
