package com.emberr.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.emberrBorder
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.logo_google_play
import emberr.shared.generated.resources.settings_play_testing_dialog_reason
import emberr.shared.generated.resources.settings_play_testing_dialog_title
import emberr.shared.generated.resources.settings_play_testing_join
import emberr.shared.generated.resources.settings_play_testing_not_now
import emberr.shared.generated.resources.settings_play_testing_no_data_lost
import emberr.shared.generated.resources.settings_play_testing_step_install
import emberr.shared.generated.resources.settings_play_testing_step_join
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal const val PLAY_TESTING_GROUP_URL = "https://groups.google.com/g/emberr-testers"

@Composable
expect fun PlayTestingInvite(isDueThisLaunch: Boolean)

@Composable
expect fun PlayTestingSettingsRow()

@Composable
internal fun PlayTestingDialog(onJoinClick: () -> Unit, onDismissRequest: () -> Unit) {
    EmberrAlertDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.settings_play_testing_dialog_title),
        icon = painterResource(Res.drawable.logo_google_play)
    ) {
        Text(
            text = stringResource(Res.string.settings_play_testing_dialog_reason),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            PlayTestingStep(number = 1, text = stringResource(Res.string.settings_play_testing_step_join))
            PlayTestingStep(number = 2, text = stringResource(Res.string.settings_play_testing_step_install))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.settings_play_testing_no_data_lost),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EmberrButtonSecondary(
                text = stringResource(Res.string.settings_play_testing_not_now),
                onClick = onDismissRequest,
                modifier = Modifier.weight(1f)
            )
            EmberrButtonPrimary(
                text = stringResource(Res.string.settings_play_testing_join),
                onClick = onJoinClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PlayTestingStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .emberrBorder(CircleShape)
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.size(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
