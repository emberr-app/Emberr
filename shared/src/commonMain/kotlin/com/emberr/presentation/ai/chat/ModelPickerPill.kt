package com.emberr.presentation.ai.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.domain.ai.AiGenerationMode
import com.emberr.domain.ai.external.ExternalAiProvider
import com.emberr.domain.ai.external.ExternalAiProviderConfig
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.ai.RagViewModel
import com.emberr.presentation.ai.settings.label
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.EmberrShadowElevation
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.ai_choose_ai_model
import emberr.shared.generated.resources.ai_choose_ai_model_title
import emberr.shared.generated.resources.ai_model_local
import emberr.shared.generated.resources.ai_model_unavailable
import emberr.shared.generated.resources.common_close
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ModelPickerPill(viewModel: RagViewModel) {
    val aiGenerationMode by viewModel.aiGenerationMode.collectAsState()
    val selectedExternalAiProvider by viewModel.selectedExternalAiProvider.collectAsState()
    val installedLocalModels by viewModel.installedLocalModels.collectAsState()
    val selectedLocalModelFileName by viewModel.selectedLocalModelFileName.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    val selectableLocalModels = if (viewModel.localAiUnsupportedReason == null) {
        installedLocalModels
    } else {
        emptyList()
    }

    var selectedExternalModelName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedExternalAiProvider, aiGenerationMode) {
        selectedExternalModelName = viewModel.getExternalAiConfig(selectedExternalAiProvider)?.model
    }

    val label = if (aiGenerationMode == AiGenerationMode.LOCAL) {
        selectableLocalModels.find { it.fileName == selectedLocalModelFileName }?.displayName
            ?: stringResource(if (viewModel.localAiUnsupportedReason == null) Res.string.ai_model_local else Res.string.ai_model_unavailable)
    } else {
        selectedExternalModelName?.takeIf { it.isNotBlank() } ?: selectedExternalAiProvider.label()
    }

    val externalConfigs = remember { mutableStateMapOf<ExternalAiProvider, ExternalAiProviderConfig?>() }
    var configsLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(showPicker) {
        if (!showPicker) return@LaunchedEffect
        viewModel.refreshInstalledLocalModels()
        ExternalAiProvider.entries.forEach { provider ->
            externalConfigs[provider] = viewModel.getExternalAiConfig(provider)
        }
        configsLoaded = true
    }

    Box {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = EmberrShadowElevation.None,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { showPicker = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 110.dp)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(Res.string.ai_choose_ai_model),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (isDesktopPlatform) {
            EmberrDesktopMenu(
                expanded = showPicker,
                onDismissRequest = { showPicker = false }
            ) {
                Column(modifier = Modifier.width(240.dp).padding(vertical = 4.dp)) {
                    selectableLocalModels.forEach { model ->
                        EmberrDesktopMenuOption(
                            label = model.displayName,
                            isSelected = aiGenerationMode == AiGenerationMode.LOCAL &&
                                    selectedLocalModelFileName == model.fileName,
                            onClick = { showPicker = false; viewModel.selectLocalModel(model.fileName) }
                        )
                    }
                    if (configsLoaded) {
                        ExternalAiProvider.entries.forEach { provider ->
                            val config = externalConfigs[provider]
                            val isConfigured = !config?.apiKey.isNullOrBlank()
                            if (isConfigured) {
                                EmberrDesktopMenuOption(
                                    label = config.model.takeIf { it.isNotBlank() } ?: provider.label(),
                                    isSelected = aiGenerationMode == AiGenerationMode.EXTERNAL &&
                                            selectedExternalAiProvider == provider,
                                    onClick = { showPicker = false; viewModel.selectExternalProvider(provider) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (!isDesktopPlatform) {
        EmberrBottomSheet(
            expanded = showPicker,
            onDismiss = { showPicker = false },
            title = stringResource(Res.string.ai_choose_ai_model_title),
            contentHorizontalPadding = 0.dp
        ) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                selectableLocalModels.forEach { model ->
                    val isSelectedModel = aiGenerationMode == AiGenerationMode.LOCAL &&
                            selectedLocalModelFileName == model.fileName
                    EmberrBottomSheetOption(
                        label = model.displayName,
                        labelMaxLines = 1,
                        isSelected = isSelectedModel,
                        onClick = { closeAnd { viewModel.selectLocalModel(model.fileName) } }
                    )
                }

                if (configsLoaded) {
                    ExternalAiProvider.entries.forEach { provider ->
                        val config = externalConfigs[provider]
                        val isConfigured = !config?.apiKey.isNullOrBlank()
                        if (isConfigured) {
                            val isSelectedProvider = aiGenerationMode == AiGenerationMode.EXTERNAL &&
                                    selectedExternalAiProvider == provider
                            EmberrBottomSheetOption(
                                label = config.model.takeIf { it.isNotBlank() } ?: provider.label(),
                                labelMaxLines = 1,
                                isSelected = isSelectedProvider,
                                onClick = { closeAnd { viewModel.selectExternalProvider(provider) } }
                            )
                        }
                    }
                }

                EmberrButtonPrimary(
                    text = stringResource(Res.string.common_close),
                    onClick = { showPicker = false },
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                )
            }
        }
    }
}
