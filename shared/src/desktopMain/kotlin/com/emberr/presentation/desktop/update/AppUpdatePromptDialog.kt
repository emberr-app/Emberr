package com.emberr.presentation.desktop.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emberr.domain.update.AppUpdateController
import com.emberr.domain.update.AppUpdatePrompt
import com.emberr.domain.util.system.restartApplication
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import org.koin.compose.koinInject
import emberr.shared.generated.resources.desktop_ok
import emberr.shared.generated.resources.desktop_update_available
import emberr.shared.generated.resources.desktop_update_available_message
import emberr.shared.generated.resources.desktop_update_download
import emberr.shared.generated.resources.desktop_update_failed
import emberr.shared.generated.resources.desktop_update_later
import emberr.shared.generated.resources.desktop_update_ready_message
import emberr.shared.generated.resources.desktop_update_ready_to_install
import emberr.shared.generated.resources.desktop_update_restart
import org.jetbrains.compose.resources.stringResource
import emberr.shared.generated.resources.Res

@Composable
fun AppUpdatePromptDialog() {
    val updateController = koinInject<AppUpdateController>()
    val prompt by updateController.prompt.collectAsState()

    when (val currentPrompt = prompt) {
        null -> Unit

        is AppUpdatePrompt.OfferDownload -> UpdateChoiceDialog(
            title = stringResource(Res.string.desktop_update_available),
            message = stringResource(
                Res.string.desktop_update_available_message,
                currentPrompt.version,
                updateController.installedVersion.toString()
            ),
            confirmText = stringResource(Res.string.desktop_update_download),
            onConfirm = updateController::startDownload,
            onLater = updateController::dismissPrompt
        )

        is AppUpdatePrompt.OfferRestart -> UpdateChoiceDialog(
            title = stringResource(Res.string.desktop_update_ready_to_install),
            message = stringResource(Res.string.desktop_update_ready_message, currentPrompt.version),
            confirmText = stringResource(Res.string.desktop_update_restart),
            onConfirm = {
                updateController.dismissPrompt()
                restartApplication()
            },
            onLater = updateController::dismissPrompt
        )

        is AppUpdatePrompt.ReportFailure -> EmberrAlertDialog(
            onDismissRequest = updateController::dismissPrompt,
            title = stringResource(Res.string.desktop_update_failed)
        ) {
            UpdateDialogMessage(stringResource(currentPrompt.reason))
            Spacer(Modifier.height(20.dp))
            EmberrButtonPrimary(
                text = stringResource(Res.string.desktop_ok),
                onClick = updateController::dismissPrompt,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun UpdateChoiceDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onLater: () -> Unit
) {
    EmberrAlertDialog(onDismissRequest = onLater, title = title) {
        UpdateDialogMessage(message)
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EmberrButtonSecondary(
                text = stringResource(Res.string.desktop_update_later),
                onClick = onLater,
                modifier = Modifier.weight(1f)
            )
            EmberrButtonPrimary(
                text = confirmText,
                onClick = onConfirm,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UpdateDialogMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    )
}
