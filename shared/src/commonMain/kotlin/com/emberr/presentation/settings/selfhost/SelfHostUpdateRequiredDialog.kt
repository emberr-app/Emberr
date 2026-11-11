package com.emberr.presentation.settings.selfhost

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
import com.emberr.domain.selfhost.sync.SelfHostUpdateRequiredSignal
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.selfhost_later
import emberr.shared.generated.resources.selfhost_update
import emberr.shared.generated.resources.selfhost_update_required_message
import emberr.shared.generated.resources.selfhost_update_required_title
import org.jetbrains.compose.resources.stringResource
import emberr.shared.generated.resources.message_selfhost_update_required

@Composable
fun SelfHostUpdateRequiredDialog(onUpdateClick: () -> Unit) {
    val shouldShowDialog by SelfHostUpdateRequiredSignal.shouldShowDialog.collectAsState()
    if (!shouldShowDialog) return

    EmberrAlertDialog(
        onDismissRequest = SelfHostUpdateRequiredSignal::dismissDialog,
        title = stringResource(Res.string.selfhost_update_required_title)
    ) {
        Text(
            text = stringResource(Res.string.selfhost_update_required_message, stringResource(Res.string.message_selfhost_update_required)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EmberrButtonSecondary(
                text = stringResource(Res.string.selfhost_later),
                onClick = SelfHostUpdateRequiredSignal::dismissDialog,
                modifier = Modifier.weight(1f)
            )
            EmberrButtonPrimary(
                text = stringResource(Res.string.selfhost_update),
                onClick = {
                    SelfHostUpdateRequiredSignal.dismissDialog()
                    onUpdateClick()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
