package com.emberr.presentation.ai.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.domain.ai.LocalAiUnsupportedReason
import com.emberr.domain.ai.models.ModelDownloadProgress
import com.emberr.presentation.ai.components.ModelOptionCard
import com.emberr.presentation.ai.components.UnsupportedHardwareNotice
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.ai_connect_cloud_provider
import emberr.shared.generated.resources.ai_connect_cloud_provider_subtitle
import emberr.shared.generated.resources.ai_download_stopped_tap_to_retry
import emberr.shared.generated.resources.ai_downloading_tap_to_manage
import emberr.shared.generated.resources.ai_no_ai_connected
import emberr.shared.generated.resources.ai_paused_tap_to_resume
import emberr.shared.generated.resources.ai_private_and_offline
import emberr.shared.generated.resources.ai_run_on_this_device
import emberr.shared.generated.resources.ai_setting_up_your_ai
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ModelUnavailablePrompt(
    sidePadding: Dp,
    downloadProgress: ModelDownloadProgress?,
    localAiUnsupportedReason: LocalAiUnsupportedReason?,
    onDownloadClick: () -> Unit,
    onApiKeyClick: () -> Unit
) {
    val isDownloading = downloadProgress is ModelDownloadProgress.Downloading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = sidePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(if (isDownloading) Res.string.ai_setting_up_your_ai else Res.string.ai_no_ai_connected),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        if (localAiUnsupportedReason == null) {
            ModelOptionCard(
                title = stringResource(Res.string.ai_run_on_this_device),
                subtitle = when (downloadProgress) {
                    is ModelDownloadProgress.Downloading ->
                        stringResource(Res.string.ai_downloading_tap_to_manage, (downloadProgress.fraction * 100).toInt())
                    is ModelDownloadProgress.Failed -> stringResource(Res.string.ai_download_stopped_tap_to_retry)
                    ModelDownloadProgress.Paused -> stringResource(Res.string.ai_paused_tap_to_resume)
                    else -> stringResource(Res.string.ai_private_and_offline)
                },
                onClick = onDownloadClick,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            )
        } else {
            UnsupportedHardwareNotice(reason = localAiUnsupportedReason)
        }
        Spacer(Modifier.height(10.dp))

        ModelOptionCard(
            title = stringResource(Res.string.ai_connect_cloud_provider),
            subtitle = stringResource(Res.string.ai_connect_cloud_provider_subtitle),
            onClick = onApiKeyClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}
