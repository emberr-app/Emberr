package com.emberr.presentation.shared.editor.blockViews.credentials

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.emberr.domain.cloud.account.CloudPasswordStrength
import com.emberr.domain.cloud.account.estimateCloudPasswordStrength
import com.emberr.domain.credentials.CredentialEncryption
import com.emberr.domain.credentials.CredentialVault
import com.emberr.domain.model.ChangedCredentialField
import com.emberr.domain.model.CredentialField
import com.emberr.domain.model.CredentialFieldKind
import com.emberr.domain.model.CredentialsBlock
import com.emberr.domain.model.CredentialsEdit
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.settings.cloud.ModerateColor
import com.emberr.presentation.settings.cloud.StrongColor
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.SmallRoundedCornerShape
import com.emberr.presentation.shared.components.emberrBorderColor
import com.emberr.presentation.shared.components.subtleFillColor
import com.emberr.presentation.shared.editor.GlobalEditorState
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.eye_closed
import emberr.shared.generated.resources.eye_open
import emberr.shared.generated.resources.lock_password
import emberr.shared.generated.resources.notes2
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.refresh_cw
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.user2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val TextFieldHeight = 56.dp
private val DesktopLabelInputWidth = 128.dp
private val StrengthBarWidth = 24.dp
private val KindMenuMinWidth = 220.dp
private val KindMenuMaxWidth = 300.dp
private val DesktopFooterButtonMinWidth = 112.dp
private val TrailingIconGap = 8.dp
private val TrailingIconsEndPadding = 14.dp
private val RemoveButtonStartPadding = 14.dp
private val RemoveButtonEndPadding = 2.dp
private val AndroidLabelPullTowardIcon = 6.dp
internal val SheetRowInset = 8.dp
internal val SheetBottomPadding = 16.dp
private val SheetButtonHorizontalPadding = 20.dp

class CredentialsEditStart(val decryptedSecretsByFieldId: Map<String, String>)

private class CredentialFieldDraft(
    val id: String,
    val originalKind: CredentialFieldKind,
    val originalLabel: String,
    val originalValue: String
) {
    var kind by mutableStateOf(originalKind)
    var label by mutableStateOf(originalLabel)
    var value by mutableStateOf(originalValue)
    var isValueVisible by mutableStateOf(false)
    val focusRequester = FocusRequester()

    fun changeKind(newKind: CredentialFieldKind) {
        if (label.isBlank() || label == kind.defaultLabel()) label = newKind.defaultLabel()
        kind = newKind
        isValueVisible = false
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun newFieldDraft(kind: CredentialFieldKind) =
    CredentialFieldDraft(id = Uuid.random().toString(), originalKind = kind, originalLabel = kind.defaultLabel(), originalValue = "")

internal fun CredentialFieldKind.defaultLabel(): String = when (this) {
    CredentialFieldKind.USERNAME -> "Username"
    CredentialFieldKind.PASSWORD -> "Password"
    CredentialFieldKind.NOTE -> "Note"
}

private fun CredentialFieldKind.menuLabel(): String =
    if (this == CredentialFieldKind.USERNAME) "Username or email" else defaultLabel()

private fun CredentialFieldKind.valuePlaceholder(): String = when (this) {
    CredentialFieldKind.USERNAME -> "Username or email"
    CredentialFieldKind.PASSWORD -> "Password"
    CredentialFieldKind.NOTE -> "Add a note"
}

private fun CredentialFieldKind.valueKeyboardOptions(): KeyboardOptions = when (this) {
    CredentialFieldKind.USERNAME -> KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false)
    CredentialFieldKind.PASSWORD -> KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)
    CredentialFieldKind.NOTE -> KeyboardOptions.Default
}

private val CredentialFieldKind.hidesValue: Boolean
    get() = this == CredentialFieldKind.PASSWORD

@Composable
internal fun CredentialFieldKind.icon(): Painter = when (this) {
    CredentialFieldKind.USERNAME -> painterResource(Res.drawable.user2)
    CredentialFieldKind.PASSWORD -> painterResource(Res.drawable.lock_password)
    CredentialFieldKind.NOTE -> painterResource(Res.drawable.notes2)
}

private fun finishesEditing(event: KeyEvent): Boolean {
    if (!isDesktopPlatform || event.type != KeyEventType.KeyDown) return false
    val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
    return event.key == Key.Escape || (event.isCtrlPressed && isEnter)
}

@Composable
fun CredentialsBlockEditor(
    block: CredentialsBlock,
    editStart: CredentialsEditStart,
    onDone: (CredentialsEdit) -> Unit,
    onCancel: () -> Unit,
    runAfterKeyboardCloses: (action: () -> Unit) -> Unit
) {
    val credentialVault = koinInject<CredentialVault>()
    val scope = rememberCoroutineScope()
    val blockAtStart = remember { block }
    var serviceName by remember { mutableStateOf(blockAtStart.serviceName) }
    var loginUrl by remember { mutableStateOf(blockAtStart.loginUrl) }
    val fieldDrafts = remember {
        blockAtStart.fields.map { field ->
            CredentialFieldDraft(
                id = field.id,
                originalKind = field.kind,
                originalLabel = field.label,
                originalValue = if (field.kind == CredentialFieldKind.USERNAME) {
                    field.plainValue
                } else {
                    editStart.decryptedSecretsByFieldId[field.id].orEmpty()
                }
            )
        }.toMutableStateList()
    }
    var fieldIdToFocus by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        GlobalEditorState.openCredentialsFormCount += 1
        onDispose { GlobalEditorState.openCredentialsFormCount -= 1 }
    }

    LaunchedEffect(fieldIdToFocus) {
        val fieldId = fieldIdToFocus ?: return@LaunchedEffect
        withFrameNanos { }
        runCatching { fieldDrafts.firstOrNull { draft -> draft.id == fieldId }?.focusRequester?.requestFocus() }
        fieldIdToFocus = null
    }

    fun finish() {
        if (isSaving) return
        isSaving = true
        scope.launch {
            val edit = credentialsEdit(serviceName, loginUrl, fieldDrafts.toList(), blockAtStart, credentialVault)
            if (edit == null) {
                showNativeToast("Couldn't lock these credentials on this device. Try again.")
                isSaving = false
            } else {
                onDone(edit)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (finishesEditing(event)) {
                    finish()
                    true
                } else {
                    false
                }
            }
    ) {
        CredentialsEditorHeader(
            serviceName = serviceName,
            onServiceNameChange = { serviceName = it },
            loginUrl = loginUrl,
            onLoginUrlChange = { loginUrl = it },
            iconUrl = blockAtStart.iconUrl.takeIf { loginUrl == blockAtStart.loginUrl }
        )
        CredentialsDivider()
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = if (isDesktopPlatform) 12.dp else 16.dp, vertical = 6.dp)) {
            if (fieldDrafts.isEmpty()) {
                Text(
                    text = "Add a field for a username, password or note.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            fieldDrafts.forEachIndexed { index, draft ->
                key(draft.id) {
                    if (index > 0 && !isDesktopPlatform) CredentialsDivider()
                    CredentialFieldEditorRow(
                        draft = draft,
                        onRemove = { fieldDrafts.remove(draft) },
                        runAfterKeyboardCloses = runAfterKeyboardCloses
                    )
                }
            }
        }
        CredentialsDivider()
        CredentialsEditorFooter(
            isSaving = isSaving,
            onAddField = { kind ->
                val newDraft = newFieldDraft(kind)
                fieldDrafts.add(newDraft)
                fieldIdToFocus = newDraft.id
            },
            onCancel = onCancel,
            onSave = { finish() },
            runAfterKeyboardCloses = runAfterKeyboardCloses
        )
    }
}

private suspend fun credentialsEdit(
    serviceName: String,
    loginUrl: String,
    drafts: List<CredentialFieldDraft>,
    blockAtStart: CredentialsBlock,
    credentialVault: CredentialVault
): CredentialsEdit? {
    val fieldIdsAtStart = blockAtStart.fields.map { field -> field.id }.toSet()
    val changedFields = mutableListOf<ChangedCredentialField>()
    val addedFields = mutableListOf<CredentialField>()
    for (draft in drafts) {
        val labelChanged = draft.label != draft.originalLabel
        val valueChanged = draft.value != draft.originalValue || draft.kind != draft.originalKind
        when {
            draft.id !in fieldIdsAtStart -> addedFields += lockedField(draft, credentialVault) ?: return null
            valueChanged -> changedFields += ChangedCredentialField(
                field = lockedField(draft, credentialVault) ?: return null,
                labelChanged = labelChanged,
                valueChanged = true
            )
            labelChanged -> changedFields += ChangedCredentialField(
                field = CredentialField(id = draft.id, kind = draft.kind, label = draft.label),
                labelChanged = true,
                valueChanged = false
            )
        }
    }
    return CredentialsEdit(
        serviceName = serviceName.takeIf { it != blockAtStart.serviceName },
        loginUrl = loginUrl.takeIf { it != blockAtStart.loginUrl },
        changedFields = changedFields,
        addedFields = addedFields,
        removedFieldIds = fieldIdsAtStart - drafts.map { draft -> draft.id }.toSet()
    )
}

private suspend fun lockedField(draft: CredentialFieldDraft, credentialVault: CredentialVault): CredentialField? = when {
    draft.kind == CredentialFieldKind.USERNAME ->
        CredentialField(id = draft.id, kind = draft.kind, label = draft.label, plainValue = draft.value)
    draft.value.isEmpty() ->
        CredentialField(id = draft.id, kind = draft.kind, label = draft.label)
    else -> when (val encryption = credentialVault.encrypt(draft.kind, draft.value)) {
        is CredentialEncryption.Encrypted -> CredentialField(
            id = draft.id,
            kind = draft.kind,
            label = draft.label,
            encryptedValue = encryption.encryptedValue,
            keyId = encryption.keyId
        )
        CredentialEncryption.KeyringUnavailable -> null
    }
}

@Composable
private fun CredentialsEditorHeader(
    serviceName: String,
    onServiceNameChange: (String) -> Unit,
    loginUrl: String,
    onLoginUrlChange: (String) -> Unit,
    iconUrl: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        CredentialServiceTile(
            serviceName = serviceName,
            iconUrl = iconUrl,
            modifier = Modifier.padding(top = (TextFieldHeight - CredentialTileSize) / 2)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            EmberrTextField(
                value = serviceName,
                onValueChange = onServiceNameChange,
                placeholder = "Service name",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            EmberrTextField(
                value = loginUrl,
                onValueChange = onLoginUrlChange,
                placeholder = "Login URL",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

internal val CredentialTileSize: Dp
    get() = if (isDesktopPlatform) 32.dp else 36.dp

@Composable
internal fun CredentialServiceTile(serviceName: String, iconUrl: String?, modifier: Modifier = Modifier) {
    val firstLetter = serviceName.trim().firstOrNull()?.uppercaseChar()
    var hasLoadedIcon by remember(iconUrl) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(CredentialTileSize)
            .clip(RoundedCornerShape(8.dp))
            .background(if (hasLoadedIcon) Color.Transparent else MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        if (!hasLoadedIcon && firstLetter == null) {
            Icon(
                painter = painterResource(Res.drawable.lock_password),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        } else if (!hasLoadedIcon) {
            Text(
                text = firstLetter.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        if (iconUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(iconUrl)
                    .httpHeaders(
                        NetworkHeaders.Builder()
                            .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                            .set("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                            .build()
                    )
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onState = { state -> hasLoadedIcon = state is AsyncImagePainter.State.Success },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (hasLoadedIcon) 1f else 0f)
            )
        }
    }
}

@Composable
private fun CredentialGhostInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    var isFocused by remember { mutableStateOf(false) }

    EmberrTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        hasBackground = false,
        modifier = modifier
            .clip(SmallRoundedCornerShape)
            .hoverable(hoverSource)
            .background(if (isFocused || isHovered) subtleFillColor() else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .onFocusChanged { focusState -> isFocused = focusState.isFocused }
    )
}

@Composable
private fun CredentialFieldEditorRow(
    draft: CredentialFieldDraft,
    onRemove: () -> Unit,
    runAfterKeyboardCloses: (action: () -> Unit) -> Unit
) {
    var isKindMenuOpen by remember { mutableStateOf(false) }
    var isGeneratorOpen by remember { mutableStateOf(false) }
    val kindButton = @Composable {
        Box {
            CredentialIconButton(
                painter = draft.kind.icon(),
                description = "Change field type",
                onClick = { runAfterKeyboardCloses { isKindMenuOpen = true } }
            )
            CredentialFieldKindMenu(
                expanded = isKindMenuOpen,
                title = "Field type",
                selectedKind = draft.kind,
                onDismiss = { isKindMenuOpen = false },
                onPick = { kind -> draft.changeKind(kind) }
            )
        }
    }
    val labelInput = @Composable { modifier: Modifier ->
        CredentialGhostInput(
            value = draft.label,
            onValueChange = { draft.label = it },
            placeholder = draft.kind.defaultLabel(),
            modifier = modifier
        )
    }
    val generatorButton = @Composable { showsRipple: Boolean ->
        Box {
            CredentialIconButton(
                painter = painterResource(Res.drawable.refresh_cw),
                description = "Generate a password",
                onClick = { runAfterKeyboardCloses { isGeneratorOpen = true } },
                showsRipple = showsRipple
            )
            PasswordGeneratorMenu(
                expanded = isGeneratorOpen,
                onDismiss = { isGeneratorOpen = false },
                onUsePassword = { generatedPassword ->
                    draft.value = generatedPassword
                    draft.isValueVisible = true
                }
            )
        }
    }

    if (isDesktopPlatform) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.height(TextFieldHeight), contentAlignment = Alignment.Center) {
                kindButton()
            }
            Box(
                modifier = Modifier.width(DesktopLabelInputWidth).height(TextFieldHeight),
                contentAlignment = Alignment.CenterStart
            ) {
                labelInput(Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                CredentialValueInput(
                    draft = draft,
                    generatorButton = if (draft.kind == CredentialFieldKind.PASSWORD) {
                        { generatorButton(false) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (draft.kind == CredentialFieldKind.PASSWORD) PasswordStrengthMeter(password = draft.value)
            }
            Box(
                modifier = Modifier.height(TextFieldHeight).padding(start = RemoveButtonStartPadding, end = RemoveButtonEndPadding),
                contentAlignment = Alignment.Center
            ) {
                CredentialIconButton(
                    painter = painterResource(Res.drawable.trash),
                    description = "Remove field",
                    onClick = onRemove
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                kindButton()
                labelInput(Modifier.weight(1f).offset(x = -AndroidLabelPullTowardIcon))
                if (draft.kind == CredentialFieldKind.PASSWORD) generatorButton(true)
                CredentialIconButton(
                    painter = painterResource(Res.drawable.trash),
                    description = "Remove field",
                    onClick = onRemove
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            CredentialValueInput(draft = draft, generatorButton = null, modifier = Modifier.fillMaxWidth())
            if (draft.kind == CredentialFieldKind.PASSWORD) PasswordStrengthMeter(password = draft.value)
        }
    }
}

@Composable
private fun CredentialValueInput(
    draft: CredentialFieldDraft,
    generatorButton: (@Composable () -> Unit)?,
    modifier: Modifier
) {
    val bodyLarge = MaterialTheme.typography.bodyLarge
    EmberrTextField(
        value = draft.value,
        onValueChange = { newValue -> draft.value = newValue },
        placeholder = draft.kind.valuePlaceholder(),
        singleLine = draft.kind != CredentialFieldKind.NOTE,
        keyboardOptions = draft.kind.valueKeyboardOptions(),
        visualTransformation = if (draft.kind.hidesValue && !draft.isValueVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = if (draft.kind.hidesValue) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TrailingIconGap),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = if (generatorButton != null) TrailingIconsEndPadding else 0.dp)
                ) {
                    generatorButton?.invoke()
                    CredentialIconButton(
                        painter = painterResource(if (draft.isValueVisible) Res.drawable.eye_closed else Res.drawable.eye_open),
                        description = if (draft.isValueVisible) "Hide" else "Show",
                        onClick = { draft.isValueVisible = !draft.isValueVisible },
                        showsRipple = false
                    )
                }
            }
        } else {
            null
        },
        textStyle = if (draft.kind.hidesValue) bodyLarge.copy(fontFamily = FontFamily.Monospace) else bodyLarge,
        modifier = modifier.focusRequester(draft.focusRequester)
    )
}

@Composable
private fun PasswordStrengthMeter(password: String) {
    val strength by produceState<CloudPasswordStrength?>(null, password) {
        value = if (password.isEmpty()) null else withContext(Dispatchers.Default) { estimateCloudPasswordStrength(password) }
    }
    val shownStrength = strength
    if (password.isEmpty() || shownStrength == null) return

    val (label, color, filledBarCount) = when (shownStrength) {
        CloudPasswordStrength.WEAK -> Triple("Weak", MaterialTheme.colorScheme.error, 1)
        CloudPasswordStrength.MODERATE -> Triple("Moderate", ModerateColor, 2)
        CloudPasswordStrength.STRONG -> Triple("Strong", StrongColor, 3)
    }
    Row(modifier = Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(CloudPasswordStrength.entries.size) { barIndex ->
            Box(
                modifier = Modifier
                    .padding(end = 3.dp)
                    .size(width = StrengthBarWidth, height = 3.dp)
                    .clip(CircleShape)
                    .background(if (barIndex < filledBarCount) color else emberrBorderColor)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun CredentialsEditorFooter(
    isSaving: Boolean,
    onAddField: (CredentialFieldKind) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    runAfterKeyboardCloses: (action: () -> Unit) -> Unit
) {
    var isAddFieldMenuOpen by remember { mutableStateOf(false) }
    val addFieldButton = @Composable {
        Box {
            AddFieldButton(onClick = { runAfterKeyboardCloses { isAddFieldMenuOpen = true } })
            CredentialFieldKindMenu(
                expanded = isAddFieldMenuOpen,
                title = "Add field",
                selectedKind = null,
                onDismiss = { isAddFieldMenuOpen = false },
                onPick = onAddField
            )
        }
    }

    if (isDesktopPlatform) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            addFieldButton()
            Spacer(modifier = Modifier.weight(1f))
            EmberrButtonSecondary(text = "Cancel", onClick = onCancel, enabled = !isSaving, modifier = Modifier.widthIn(min = DesktopFooterButtonMinWidth))
            Spacer(modifier = Modifier.width(8.dp))
            EmberrButtonPrimary(text = "Save", onClick = onSave, enabled = !isSaving, modifier = Modifier.widthIn(min = DesktopFooterButtonMinWidth))
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            addFieldButton()
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                EmberrButtonSecondary(text = "Cancel", onClick = onCancel, enabled = !isSaving, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                EmberrButtonPrimary(text = "Save", onClick = onSave, enabled = !isSaving, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AddFieldButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(SmallRoundedCornerShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.plus),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Add field",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CredentialFieldKindMenu(
    expanded: Boolean,
    title: String,
    selectedKind: CredentialFieldKind?,
    onDismiss: () -> Unit,
    onPick: (CredentialFieldKind) -> Unit
) {
    val kindIcon: @Composable (CredentialFieldKind) -> Unit = { kind ->
        Icon(
            painter = kind.icon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }

    if (isDesktopPlatform) {
        EmberrDesktopMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.widthIn(min = KindMenuMinWidth, max = KindMenuMaxWidth)
        ) {
            CredentialFieldKind.entries.forEach { kind ->
                EmberrDesktopMenuOption(
                    label = kind.menuLabel(),
                    icon = { kindIcon(kind) },
                    isSelected = kind == selectedKind,
                    onClick = {
                        onDismiss()
                        onPick(kind)
                    }
                )
            }
        }
    } else {
        EmberrBottomSheet(
            expanded = expanded,
            onDismiss = onDismiss,
            title = title,
            contentHorizontalPadding = 0.dp
        ) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = SheetBottomPadding)) {
                CredentialFieldKind.entries.forEach { kind ->
                    EmberrBottomSheetOption(
                        label = kind.menuLabel(),
                        icon = { kindIcon(kind) },
                        isSelected = kind == selectedKind,
                        outerHorizontalPadding = SheetRowInset,
                        onClick = { closeAnd { onPick(kind) } }
                    )
                }
                SheetCloseButton(onClick = { closeAnd { } })
            }
        }
    }
}

@Composable
internal fun SheetCloseButton(onClick: () -> Unit) {
    EmberrButtonPrimary(
        text = "Close",
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SheetButtonHorizontalPadding, end = SheetButtonHorizontalPadding, top = 12.dp)
    )
}
