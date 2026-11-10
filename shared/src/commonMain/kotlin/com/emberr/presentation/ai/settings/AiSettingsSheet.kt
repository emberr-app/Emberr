package com.emberr.presentation.ai.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emberr.domain.ai.AiGenerationMode
import com.emberr.presentation.ai.RagViewModel
import com.emberr.presentation.ai.components.ModelOptionCard
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.ai_external_ai
import emberr.shared.generated.resources.ai_external_ai_active
import emberr.shared.generated.resources.ai_external_ai_subtitle
import emberr.shared.generated.resources.ai_fine_tuning
import emberr.shared.generated.resources.ai_local_ai
import emberr.shared.generated.resources.ai_local_ai_subtitle
import emberr.shared.generated.resources.ai_not_supported_by_hardware
import emberr.shared.generated.resources.ai_settings_title
import emberr.shared.generated.resources.common_close
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AiSettingsSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    viewModel: RagViewModel,
    onLocalAiClick: () -> Unit,
    onExternalAiClick: () -> Unit,
    onFineTuningClick: () -> Unit
) {
    val aiGenerationMode by viewModel.aiGenerationMode.collectAsState()
    val selectedExternalAiProvider by viewModel.selectedExternalAiProvider.collectAsState()
    val knowledgeMode by viewModel.knowledgeMode.collectAsState()

    EmberrBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = stringResource(Res.string.ai_settings_title),
    ) { _ ->

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            ModelOptionCard(
                title = stringResource(Res.string.ai_local_ai),
                subtitle = if (viewModel.localAiUnsupportedReason == null)
                    stringResource(Res.string.ai_local_ai_subtitle)
                else
                    stringResource(Res.string.ai_not_supported_by_hardware),
                onClick = onLocalAiClick
            )
            Spacer(Modifier.height(10.dp))
            ModelOptionCard(
                title = stringResource(Res.string.ai_external_ai),
                subtitle = if (aiGenerationMode == AiGenerationMode.EXTERNAL)
                    stringResource(Res.string.ai_external_ai_active, selectedExternalAiProvider.label())
                else
                    stringResource(Res.string.ai_external_ai_subtitle),
                onClick = onExternalAiClick
            )
            Spacer(Modifier.height(10.dp))
            ModelOptionCard(
                title = stringResource(Res.string.ai_fine_tuning),
                subtitle = knowledgeMode.label(),
                onClick = onFineTuningClick
            )

            EmberrButtonPrimary(
                text = stringResource(Res.string.common_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }
    }
}
