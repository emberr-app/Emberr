package com.emberr.presentation.shared.editor.blockViews.credentials

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import androidx.compose.ui.unit.dp
import com.emberr.domain.credentials.CredentialEncryption
import com.emberr.domain.credentials.CredentialVault
import com.emberr.domain.credentials.oneTimeCodeFor
import com.emberr.domain.model.CredentialField
import com.emberr.domain.model.CredentialFieldKind
import com.emberr.domain.model.CredentialsBlock
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.IconTooltip
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.clock_circle
import emberr.shared.generated.resources.notes2
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.trash
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val TextFieldHeight = 56.dp
private val DesktopLabelInputWidth = 132.dp

class CredentialsEditStart(
    val decryptedSecretsByFieldId: Map<String, String>,
    val focusesNewOneTimeCode: Boolean
)

private class CredentialFieldDraft(
    val id: String,
    val kind: CredentialFieldKind,
    label: String,
    val originalValue: String
) {
    var label by mutableStateOf(label)
    var value by mutableStateOf(originalValue)
    var isValueVisible by mutableStateOf(false)
    val focusRequester = FocusRequester()
}

@OptIn(ExperimentalUuidApi::class)
private fun newFieldDraft(kind: CredentialFieldKind) =
    CredentialFieldDraft(id = Uuid.random().toString(), kind = kind, label = kind.defaultLabel(), originalValue = "")

private fun CredentialFieldKind.defaultLabel(): String = when (this) {
    CredentialFieldKind.USERNAME -> "Username"
    CredentialFieldKind.PASSWORD -> "Password"
    CredentialFieldKind.ONE_TIME_CODE -> "One-time code"
    CredentialFieldKind.NOTE -> "Note"
}

private fun CredentialFieldKind.addFieldLabel(): String =
    if (this == CredentialFieldKind.USERNAME) "Username or email" else defaultLabel()

private fun CredentialFieldKind.valuePlaceholder(): String = when (this) {
    CredentialFieldKind.USERNAME -> "Username or email"
    CredentialFieldKind.PASSWORD -> "Password"
    CredentialFieldKind.ONE_TIME_CODE -> "Setup key or otpauth:// link"
    CredentialFieldKind.NOTE -> "Add a note"
}

private fun CredentialFieldKind.valueKeyboardOptions(): KeyboardOptions = when (this) {
    CredentialFieldKind.USERNAME -> KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false)
    CredentialFieldKind.PASSWORD,
    CredentialFieldKind.ONE_TIME_CODE -> KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)
    CredentialFieldKind.NOTE -> KeyboardOptions.Default
}

private val CredentialFieldKind.hidesValue: Boolean
    get() = this == CredentialFieldKind.PASSWORD || this == CredentialFieldKind.ONE_TIME_CODE

@Composable
private fun CredentialFieldKind.icon(): Painter = when (this) {
    CredentialFieldKind.USERNAME -> rememberVectorPainter(Icons.Outlined.Person)
    CredentialFieldKind.PASSWORD -> rememberVectorPainter(Icons.Outlined.Key)
    CredentialFieldKind.ONE_TIME_CODE -> painterResource(Res.drawable.clock_circle)
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
    onDone: (CredentialsBlock) -> Unit,
    runAfterKeyboardCloses: (action: () -> Unit) -> Unit
) {
    val credentialVault = koinInject<CredentialVault>()
    val scope = rememberCoroutineScope()
    val fieldsAtStart = remember { block.fields }
    var serviceName by remember { mutableStateOf(block.serviceName) }
    var loginUrl by remember { mutableStateOf(block.loginUrl) }
    val fieldDrafts = remember {
        val savedDrafts = fieldsAtStart.map { field ->
            CredentialFieldDraft(
                id = field.id,
                kind = field.kind,
                label = field.label,
                originalValue = if (field.kind == CredentialFieldKind.USERNAME) {
                    field.plainValue
                } else {
                    editStart.decryptedSecretsByFieldId[field.id].orEmpty()
                }
            )
        }
        val addedDrafts = if (editStart.focusesNewOneTimeCode) listOf(newFieldDraft(CredentialFieldKind.ONE_TIME_CODE)) else emptyList()
        (savedDrafts + addedDrafts).toMutableStateList()
    }
    var fieldIdToFocus by remember {
        mutableStateOf(if (editStart.focusesNewOneTimeCode) fieldDrafts.lastOrNull()?.id else null)
    }
    var invalidCodeFieldIds by remember { mutableStateOf(emptySet<String>()) }
    var isSaving by remember { mutableStateOf(false) }
    var isAddFieldMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(fieldIdToFocus) {
        val fieldId = fieldIdToFocus ?: return@LaunchedEffect
        withFrameNanos { }
        runCatching { fieldDrafts.firstOrNull { draft -> draft.id == fieldId }?.focusRequester?.requestFocus() }
        fieldIdToFocus = null
    }

    fun finish() {
        if (isSaving) return
        val now = Clock.System.now().toEpochMilliseconds()
        invalidCodeFieldIds = fieldDrafts
            .filter { draft ->
                draft.kind == CredentialFieldKind.ONE_TIME_CODE &&
                    draft.value.isNotBlank() &&
                    oneTimeCodeFor(draft.value, now) == null
            }
            .map { draft -> draft.id }
            .toSet()
        if (invalidCodeFieldIds.isNotEmpty()) return
        isSaving = true
        scope.launch {
            val savedFields = lockedFields(fieldDrafts.toList(), fieldsAtStart, credentialVault)
            if (savedFields == null) {
                showNativeToast("Couldn't lock this login on this device. Try again.")
                isSaving = false
            } else {
                onDone(block.copy(serviceName = serviceName, loginUrl = loginUrl, fields = savedFields))
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
            .padding(16.dp)
    ) {
        CredentialsEditorHeader(
            serviceName = serviceName,
            onServiceNameChange = { serviceName = it },
            loginUrl = loginUrl,
            onLoginUrlChange = { loginUrl = it }
        )
        fieldDrafts.forEach { draft ->
            key(draft.id) {
                Spacer(modifier = Modifier.height(12.dp))
                CredentialFieldEditorRow(
                    draft = draft,
                    showsInvalidCodeError = draft.id in invalidCodeFieldIds,
                    onValueChange = { newValue ->
                        draft.value = newValue
                        invalidCodeFieldIds = invalidCodeFieldIds - draft.id
                    },
                    onRemove = {
                        fieldDrafts.remove(draft)
                        invalidCodeFieldIds = invalidCodeFieldIds - draft.id
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                AddFieldButton(onClick = { runAfterKeyboardCloses { isAddFieldMenuOpen = true } })
                AddCredentialFieldMenu(
                    expanded = isAddFieldMenuOpen,
                    onDismiss = { isAddFieldMenuOpen = false },
                    onAddField = { kind ->
                        val newDraft = newFieldDraft(kind)
                        fieldDrafts.add(newDraft)
                        fieldIdToFocus = newDraft.id
                    }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            EmberrButtonPrimary(text = "Done", onClick = { finish() }, enabled = !isSaving)
        }
    }
}

private suspend fun lockedFields(
    drafts: List<CredentialFieldDraft>,
    fieldsAtStart: List<CredentialField>,
    credentialVault: CredentialVault
): List<CredentialField>? = drafts.map { draft ->
    val savedField = fieldsAtStart.firstOrNull { field -> field.id == draft.id }
    when {
        draft.kind == CredentialFieldKind.USERNAME ->
            CredentialField(id = draft.id, kind = draft.kind, label = draft.label, plainValue = draft.value)
        draft.value.isEmpty() ->
            CredentialField(id = draft.id, kind = draft.kind, label = draft.label)
        savedField != null && draft.value == draft.originalValue ->
            savedField.copy(label = draft.label)
        else -> when (val encryption = credentialVault.encrypt(draft.kind, draft.value)) {
            is CredentialEncryption.Encrypted -> CredentialField(
                id = draft.id,
                kind = draft.kind,
                label = draft.label,
                encryptedValue = encryption.encryptedValue,
                keyId = encryption.keyId
            )
            CredentialEncryption.KeyringUnavailable -> return null
        }
    }
}

@Composable
private fun CredentialsEditorHeader(
    serviceName: String,
    onServiceNameChange: (String) -> Unit,
    loginUrl: String,
    onLoginUrlChange: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        CredentialServiceTile(
            serviceName = serviceName,
            modifier = Modifier.padding(top = (TextFieldHeight - 32.dp) / 2)
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

@Composable
private fun CredentialServiceTile(serviceName: String, modifier: Modifier = Modifier) {
    val firstLetter = serviceName.trim().firstOrNull()?.uppercaseChar()
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        if (firstLetter == null) {
            Icon(
                imageVector = Icons.Outlined.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = firstLetter.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun CredentialFieldEditorRow(
    draft: CredentialFieldDraft,
    showsInvalidCodeError: Boolean,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    if (isDesktopPlatform) {
        Row(verticalAlignment = Alignment.Top) {
            CredentialLabelInput(draft = draft, modifier = Modifier.width(DesktopLabelInputWidth))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                CredentialValueInput(draft = draft, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth())
                if (showsInvalidCodeError) InvalidCodeKeyError()
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.height(TextFieldHeight), contentAlignment = Alignment.Center) {
                RemoveFieldButton(onRemove = onRemove)
            }
        }
    } else {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CredentialLabelInput(draft = draft, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(4.dp))
                RemoveFieldButton(onRemove = onRemove)
            }
            Spacer(modifier = Modifier.height(8.dp))
            CredentialValueInput(draft = draft, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth())
            if (showsInvalidCodeError) InvalidCodeKeyError()
        }
    }
}

@Composable
private fun CredentialLabelInput(draft: CredentialFieldDraft, modifier: Modifier) {
    EmberrTextField(
        value = draft.label,
        onValueChange = { draft.label = it },
        placeholder = "Label",
        modifier = modifier
    )
}

@Composable
private fun CredentialValueInput(
    draft: CredentialFieldDraft,
    onValueChange: (String) -> Unit,
    modifier: Modifier
) {
    val bodyLarge = MaterialTheme.typography.bodyLarge
    EmberrTextField(
        value = draft.value,
        onValueChange = onValueChange,
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
                CredentialEditorIconButton(
                    painter = rememberVectorPainter(
                        if (draft.isValueVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility
                    ),
                    description = if (draft.isValueVisible) "Hide" else "Show",
                    onClick = { draft.isValueVisible = !draft.isValueVisible }
                )
            }
        } else {
            null
        },
        textStyle = if (draft.kind.hidesValue) bodyLarge.copy(fontFamily = FontFamily.Monospace) else bodyLarge,
        modifier = modifier.focusRequester(draft.focusRequester)
    )
}

@Composable
private fun InvalidCodeKeyError() {
    Text(
        text = "This setup key isn't valid",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun RemoveFieldButton(onRemove: () -> Unit) {
    CredentialEditorIconButton(
        painter = painterResource(Res.drawable.trash),
        description = "Remove field",
        onClick = onRemove
    )
}

@Composable
private fun CredentialEditorIconButton(painter: Painter, description: String, onClick: () -> Unit) {
    IconTooltip(text = description) {
        Icon(
            painter = painter,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .padding(8.dp)
                .size(18.dp)
        )
    }
}

@Composable
private fun AddFieldButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
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
private fun AddCredentialFieldMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAddField: (CredentialFieldKind) -> Unit
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
            modifier = Modifier.width(220.dp)
        ) {
            CredentialFieldKind.entries.forEach { kind ->
                EmberrDesktopMenuOption(
                    label = kind.addFieldLabel(),
                    icon = { kindIcon(kind) },
                    onClick = {
                        onDismiss()
                        onAddField(kind)
                    }
                )
            }
        }
    } else {
        EmberrBottomSheet(
            expanded = expanded,
            onDismiss = onDismiss,
            title = "Add field",
            contentHorizontalPadding = 0.dp
        ) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                CredentialFieldKind.entries.forEach { kind ->
                    EmberrBottomSheetOption(
                        label = kind.addFieldLabel(),
                        icon = { kindIcon(kind) },
                        onClick = { closeAnd { onAddField(kind) } }
                    )
                }
            }
        }
    }
}
