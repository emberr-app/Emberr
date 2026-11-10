package com.emberr.presentation.shared.editor.blockViews.credentials

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emberr.domain.credentials.CredentialUnlockSession
import com.emberr.domain.credentials.CredentialVault
import com.emberr.domain.model.CredentialField
import com.emberr.domain.model.CredentialFieldKind
import com.emberr.domain.util.system.DeviceLockPrompt
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.rememberDeviceLockPrompt
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.shared.components.emberrBorderWidth
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.SmallRoundedCornerShape
import com.emberr.presentation.shared.components.emberrBorderColor
import com.emberr.presentation.shared.components.subtleFillColor
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.check
import emberr.shared.generated.resources.cloud_hide_password
import emberr.shared.generated.resources.cloud_show_password
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.credentials_empty
import emberr.shared.generated.resources.credentials_locked_until_key_arrives
import emberr.shared.generated.resources.credentials_waiting_for_key
import emberr.shared.generated.resources.eye_closed
import emberr.shared.generated.resources.eye_open
import emberr.shared.generated.resources.lock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private const val PASSWORD_REVEAL_MILLIS = 20_000
private const val COPIED_CHECK_MILLIS = 1_300L
private const val HIDDEN_PASSWORD = "••••••••••••"
private val DesktopLabelWidth = 120.dp
private val DesktopRowMinHeight = 38.dp
private val RevealTimerSpace = 12.dp
private val RowActionGap = 8.dp
private val RevealTimerBottomGap = 8.dp

internal class CredentialSecretReader(
    private val credentialVault: CredentialVault,
    private val unlockSession: CredentialUnlockSession,
    private val deviceLockPrompt: DeviceLockPrompt
) {
    suspend fun readAfterUnlock(field: CredentialField): String? {
        if (!unlockSession.unlock(deviceLockPrompt)) return null
        return decryptOrShowLocked(field)
    }

    suspend fun readAllAfterUnlock(fields: List<CredentialField>): Map<String, String>? {
        val decryptedSecretsByFieldId = fields
            .filter { field -> field.encryptedValue != null }
            .associate { field -> field.id to (decryptOrShowLocked(field) ?: return null) }
        if (decryptedSecretsByFieldId.isNotEmpty() && !unlockSession.unlock(deviceLockPrompt)) return null
        return decryptedSecretsByFieldId
    }

    suspend fun readWithoutUnlock(field: CredentialField): String? = credentialVault.decrypt(field)

    private suspend fun decryptOrShowLocked(field: CredentialField): String? {
        val value = credentialVault.decrypt(field)
        if (value == null) showNativeToast(getString(Res.string.credentials_locked_until_key_arrives))
        return value
    }
}

@Composable
internal fun rememberCredentialSecretReader(): CredentialSecretReader {
    val credentialVault = koinInject<CredentialVault>()
    val unlockSession = koinInject<CredentialUnlockSession>()
    val deviceLockPrompt = rememberDeviceLockPrompt()
    return remember(credentialVault, unlockSession, deviceLockPrompt) {
        CredentialSecretReader(credentialVault, unlockSession, deviceLockPrompt)
    }
}

@Composable
internal fun CredentialFieldRows(
    fields: List<CredentialField>,
    lockedFieldIds: Set<String>,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit
) {
    val secretReader = rememberCredentialSecretReader()

    CredentialsDivider()
    Column(modifier = Modifier.fillMaxWidth().padding(if (isDesktopPlatform) 6.dp else 0.dp)) {
        fields.forEachIndexed { index, field ->
            key(field.id) {
                if (index > 0 && !isDesktopPlatform) CredentialsDivider(modifier = Modifier.padding(start = 16.dp))
                CredentialFieldRow(
                    field = field,
                    isLocked = field.id in lockedFieldIds,
                    secretReader = secretReader,
                    inSelectionMode = inSelectionMode,
                    onToggleSelection = onToggleSelection
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CredentialFieldRow(
    field: CredentialField,
    isLocked: Boolean,
    secretReader: CredentialSecretReader,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    var revealedPassword by remember(field.encryptedValue) { mutableStateOf<String?>(null) }
    var noteText by remember(field.encryptedValue) { mutableStateOf<String?>(null) }
    var isReadingSecret by remember { mutableStateOf(false) }
    var showsCopied by remember { mutableStateOf(false) }
    val copiedMessage = field.kind.copiedMessage()

    val isSecret = field.kind != CredentialFieldKind.USERNAME
    val hasValue = if (isSecret) field.encryptedValue != null else field.plainValue.isNotEmpty()
    val canUseValue = hasValue && !isLocked
    val isRevealed = revealedPassword != null
    val revealTimeLeft = remember(isRevealed) { Animatable(1f) }
    val revealTimerSpace by animateDpAsState(if (isRevealed) RevealTimerSpace else 0.dp)

    LaunchedEffect(isRevealed) {
        if (!isRevealed) return@LaunchedEffect
        launch {
            revealTimeLeft.animateTo(0f, tween(durationMillis = PASSWORD_REVEAL_MILLIS, easing = LinearEasing))
        }
        delay(PASSWORD_REVEAL_MILLIS.toLong())
        revealedPassword = null
    }

    LaunchedEffect(field.encryptedValue, isLocked) {
        if (field.kind != CredentialFieldKind.NOTE || field.encryptedValue == null || isLocked) return@LaunchedEffect
        noteText = secretReader.readWithoutUnlock(field)
    }

    LaunchedEffect(showsCopied) {
        if (!showsCopied) return@LaunchedEffect
        delay(COPIED_CHECK_MILLIS)
        showsCopied = false
    }

    fun copyToClipboard(value: String) {
        clipboardManager.setText(AnnotatedString(value))
        showsCopied = true
        showNativeToast(copiedMessage)
    }

    fun copyValue() {
        if (!canUseValue || isReadingSecret) return
        val valueOnScreen = when (field.kind) {
            CredentialFieldKind.USERNAME -> field.plainValue
            CredentialFieldKind.PASSWORD -> revealedPassword
            CredentialFieldKind.NOTE -> noteText
        }
        if (valueOnScreen != null) {
            copyToClipboard(valueOnScreen)
            return
        }
        isReadingSecret = true
        scope.launch {
            try {
                secretReader.readAfterUnlock(field)?.let { secret -> copyToClipboard(secret) }
            } finally {
                isReadingSecret = false
            }
        }
    }

    fun toggleReveal() {
        if (revealedPassword != null) {
            revealedPassword = null
            return
        }
        if (!canUseValue || isReadingSecret) return
        isReadingSecret = true
        scope.launch {
            try {
                revealedPassword = secretReader.readAfterUnlock(field)
            } finally {
                isReadingSecret = false
            }
        }
    }

    val valueModifier = Modifier.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = { if (inSelectionMode) onToggleSelection() else copyValue() },
        onLongClick = if (isDesktopPlatform) null else onToggleSelection
    )
    val showsActions = canUseValue && !inSelectionMode
    val actions = @Composable { modifier: Modifier ->
        CredentialRowActions(
            field = field,
            isRevealed = isRevealed,
            showsCopied = showsCopied,
            onToggleReveal = { toggleReveal() },
            onCopy = { copyValue() },
            modifier = modifier
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SmallRoundedCornerShape)
            .then(
                if (isDesktopPlatform) {
                    Modifier
                        .hoverable(hoverSource)
                        .background(if (isHovered) subtleFillColor() else Color.Transparent)
                } else {
                    Modifier
                }
            )
    ) {
        if (isDesktopPlatform) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = revealTimerSpace),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier.width(DesktopLabelWidth).heightIn(min = DesktopRowMinHeight),
                    contentAlignment = Alignment.CenterStart
                ) {
                    CredentialFieldLabel(field = field, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = DesktopRowMinHeight)
                        .then(valueModifier)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    CredentialFieldValue(field = field, revealedPassword = revealedPassword, noteText = noteText, isLocked = isLocked)
                }
                if (showsActions) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.heightIn(min = DesktopRowMinHeight), contentAlignment = Alignment.Center) {
                        actions(Modifier.alpha(if (isHovered || isRevealed || showsCopied) 1f else 0f))
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp + revealTimerSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    CredentialFieldLabel(field = field, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier.fillMaxWidth().then(valueModifier)) {
                        CredentialFieldValue(field = field, revealedPassword = revealedPassword, noteText = noteText, isLocked = isLocked)
                    }
                }
                if (showsActions) actions(Modifier)
            }
        }
        if (isRevealed) {
            val barColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = if (isDesktopPlatform) 8.dp else 16.dp)
                    .padding(bottom = RevealTimerBottomGap)
                    .height(2.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = barColor,
                            size = size.copy(width = size.width * revealTimeLeft.value),
                            cornerRadius = CornerRadius(size.height / 2)
                        )
                    }
            )
        }
    }
}

@Composable
private fun CredentialFieldLabel(field: CredentialField, style: TextStyle) {
    val kindLabel = field.kind.displayLabel()
    Text(
        text = shownCredentialLabel(field.label).ifBlank { kindLabel },
        style = style,
        color = MaterialTheme.colorScheme.outline,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun CredentialsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = emberrBorderWidth, color = emberrBorderColor)
}

@Composable
private fun CredentialFieldValue(field: CredentialField, revealedPassword: String?, noteText: String?, isLocked: Boolean) {
    val bodyLarge = MaterialTheme.typography.bodyLarge
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    when {
        isLocked -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(Res.drawable.lock),
                contentDescription = null,
                tint = outline,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(Res.string.credentials_waiting_for_key), style = bodyLarge, color = outline)
        }
        field.kind == CredentialFieldKind.USERNAME && field.plainValue.isNotEmpty() -> Text(
            text = field.plainValue,
            style = bodyLarge,
            color = onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        field.kind == CredentialFieldKind.USERNAME || field.encryptedValue == null ->
            Text(text = stringResource(Res.string.credentials_empty), style = bodyLarge, color = outline)
        field.kind == CredentialFieldKind.NOTE -> Text(text = noteText.orEmpty(), style = bodyLarge, color = onSurface)
        revealedPassword != null -> Text(
            text = revealedPassword,
            style = bodyLarge.copy(fontFamily = FontFamily.Monospace),
            color = onSurface
        )
        else -> Text(text = HIDDEN_PASSWORD, style = bodyLarge.copy(letterSpacing = 2.sp), color = outline, maxLines = 1)
    }
}

@Composable
private fun CredentialRowActions(
    field: CredentialField,
    isRevealed: Boolean,
    showsCopied: Boolean,
    onToggleReveal: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (isDesktopPlatform) RowActionGap else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (field.kind == CredentialFieldKind.PASSWORD) {
            CredentialIconButton(
                painter = painterResource(if (isRevealed) Res.drawable.eye_closed else Res.drawable.eye_open),
                description = if (isRevealed) stringResource(Res.string.cloud_hide_password) else stringResource(Res.string.cloud_show_password),
                onClick = onToggleReveal,
                showsRipple = false
            )
        }
        CredentialIconButton(
            painter = painterResource(if (showsCopied) Res.drawable.check else Res.drawable.copy),
            description = field.kind.copyDescription(),
            tint = if (showsCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            onClick = onCopy,
            showsRipple = false
        )
    }
}

@Composable
internal fun CredentialIconButton(
    painter: Painter,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.outline,
    showsRipple: Boolean = true
) {
    IconTooltip(text = description) {
        Box(
            modifier = modifier
                .size(if (isDesktopPlatform) 28.dp else 40.dp)
                .clip(CircleShape)
                .then(
                    if (showsRipple) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier.clickable(interactionSource = null, indication = null, onClick = onClick)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painter,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(if (isDesktopPlatform) 16.dp else 18.dp)
            )
        }
    }
}
