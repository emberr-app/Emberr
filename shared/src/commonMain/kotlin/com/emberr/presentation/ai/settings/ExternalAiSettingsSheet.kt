package com.emberr.presentation.ai.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.components.EmberrTextFieldIconButton
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.domain.ai.external.ExternalAiProvider
import com.emberr.domain.ai.external.ExternalAiProviderConfig
import com.emberr.presentation.ai.RagViewModel
import com.emberr.presentation.ai.components.clickableWithoutMobileRipple
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrShadowElevation
import com.emberr.presentation.shared.components.EmberrTextField
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.eye_closed
import emberr.shared.generated.resources.eye_open
import kotlin.time.Clock
import org.jetbrains.compose.resources.painterResource
import emberr.shared.generated.resources.ai_api_key
import emberr.shared.generated.resources.ai_api_key_saved
import emberr.shared.generated.resources.ai_base_url
import emberr.shared.generated.resources.ai_delete_api_key_message
import emberr.shared.generated.resources.ai_delete_api_key_question
import emberr.shared.generated.resources.ai_enter_model_name
import emberr.shared.generated.resources.ai_enter_provider_api_key
import emberr.shared.generated.resources.ai_external_ai
import emberr.shared.generated.resources.ai_external_ai_sheet_subtitle
import emberr.shared.generated.resources.ai_hide_api_key
import emberr.shared.generated.resources.ai_model
import emberr.shared.generated.resources.ai_provider
import emberr.shared.generated.resources.ai_show_api_key
import emberr.shared.generated.resources.common_cancel
import emberr.shared.generated.resources.common_close
import emberr.shared.generated.resources.common_delete
import emberr.shared.generated.resources.common_save
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ExternalAiSettingsSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    viewModel: RagViewModel
) {
    EmberrBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = stringResource(Res.string.ai_external_ai),
        subtitle = stringResource(Res.string.ai_external_ai_sheet_subtitle),
    ) { closeAnd ->
        var isReady by remember { mutableStateOf(false) }
        var selectedProvider by remember { mutableStateOf(ExternalAiProvider.OPENAI) }
        var loadedConfig by remember { mutableStateOf<ExternalAiProviderConfig?>(null) }
        var apiKeyInput by remember { mutableStateOf("") }
        var modelInput by remember { mutableStateOf("") }
        var baseUrlInput by remember { mutableStateOf("") }
        var isApiKeyVisible by remember { mutableStateOf(false) }
        var showDeleteConfirm by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            selectedProvider = viewModel.selectedExternalAiProvider.value
            isReady = true
        }

        LaunchedEffect(selectedProvider, isReady) {
            if (!isReady) return@LaunchedEffect
            val config = viewModel.getExternalAiConfig(selectedProvider)
            loadedConfig = config
            modelInput = config?.model.orEmpty()
            baseUrlInput = config?.baseUrl.orEmpty()
            apiKeyInput = ""
        }

        val hasUsableKey = apiKeyInput.isNotBlank() || loadedConfig?.apiKey?.isNotBlank() == true
        val hasUsableEndpoint = selectedProvider != ExternalAiProvider.CUSTOM || baseUrlInput.isNotBlank()
        val onSave: () -> Unit = {
            if (hasUsableKey && hasUsableEndpoint) {
                val configToSave = ExternalAiProviderConfig(
                    apiKey = apiKeyInput.ifBlank { loadedConfig?.apiKey.orEmpty() },
                    model = modelInput.ifBlank { selectedProvider.defaultModel.orEmpty() },
                    baseUrl = baseUrlInput.ifBlank { null },
                    updatedAt = Clock.System.now().toEpochMilliseconds()
                )
                viewModel.saveExternalAiConfig(selectedProvider, configToSave)
                closeAnd { }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                text = stringResource(Res.string.ai_provider),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ExternalAiProvider.entries.forEach { provider ->
                    ProviderChip(
                        label = provider.label(),
                        selected = provider == selectedProvider,
                        onClick = { selectedProvider = provider }
                    )
                }
            }

            if (selectedProvider == ExternalAiProvider.CUSTOM) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(Res.string.ai_base_url),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                EmberrTextField(
                    value = baseUrlInput,
                    onValueChange = { baseUrlInput = it },
                    placeholder = "https://your-endpoint.example.com/v1",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    onSubmit = onSave
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.ai_model),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(8.dp))
            EmberrTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                placeholder = selectedProvider.defaultModel ?: stringResource(Res.string.ai_enter_model_name),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                onSubmit = onSave
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.ai_api_key),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(8.dp))
            EmberrTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                placeholder = if (loadedConfig?.apiKey?.isNotBlank() == true)
                    stringResource(Res.string.ai_api_key_saved)
                else
                    stringResource(Res.string.ai_enter_provider_api_key, selectedProvider.label()),
                singleLine = true,
                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconTooltip(text = stringResource(if (isApiKeyVisible) Res.string.ai_hide_api_key else Res.string.ai_show_api_key)) {
                        EmberrTextFieldIconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                painter = painterResource(if (isApiKeyVisible) Res.drawable.eye_closed else Res.drawable.eye_open),
                                contentDescription = stringResource(if (isApiKeyVisible) Res.string.ai_hide_api_key else Res.string.ai_show_api_key),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                onSubmit = onSave
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (loadedConfig != null) {
                    EmberrButtonSecondary(
                        text = stringResource(Res.string.common_delete),
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                EmberrButtonPrimary(
                    text = stringResource(Res.string.common_save),
                    enabled = hasUsableKey && hasUsableEndpoint,
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                )
            }

            EmberrButtonPrimary(
                text = stringResource(Res.string.common_close),
                onClick = { closeAnd { } },
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            if (showDeleteConfirm) {
                EmberrAlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = stringResource(Res.string.ai_delete_api_key_question)
                ) {
                    Text(
                        text = stringResource(Res.string.ai_delete_api_key_message),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EmberrButtonSecondary(
                            text = stringResource(Res.string.common_cancel),
                            onClick = { showDeleteConfirm = false },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                viewModel.deleteExternalAiConfig(selectedProvider)
                                loadedConfig = null
                                apiKeyInput = ""
                                modelInput = ""
                                baseUrlInput = ""
                                showDeleteConfirm = false
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = EmberrShadowElevation.None)
                        ) {
                            Text(stringResource(Res.string.common_delete), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = EmberrShadowElevation.None,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickableWithoutMobileRipple(onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}
