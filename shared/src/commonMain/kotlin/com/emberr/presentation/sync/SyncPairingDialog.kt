package com.emberr.presentation.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emberr.domain.sync.SyncPairingData
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.common_close
import emberr.shared.generated.resources.lan_sync_pair_dialog_message
import emberr.shared.generated.resources.lan_sync_pair_dialog_secret_token
import emberr.shared.generated.resources.lan_sync_pair_dialog_title
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource

@Composable
fun SyncPairingDialog(
    pairingData: SyncPairingData?,
    onDismiss: () -> Unit
) {
    if (pairingData == null) return

    val jsonString = remember(pairingData) {
        Json.encodeToString(pairingData)
    }

    EmberrAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.lan_sync_pair_dialog_title)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.lan_sync_pair_dialog_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                QrCodeDisplay(
                    data = jsonString,
                    size = 180,
                    modifier = Modifier
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.lan_sync_pair_dialog_secret_token, pairingData.authToken.take(8)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            EmberrButtonPrimary(
                text = stringResource(Res.string.common_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
