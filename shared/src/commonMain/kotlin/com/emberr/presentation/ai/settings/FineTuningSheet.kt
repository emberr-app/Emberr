package com.emberr.presentation.ai.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emberr.domain.ai.KnowledgeMode
import com.emberr.presentation.ai.RagViewModel
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.ai_fine_tuning
import emberr.shared.generated.resources.ai_knowledge_default_subtitle
import emberr.shared.generated.resources.ai_knowledge_notes_only_subtitle
import emberr.shared.generated.resources.ai_knowledge_source
import emberr.shared.generated.resources.ai_knowledge_world_only_subtitle
import emberr.shared.generated.resources.ai_response_length
import emberr.shared.generated.resources.common_close
import org.jetbrains.compose.resources.stringResource

private val SectionLabelHorizontalPadding = 20.dp

@Composable
internal fun FineTuningSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    viewModel: RagViewModel
) {
    val selectedMode by viewModel.knowledgeMode.collectAsState()
    val selectedMaxOutputTokens by viewModel.maxOutputTokens.collectAsState()

    EmberrBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = stringResource(Res.string.ai_fine_tuning),
        contentHorizontalPadding = 0.dp
    ) { closeAnd ->

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = stringResource(Res.string.ai_knowledge_source),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = SectionLabelHorizontalPadding)
            )
            Spacer(Modifier.height(8.dp))
            EmberrBottomSheetOption(
                label = KnowledgeMode.DEFAULT.label(),
                subtitle = stringResource(Res.string.ai_knowledge_default_subtitle),
                isSelected = selectedMode == KnowledgeMode.DEFAULT,
                onClick = { closeAnd { viewModel.selectKnowledgeMode(KnowledgeMode.DEFAULT) } }
            )
            EmberrBottomSheetOption(
                label = KnowledgeMode.NOTES_ONLY.label(),
                subtitle = stringResource(Res.string.ai_knowledge_notes_only_subtitle),
                isSelected = selectedMode == KnowledgeMode.NOTES_ONLY,
                onClick = { closeAnd { viewModel.selectKnowledgeMode(KnowledgeMode.NOTES_ONLY) } }
            )
            EmberrBottomSheetOption(
                label = KnowledgeMode.WORLD_ONLY.label(),
                subtitle = stringResource(Res.string.ai_knowledge_world_only_subtitle),
                isSelected = selectedMode == KnowledgeMode.WORLD_ONLY,
                onClick = { closeAnd { viewModel.selectKnowledgeMode(KnowledgeMode.WORLD_ONLY) } }
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.ai_response_length),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = SectionLabelHorizontalPadding)
            )
            Spacer(Modifier.height(8.dp))
            responseLengthOptions.forEach { option ->
                EmberrBottomSheetOption(
                    label = stringResource(option.label),
                    subtitle = stringResource(option.subtitle),
                    isSelected = selectedMaxOutputTokens == option.tokens,
                    onClick = { closeAnd { viewModel.selectMaxOutputTokens(option.tokens) } }
                )
            }

            EmberrButtonPrimary(
                text = stringResource(Res.string.common_close),
                onClick = { closeAnd { } },
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 12.dp, start = 20.dp, end = 20.dp)
            )
        }
    }
}
