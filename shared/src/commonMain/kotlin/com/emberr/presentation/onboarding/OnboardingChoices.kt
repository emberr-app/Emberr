package com.emberr.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emberr.domain.util.system.AppPermission
import com.emberr.domain.util.system.rememberAppPermissionCoordinator
import com.emberr.presentation.settings.label
import com.emberr.presentation.shared.SubNoteOpenMode
import com.emberr.ui.theme.FontSizePreference
import com.emberr.ui.theme.FontStylePreference
import com.emberr.ui.theme.fontFamilyFor
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.check
import org.jetbrains.compose.resources.painterResource
import emberr.shared.generated.resources.onboarding_allow
import emberr.shared.generated.resources.onboarding_allowed
import emberr.shared.generated.resources.onboarding_chat_answer
import emberr.shared.generated.resources.onboarding_chat_placeholder
import emberr.shared.generated.resources.onboarding_chat_question
import emberr.shared.generated.resources.onboarding_enable_ai
import emberr.shared.generated.resources.onboarding_enable_ai_description
import emberr.shared.generated.resources.onboarding_permission_alarms
import emberr.shared.generated.resources.onboarding_permission_alarms_description
import emberr.shared.generated.resources.onboarding_permission_camera
import emberr.shared.generated.resources.onboarding_permission_camera_description
import emberr.shared.generated.resources.onboarding_permission_microphone
import emberr.shared.generated.resources.onboarding_permission_microphone_description
import emberr.shared.generated.resources.onboarding_permission_notifications
import emberr.shared.generated.resources.onboarding_permission_notifications_description
import emberr.shared.generated.resources.onboarding_show_scrollbars
import emberr.shared.generated.resources.onboarding_show_scrollbars_description
import emberr.shared.generated.resources.settings_font_size_default
import emberr.shared.generated.resources.settings_font_size_extra_large
import emberr.shared.generated.resources.settings_font_size_extra_small
import emberr.shared.generated.resources.settings_font_size_large
import emberr.shared.generated.resources.settings_font_size_small
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingChoicePanel(
    choice: OnboardingChoice,
    viewModel: OnboardingViewModel,
    modifier: Modifier = Modifier
) {
    OnboardingPanelTransition(targetState = choice, modifier = modifier) { visibleChoice ->
        when (visibleChoice) {
            OnboardingChoice.NONE -> Spacer(modifier = Modifier.fillMaxWidth())
            OnboardingChoice.TYPEFACE -> TypefaceChoice(viewModel)
            OnboardingChoice.TEXT_SIZE -> TextSizeChoice(viewModel)
            OnboardingChoice.AI_ASSISTANT -> AiAssistantChoice(viewModel)
            OnboardingChoice.DESKTOP_LAYOUT -> DesktopLayoutChoice(viewModel)
            OnboardingChoice.SCROLLBARS -> ScrollbarsChoice(viewModel)
            OnboardingChoice.PERMISSIONS -> PermissionsChoice()
        }
    }
}

@Composable
private fun TypefaceChoice(viewModel: OnboardingViewModel) {
    val fontStyleName by viewModel.fontStylePreference.collectAsState()
    val selectedFontStyle = runCatching { FontStylePreference.valueOf(fontStyleName) }
        .getOrDefault(FontStylePreference.POPPINS)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        FontStylePreference.entries.forEach { fontStyle ->
            ChoiceOptionRow(
                label = fontStyle.label(),
                isSelected = fontStyle == selectedFontStyle,
                fontFamily = fontFamilyFor(fontStyle),
                onClick = { viewModel.setFontStylePreference(fontStyle.name) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextSizeChoice(viewModel: OnboardingViewModel) {
    val fontSizeName by viewModel.fontSizePreference.collectAsState()
    val selectedFontSize = runCatching { FontSizePreference.valueOf(fontSizeName) }
        .getOrDefault(FontSizePreference.DEFAULT)

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (LocalOnboardingWideLayout.current) {
            Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally)
        } else {
            Arrangement.spacedBy(30.dp)
        }
    ) {
        FontSizePreference.entries.forEach { sizePreference ->
            val isSelected = sizePreference == selectedFontSize
            val labelColor by animateColorAsState(
                targetValue = optionColorFor(isSelected),
                animationSpec = tween(220),
                label = "onboarding-text-size-color"
            )

            Text(
                text = sizePreference.displayLabel(),
                fontSize = sizePreference.previewTextSize(),
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = labelColor,
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { viewModel.setFontSizePreference(sizePreference.name) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun AiAssistantChoice(viewModel: OnboardingViewModel) {
    val aiFeaturesDisabled by viewModel.aiFeaturesDisabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        OnboardingChatMock(
            question = stringResource(Res.string.onboarding_chat_question),
            answer = stringResource(Res.string.onboarding_chat_answer),
            placeholder = stringResource(Res.string.onboarding_chat_placeholder)
        )

        Spacer(modifier = Modifier.height(24.dp))

        ChoiceSwitchRow(
            label = stringResource(Res.string.onboarding_enable_ai),
            description = stringResource(Res.string.onboarding_enable_ai_description),
            checked = !aiFeaturesDisabled,
            onCheckedChange = { isEnabled -> viewModel.setAiFeaturesDisabled(!isEnabled) }
        )
    }
}

@Composable
private fun DesktopLayoutChoice(viewModel: OnboardingViewModel) {
    val subNoteOpenModeName by viewModel.subNoteOpenMode.collectAsState()

    val selectedOpenMode = runCatching { SubNoteOpenMode.valueOf(subNoteOpenModeName) }
        .getOrDefault(SubNoteOpenMode.SIDE_PANEL)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        OnboardingWindowPreview(openMode = selectedOpenMode)

        Spacer(modifier = Modifier.height(22.dp))

        SubNoteOpenMode.entries.forEach { openMode ->
            ChoiceOptionRow(
                label = openMode.label(),
                isSelected = openMode == selectedOpenMode,
                onClick = { viewModel.setSubNoteOpenMode(openMode.name) }
            )
        }
    }
}

@Composable
private fun ScrollbarsChoice(viewModel: OnboardingViewModel) {
    val showScrollbar by viewModel.showScrollbar.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        ChoiceSwitchRow(
            label = stringResource(Res.string.onboarding_show_scrollbars),
            description = stringResource(Res.string.onboarding_show_scrollbars_description),
            checked = showScrollbar,
            onCheckedChange = viewModel::setShowScrollbar
        )
    }
}

private data class OnboardingPermissionRequest(
    val permission: AppPermission,
    val title: StringResource,
    val description: StringResource
)

@Composable
private fun PermissionsChoice() {
    val permissionCoordinator = rememberAppPermissionCoordinator()

    val permissionRequests = remember {
        listOf(
            OnboardingPermissionRequest(
                permission = AppPermission.Notifications,
                title = Res.string.onboarding_permission_notifications,
                description = Res.string.onboarding_permission_notifications_description
            ),
            OnboardingPermissionRequest(
                permission = AppPermission.ExactAlarms,
                title = Res.string.onboarding_permission_alarms,
                description = Res.string.onboarding_permission_alarms_description
            ),
            OnboardingPermissionRequest(
                permission = AppPermission.Microphone,
                title = Res.string.onboarding_permission_microphone,
                description = Res.string.onboarding_permission_microphone_description
            ),
            OnboardingPermissionRequest(
                permission = AppPermission.Camera,
                title = Res.string.onboarding_permission_camera,
                description = Res.string.onboarding_permission_camera_description
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        permissionRequests.forEach { permissionRequest ->
            PermissionRow(
                title = stringResource(permissionRequest.title),
                description = stringResource(permissionRequest.description),
                isGranted = permissionCoordinator.isGranted(permissionRequest.permission),
                onRequest = { permissionCoordinator.request(permissionRequest.permission) }
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isGranted,
                onClick = onRequest
            )
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Crossfade(
            targetState = isGranted,
            animationSpec = tween(240),
            label = "onboarding-permission-state"
        ) { isCurrentlyGranted ->
            if (isCurrentlyGranted) {
                Icon(
                    painter = painterResource(Res.drawable.check),
                    contentDescription = stringResource(Res.string.onboarding_allowed),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = stringResource(Res.string.onboarding_allow),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun ChoiceOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    fontFamily: FontFamily? = null
) {
    val contentColor by animateColorAsState(
        targetValue = optionColorFor(isSelected),
        animationSpec = tween(220),
        label = "onboarding-option-color"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = fontFamily,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.6f),
            exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.6f)
        ) {
            Icon(
                painter = painterResource(Res.drawable.check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun ChoiceSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
            )
        }

        Spacer(modifier = Modifier.width(18.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun optionColorFor(isSelected: Boolean): Color = if (isSelected) {
    MaterialTheme.colorScheme.onBackground
} else {
    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
}

@Composable
private fun FontSizePreference.displayLabel(): String = stringResource(
    when (this) {
        FontSizePreference.EXTRA_SMALL -> Res.string.settings_font_size_extra_small
        FontSizePreference.SMALL -> Res.string.settings_font_size_small
        FontSizePreference.DEFAULT -> Res.string.settings_font_size_default
        FontSizePreference.LARGE -> Res.string.settings_font_size_large
        FontSizePreference.EXTRA_LARGE -> Res.string.settings_font_size_extra_large
    }
)

private fun FontSizePreference.previewTextSize(): TextUnit = when (this) {
    FontSizePreference.EXTRA_SMALL -> 13.sp
    FontSizePreference.SMALL -> 15.sp
    FontSizePreference.DEFAULT -> 19.sp
    FontSizePreference.LARGE -> 24.sp
    FontSizePreference.EXTRA_LARGE -> 29.sp
}
