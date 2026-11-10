package com.emberr.presentation.ai.settings

import androidx.compose.runtime.Composable
import com.emberr.domain.ai.KnowledgeMode
import com.emberr.domain.ai.external.ExternalAiProvider
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.ai_knowledge_default
import emberr.shared.generated.resources.ai_knowledge_notes_only
import emberr.shared.generated.resources.ai_knowledge_world_only
import emberr.shared.generated.resources.ai_provider_custom
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun KnowledgeMode.label(): String = stringResource(
    when (this) {
        KnowledgeMode.DEFAULT -> Res.string.ai_knowledge_default
        KnowledgeMode.NOTES_ONLY -> Res.string.ai_knowledge_notes_only
        KnowledgeMode.WORLD_ONLY -> Res.string.ai_knowledge_world_only
    }
)

@Composable
internal fun ExternalAiProvider.label(): String =
    if (this == ExternalAiProvider.CUSTOM) stringResource(Res.string.ai_provider_custom) else displayName
