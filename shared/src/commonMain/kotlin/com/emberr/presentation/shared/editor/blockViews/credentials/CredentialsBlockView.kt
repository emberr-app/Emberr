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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.emberr.domain.credentials.CredentialUnlockSession
import com.emberr.domain.credentials.CredentialVault
import com.emberr.domain.model.CredentialFieldKind
import com.emberr.domain.model.CredentialsBlock
import com.emberr.domain.model.hasNothingFilledIn
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.rememberDeviceLockPrompt
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.emberrBorderColor
import com.emberr.presentation.shared.editor.DefaultBlockShape
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.clock_circle
import emberr.shared.generated.resources.ellipsis
import emberr.shared.generated.resources.pen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

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
                CredentialsEditStart(decryptedSecretsByFieldId = emptyMap(), focusesNewOneTimeCode = false)
            } else {
                null
            }
        )
    }
    var isOpeningEditor by remember { mutableStateOf(false) }

    fun openEditor(addsOneTimeCode: Boolean) {
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
                editStart = CredentialsEditStart(decryptedSecretsByFieldId, focusesNewOneTimeCode = addsOneTimeCode)
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
        CredentialsPlaceholderCard(
            block = block,
            modifier = cardModifier,
            inSelectionMode = inSelectionMode,
            onToggleSelection = onToggleSelection,
            showsOptions = onSave != null && !inSelectionMode,
            onEditFields = { openEditor(addsOneTimeCode = false) },
            onAddOneTimeCode = { openEditor(addsOneTimeCode = true) },
            runAfterKeyboardCloses = runAfterKeyboardCloses
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CredentialsPlaceholderCard(
    block: CredentialsBlock,
    modifier: Modifier,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    showsOptions: Boolean,
    onEditFields: () -> Unit,
    onAddOneTimeCode: () -> Unit,
    runAfterKeyboardCloses: (action: () -> Unit) -> Unit
) {
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    var isMenuOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .hoverable(hoverSource)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (inSelectionMode) onToggleSelection() },
                onLongClick = if (isDesktopPlatform) null else onToggleSelection
            )
    ) {
        Text(
            text = block.serviceName.ifBlank { "Login" },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = if (showsOptions) 48.dp else 16.dp, top = 12.dp, bottom = 12.dp)
        )
        if (showsOptions) {
            Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                if (!isDesktopPlatform || isHovered || isMenuOpen) {
                    IconTooltip(text = "Options") {
                        Icon(
                            painter = painterResource(Res.drawable.ellipsis),
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { runAfterKeyboardCloses { isMenuOpen = true } }
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                }
                CredentialsOptionsMenu(
                    expanded = isMenuOpen,
                    title = block.serviceName.ifBlank { "Login" },
                    showsAddOneTimeCode = block.fields.none { field -> field.kind == CredentialFieldKind.ONE_TIME_CODE },
                    onDismiss = { isMenuOpen = false },
                    onEditFields = onEditFields,
                    onAddOneTimeCode = onAddOneTimeCode
                )
            }
        }
    }
}

@Composable
private fun CredentialsOptionsMenu(
    expanded: Boolean,
    title: String,
    showsAddOneTimeCode: Boolean,
    onDismiss: () -> Unit,
    onEditFields: () -> Unit,
    onAddOneTimeCode: () -> Unit
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
    val oneTimeCodeIcon = painterResource(Res.drawable.clock_circle)

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
            if (showsAddOneTimeCode) {
                EmberrDesktopMenuOption(
                    label = "Add one-time code",
                    icon = { optionIcon(oneTimeCodeIcon) },
                    onClick = {
                        onDismiss()
                        onAddOneTimeCode()
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
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                EmberrBottomSheetOption(
                    label = "Edit fields",
                    icon = { optionIcon(editIcon) },
                    onClick = { closeAnd(onEditFields) }
                )
                if (showsAddOneTimeCode) {
                    EmberrBottomSheetOption(
                        label = "Add one-time code",
                        icon = { optionIcon(oneTimeCodeIcon) },
                        onClick = { closeAnd(onAddOneTimeCode) }
                    )
                }
            }
        }
    }
}
