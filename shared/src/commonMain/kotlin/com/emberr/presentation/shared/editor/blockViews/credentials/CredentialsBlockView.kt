package com.emberr.presentation.shared.editor.blockViews.credentials

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.emberr.domain.model.CredentialsEdit
import com.emberr.domain.model.openableLoginUrl
import com.emberr.domain.model.shortDateText
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.rememberDeviceLockPrompt
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.LocalSitsOnCard
import com.emberr.presentation.shared.components.SmallRoundedCornerShape
import com.emberr.presentation.shared.editor.DefaultBlockShape
import com.emberr.presentation.shared.editor.GlobalEditorState
import com.emberr.presentation.shared.editor.links.rememberWebLinkActions
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.ellipsis
import emberr.shared.generated.resources.pen
import emberr.shared.generated.resources.square_arrow_out_up_right
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.Instant

private const val MILLIS_PER_MINUTE = 60_000L
private val WebSchemePrefixPattern = Regex("^https?://", RegexOption.IGNORE_CASE)

@Composable
fun CredentialsBlockView(
    block: CredentialsBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onSave: ((CredentialsEdit) -> Unit)? = null,
    runAfterKeyboardCloses: (action: () -> Unit) -> Unit = { action -> action() }
) {
    val credentialVault = koinInject<CredentialVault>()
    val unlockSession = koinInject<CredentialUnlockSession>()
    val deviceLockPrompt = rememberDeviceLockPrompt()
    val scope = rememberCoroutineScope()
    var editStart by remember {
        val opensFormForNewCard = onSave != null && block.id in GlobalEditorState.newCredentialsBlocksAwaitingForm
        if (opensFormForNewCard) GlobalEditorState.newCredentialsBlocksAwaitingForm -= block.id
        mutableStateOf(if (opensFormForNewCard) CredentialsEditStart(decryptedSecretsByFieldId = emptyMap()) else null)
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
                            showNativeToast("These credentials are locked until their key arrives from your other device.")
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

    CompositionLocalProvider(LocalSitsOnCard provides true) {
        if (currentEditStart != null && onSave != null) {
            Box(modifier = cardModifier) {
                CredentialsBlockEditor(
                    block = block,
                    editStart = currentEditStart,
                    onDone = { edit ->
                        editStart = null
                        onSave(edit)
                    },
                    onCancel = { editStart = null },
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
        CredentialServiceTile(serviceName = block.serviceName, iconUrl = block.iconUrl)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = block.serviceName.ifBlank { "Untitled credentials" },
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
            OpenSignInPageButton(onClick = { webLinkActions.openLink(openableUrl) })
        }
        if (showsOptions) {
            if (openableUrl != null) Spacer(modifier = Modifier.width(8.dp))
            Box {
                CredentialIconButton(
                    painter = painterResource(Res.drawable.ellipsis),
                    description = "Options",
                    onClick = { runAfterKeyboardCloses { isMenuOpen = true } },
                    modifier = Modifier.alpha(if (!isDesktopPlatform || isCardHovered || isMenuOpen) 1f else 0f)
                )
                CredentialsOptionsMenu(
                    expanded = isMenuOpen,
                    title = block.serviceName.ifBlank { "Credentials" },
                    updatedAt = block.updatedAt,
                    onDismiss = { isMenuOpen = false },
                    onEditFields = onEditFields
                )
            }
        }
    }
}

private fun shownLoginUrl(loginUrl: String): String =
    loginUrl.trim().replace(WebSchemePrefixPattern, "").removeSuffix("/")

@Composable
private fun OpenSignInPageButton(onClick: () -> Unit) {
    val painter = painterResource(Res.drawable.square_arrow_out_up_right)
    if (!isDesktopPlatform) {
        CredentialIconButton(painter = painter, description = "Open sign-in page", onClick = onClick)
        return
    }
    Row(
        modifier = Modifier
            .clip(SmallRoundedCornerShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Open", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
    }
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
    updatedAt: Long,
    onDismiss: () -> Unit,
    onEditFields: () -> Unit
) {
    val editedText = if (expanded && updatedAt > 0L) {
        editedAgoText(updatedAt, Clock.System.now().toEpochMilliseconds())
    } else {
        null
    }
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
            if (editedText != null) {
                CredentialsDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = editedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
    } else {
        EmberrBottomSheet(
            expanded = expanded,
            onDismiss = onDismiss,
            title = title,
            subtitle = editedText,
            contentHorizontalPadding = 0.dp
        ) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = SheetBottomPadding)) {
                EmberrBottomSheetOption(
                    label = "Edit fields",
                    icon = { optionIcon(editIcon) },
                    outerHorizontalPadding = SheetRowInset,
                    onClick = { closeAnd(onEditFields) }
                )
                SheetCloseButton(onClick = { closeAnd { } })
            }
        }
    }
}
