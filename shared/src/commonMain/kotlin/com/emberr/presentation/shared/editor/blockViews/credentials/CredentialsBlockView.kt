package com.emberr.presentation.shared.editor.blockViews.credentials

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.domain.credentials.CredentialUnlockSession
import com.emberr.domain.credentials.CredentialVault
import com.emberr.domain.model.CredentialsBlock
import com.emberr.domain.model.hasNothingFilledIn
import com.emberr.domain.model.shortDateText
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.rememberDeviceLockPrompt
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.emberrBorderColor
import com.emberr.presentation.shared.editor.DefaultBlockShape
import com.emberr.presentation.shared.editor.links.rememberWebLinkActions
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.ellipsis
import emberr.shared.generated.resources.pen
import emberr.shared.generated.resources.square_arrow_out_up_right
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.Instant

private const val MILLIS_PER_MINUTE = 60_000L
private val UrlSchemePattern = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):")

@Composable
fun CredentialsBlockView(
    block: CredentialsBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onSave: ((CredentialsBlock) -> Unit)? = null,
    runAfterKeyboardCloses: (action: () -> Unit) -> Unit = { action -> action() }
) {
    val credentialVault = koinInject<CredentialVault>()
    val unlockSession = koinInject<CredentialUnlockSession>()
    val deviceLockPrompt = rememberDeviceLockPrompt()
    val scope = rememberCoroutineScope()
    var editStart by remember {
        mutableStateOf(
            if (onSave != null && block.hasNothingFilledIn()) {
                CredentialsEditStart(decryptedSecretsByFieldId = emptyMap())
            } else {
                null
            }
        )
    }
    var isOpeningEditor by remember { mutableStateOf(false) }

    fun openEditor() {
        if (isOpeningEditor) return
        isOpeningEditor = true
        scope.launch {
            try {
                val decryptedSecretsByFieldId = block.fields
                    .filter { field -> field.encryptedValue != null }
                    .associate { field ->
                        val decryptedValue = credentialVault.decrypt(field) ?: run {
                            showNativeToast("This login is locked until its key arrives from your other device.")
                            return@launch
                        }
                        field.id to decryptedValue
                    }
                if (decryptedSecretsByFieldId.isNotEmpty() && !unlockSession.unlock(deviceLockPrompt)) return@launch
                editStart = CredentialsEditStart(decryptedSecretsByFieldId)
            } finally {
                isOpeningEditor = false
            }
        }
    }

    val currentEditStart = editStart
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .clip(DefaultBlockShape)
        .background(MaterialTheme.colorScheme.surface)
        .emberrBorder(
            DefaultBlockShape,
            color = if (currentEditStart != null) MaterialTheme.colorScheme.primary else emberrBorderColor
        )

    if (currentEditStart != null && onSave != null) {
        Box(modifier = cardModifier) {
            CredentialsBlockEditor(
                block = block,
                editStart = currentEditStart,
                onDone = { editedBlock ->
                    editStart = null
                    onSave(editedBlock)
                },
                runAfterKeyboardCloses = runAfterKeyboardCloses
            )
        }
    } else {
        CredentialsViewCard(
            block = block,
            modifier = cardModifier,
            inSelectionMode = inSelectionMode,
            onToggleSelection = onToggleSelection,
            showsOptions = onSave != null && !inSelectionMode,
            onEditFields = { openEditor() },
            runAfterKeyboardCloses = runAfterKeyboardCloses
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CredentialsViewCard(
    block: CredentialsBlock,
    modifier: Modifier,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    showsOptions: Boolean,
    onEditFields: () -> Unit,
    runAfterKeyboardCloses: (action: () -> Unit) -> Unit
) {
    val credentialVault = koinInject<CredentialVault>()
    val keyringVersion by credentialVault.keyringVersion.collectAsState()
    var lockedFieldIds by remember { mutableStateOf(emptySet<String>()) }
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()

    LaunchedEffect(block.fields, keyringVersion) {
        lockedFieldIds = block.fields
            .filter { field -> field.encryptedValue != null }
            .filterNot { field -> field.keyId?.let { keyId -> credentialVault.hasReadableKey(keyId) } == true }
            .map { field -> field.id }
            .toSet()
    }

    Column(
        modifier = modifier
            .hoverable(hoverSource)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (inSelectionMode) onToggleSelection() },
                onLongClick = if (isDesktopPlatform) null else onToggleSelection
            )
    ) {
        CredentialsViewHeader(
            block = block,
            inSelectionMode = inSelectionMode,
            showsOptions = showsOptions,
            isCardHovered = isHovered,
            onEditFields = onEditFields,
            runAfterKeyboardCloses = runAfterKeyboardCloses
        )
        if (block.fields.isNotEmpty()) {
            CredentialFieldRows(
                fields = block.fields,
                lockedFieldIds = lockedFieldIds,
                inSelectionMode = inSelectionMode,
                onToggleSelection = onToggleSelection
            )
        }
        if (block.updatedAt > 0L) {
            CredentialsEditedFooter(updatedAt = block.updatedAt)
        }
    }
}

@Composable
private fun CredentialsViewHeader(
    block: CredentialsBlock,
    inSelectionMode: Boolean,
    showsOptions: Boolean,
    isCardHovered: Boolean,
    onEditFields: () -> Unit,
    runAfterKeyboardCloses: (action: () -> Unit) -> Unit
) {
    val webLinkActions = rememberWebLinkActions()
    val openableUrl = openableLoginUrl(block.loginUrl)
    var isMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CredentialServiceTile(serviceName = block.serviceName)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = block.serviceName.ifBlank { "Untitled login" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (block.serviceName.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (block.loginUrl.isNotBlank()) {
                Text(
                    text = shownLoginUrl(block.loginUrl),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (openableUrl != null && !inSelectionMode) {
            Spacer(modifier = Modifier.width(4.dp))
            CredentialIconButton(
                painter = painterResource(Res.drawable.square_arrow_out_up_right),
                description = "Open sign-in page",
                onClick = { webLinkActions.openLink(openableUrl) }
            )
        }
        if (showsOptions) {
            Box {
                CredentialIconButton(
                    painter = painterResource(Res.drawable.ellipsis),
                    description = "Options",
                    onClick = { runAfterKeyboardCloses { isMenuOpen = true } },
                    modifier = Modifier.alpha(if (!isDesktopPlatform || isCardHovered || isMenuOpen) 1f else 0f)
                )
                CredentialsOptionsMenu(
                    expanded = isMenuOpen,
                    title = block.serviceName.ifBlank { "Login" },
                    onDismiss = { isMenuOpen = false },
                    onEditFields = onEditFields
                )
            }
        }
    }
}

private fun shownLoginUrl(loginUrl: String): String =
    loginUrl.trim().removePrefix("https://").removeSuffix("/")

private fun openableLoginUrl(loginUrl: String): String? {
    val trimmedUrl = loginUrl.trim()
    if (trimmedUrl.isEmpty()) return null
    val schemeMatch = UrlSchemePattern.find(trimmedUrl) ?: return "https://$trimmedUrl"
    val restAfterScheme = trimmedUrl.substring(schemeMatch.value.length)
    val isHostWithPort = !restAfterScheme.startsWith("//") && restAfterScheme.firstOrNull()?.isDigit() == true
    if (isHostWithPort) return "https://$trimmedUrl"
    val scheme = schemeMatch.groupValues[1].lowercase()
    return trimmedUrl.takeIf { scheme == "http" || scheme == "https" }
}

@Composable
private fun CredentialsEditedFooter(updatedAt: Long) {
    val nowMillis by produceState(Clock.System.now().toEpochMilliseconds(), updatedAt) {
        while (true) {
            value = Clock.System.now().toEpochMilliseconds()
            delay(MILLIS_PER_MINUTE)
        }
    }
    Text(
        text = editedAgoText(updatedAt, nowMillis),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
    )
}

internal fun editedAgoText(updatedAtMillis: Long, nowMillis: Long): String {
    val minutesAgo = (nowMillis - updatedAtMillis) / MILLIS_PER_MINUTE
    val hoursAgo = minutesAgo / 60
    val timeZone = TimeZone.currentSystemDefault()
    val updatedDate = Instant.fromEpochMilliseconds(updatedAtMillis).toLocalDateTime(timeZone).date
    val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
    val daysAgo = updatedDate.daysUntil(today)
    return when {
        minutesAgo < 1 -> "Edited just now"
        minutesAgo == 1L -> "Edited 1 minute ago"
        minutesAgo < 60 -> "Edited $minutesAgo minutes ago"
        hoursAgo == 1L -> "Edited 1 hour ago"
        hoursAgo < 24 -> "Edited $hoursAgo hours ago"
        daysAgo <= 1 -> "Edited yesterday"
        daysAgo <= 7 -> "Edited $daysAgo days ago"
        else -> "Edited ${shortDateText(updatedDate)}"
    }
}

@Composable
private fun CredentialsOptionsMenu(
    expanded: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onEditFields: () -> Unit
) {
    val optionIcon: @Composable (Painter) -> Unit = { painter ->
        Icon(
            painter = painter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
    val editIcon = painterResource(Res.drawable.pen)

    if (isDesktopPlatform) {
        EmberrDesktopMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(220.dp)
        ) {
            EmberrDesktopMenuOption(
                label = "Edit fields",
                icon = { optionIcon(editIcon) },
                onClick = {
                    onDismiss()
                    onEditFields()
                }
            )
        }
    } else {
        EmberrBottomSheet(
            expanded = expanded,
            onDismiss = onDismiss,
            title = title,
            contentHorizontalPadding = 0.dp
        ) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                EmberrBottomSheetOption(
                    label = "Edit fields",
                    icon = { optionIcon(editIcon) },
                    onClick = { closeAnd(onEditFields) }
                )
            }
        }
    }
}
