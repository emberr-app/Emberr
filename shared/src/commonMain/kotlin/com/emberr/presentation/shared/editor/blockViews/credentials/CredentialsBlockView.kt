package com.emberr.presentation.shared.editor.blockViews.credentials

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.domain.credentials.CredentialVault
import com.emberr.domain.model.CredentialsBlock
import com.emberr.domain.model.CredentialsEdit
import com.emberr.domain.model.openableLoginUrl
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.LocalSitsOnCard
import com.emberr.presentation.shared.components.SmallRoundedCornerShape
import com.emberr.presentation.shared.editor.DefaultBlockShape
import com.emberr.presentation.shared.editor.GlobalEditorState
import com.emberr.presentation.shared.editor.links.rememberWebLinkActions
import com.emberr.presentation.shared.editor.text.ScrollOnlyWhenHiddenBringIntoViewSpec
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.credentials_edit_fields
import emberr.shared.generated.resources.credentials_edited_days_ago
import emberr.shared.generated.resources.credentials_edited_hours_ago
import emberr.shared.generated.resources.credentials_edited_just_now
import emberr.shared.generated.resources.credentials_edited_minutes_ago
import emberr.shared.generated.resources.credentials_edited_on_date
import emberr.shared.generated.resources.credentials_edited_yesterday
import emberr.shared.generated.resources.credentials_open
import emberr.shared.generated.resources.credentials_open_sign_in_page
import emberr.shared.generated.resources.credentials_options
import emberr.shared.generated.resources.credentials_untitled
import emberr.shared.generated.resources.editor_block_credentials
import emberr.shared.generated.resources.ellipsis
import emberr.shared.generated.resources.pen
import emberr.shared.generated.resources.square_arrow_out_up_right
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.Instant
import com.emberr.domain.util.time.formatDate
import com.emberr.domain.util.time.DateTextStyle

private const val MILLIS_PER_MINUTE = 60_000L
private val WebSchemePrefixPattern = Regex("^https?://", RegexOption.IGNORE_CASE)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CredentialsBlockView(
    block: CredentialsBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onSave: ((CredentialsEdit) -> Unit)? = null,
    runAfterKeyboardCloses: (action: () -> Unit) -> Unit = { action -> action() }
) {
    val secretReader = rememberCredentialSecretReader()
    val scope = rememberCoroutineScope()
    var editStart by remember {
        val opensFormForNewCard = onSave != null && block.id in GlobalEditorState.newCredentialsBlocksAwaitingForm
        if (opensFormForNewCard) GlobalEditorState.newCredentialsBlocksAwaitingForm -= block.id
        mutableStateOf(if (opensFormForNewCard) CredentialsEditStart(blockAtStart = block, decryptedSecretsByFieldId = emptyMap()) else null)
    }
    var isOpeningEditor by remember { mutableStateOf(false) }

    fun openEditor() {
        if (isOpeningEditor) return
        isOpeningEditor = true
        val blockAtStart = block
        scope.launch {
            try {
                val decryptedSecretsByFieldId = secretReader.readAllAfterUnlock(blockAtStart.fields) ?: return@launch
                editStart = CredentialsEditStart(blockAtStart, decryptedSecretsByFieldId)
            } finally {
                isOpeningEditor = false
            }
        }
    }

    val currentEditStart = editStart
    val isEditing = currentEditStart != null && onSave != null
    val pinnableContainer = LocalPinnableContainer.current

    DisposableEffect(pinnableContainer, isEditing) {
        val keepFormWhileOffScreen = if (isEditing) pinnableContainer?.pin() else null
        onDispose { keepFormWhileOffScreen?.release() }
    }

    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .clip(DefaultBlockShape)
        .background(MaterialTheme.colorScheme.surface)

    CompositionLocalProvider(
        LocalSitsOnCard provides true,
        LocalBringIntoViewSpec provides ScrollOnlyWhenHiddenBringIntoViewSpec
    ) {
        if (currentEditStart != null && onSave != null) {
            Box(modifier = cardModifier) {
                CredentialsBlockEditor(
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
    val untitledCredentials = stringResource(Res.string.credentials_untitled)
    val credentialsBlockName = stringResource(Res.string.editor_block_credentials)

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CredentialServiceTile(serviceName = block.serviceName, iconUrl = block.iconUrl)
        Spacer(modifier = Modifier.width(if (isDesktopPlatform) 16.dp else 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = block.serviceName.ifBlank { untitledCredentials },
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
                    description = stringResource(Res.string.credentials_options),
                    onClick = { runAfterKeyboardCloses { isMenuOpen = true } },
                    modifier = Modifier.alpha(if (!isDesktopPlatform || isCardHovered || isMenuOpen) 1f else 0f)
                )
                CredentialsOptionsMenu(
                    expanded = isMenuOpen,
                    title = block.serviceName.ifBlank { credentialsBlockName },
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
        CredentialIconButton(painter = painter, description = stringResource(Res.string.credentials_open_sign_in_page), onClick = onClick)
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
        Text(text = stringResource(Res.string.credentials_open), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
internal fun editedAgoText(updatedAtMillis: Long, nowMillis: Long): String {
    val minutesAgo = (nowMillis - updatedAtMillis) / MILLIS_PER_MINUTE
    val hoursAgo = minutesAgo / 60
    val timeZone = TimeZone.currentSystemDefault()
    val updatedDate = Instant.fromEpochMilliseconds(updatedAtMillis).toLocalDateTime(timeZone).date
    val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
    val daysAgo = updatedDate.daysUntil(today)
    return when {
        minutesAgo < 1 -> stringResource(Res.string.credentials_edited_just_now)
        minutesAgo < 60 -> pluralStringResource(Res.plurals.credentials_edited_minutes_ago, minutesAgo.toInt(), minutesAgo.toInt())
        hoursAgo < 24 -> pluralStringResource(Res.plurals.credentials_edited_hours_ago, hoursAgo.toInt(), hoursAgo.toInt())
        daysAgo <= 1 -> stringResource(Res.string.credentials_edited_yesterday)
        daysAgo <= 7 -> pluralStringResource(Res.plurals.credentials_edited_days_ago, daysAgo, daysAgo)
        else -> stringResource(Res.string.credentials_edited_on_date, formatDate(updatedDate, DateTextStyle.SHORT_MONTH_DAY_YEAR))
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
                label = stringResource(Res.string.credentials_edit_fields),
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
                    label = stringResource(Res.string.credentials_edit_fields),
                    icon = { optionIcon(editIcon) },
                    outerHorizontalPadding = SheetRowInset,
                    onClick = { closeAnd(onEditFields) }
                )
                SheetCloseButton(onClick = { closeAnd { } })
            }
        }
    }
}
