package com.emberr.presentation.settings.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.domain.cloud.account.CloudPasswordStrength
import com.emberr.domain.cloud.api.CloudSession
import com.emberr.domain.model.shortDateText
import com.emberr.domain.util.format.formatByteSize
import com.emberr.domain.util.system.DeviceLockConfirmation
import com.emberr.domain.util.system.rememberDeviceLockPrompt
import com.emberr.domain.util.system.showNativeToast
import com.emberr.domain.util.time.formatTimeOfDay
import com.emberr.presentation.settings.SettingsActionRow
import com.emberr.presentation.settings.SettingsDivider
import com.emberr.presentation.settings.SettingsGroup
import com.emberr.presentation.settings.SettingsValuePill
import com.emberr.presentation.settings.desktopSettingsSidePadding
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrCheckbox
import com.emberr.presentation.shared.components.EmberrSwitch
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.KmpBackHandler
import com.emberr.presentation.shared.components.topHeaderBarPadding
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.eye3
import emberr.shared.generated.resources.lock
import emberr.shared.generated.resources.mail
import emberr.shared.generated.resources.trash
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

@Composable
fun CloudAccountScreen(
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    viewModel: CloudAccountViewModel = koinViewModel()
) {
    val screenState by viewModel.screenState.collectAsState()

    val internalHazeState = remember { HazeState() }
    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    LaunchedEffect(viewModel) { viewModel.onScreenShown() }

    KmpBackHandler(enabled = screenState.canGoBack, onBack = viewModel::goBack)

    BoxWithConstraints(modifier = Modifier.fillMaxSize().imePadding()) {
        val sidePadding = desktopSettingsSidePadding(maxWidth)

        if (screenState.step == CloudAccountStep.LOADING) {
            LoadingIndicator(topBarHeightDp = topBarHeightDp)
        } else {
            CloudAccountContent(
                state = screenState,
                viewModel = viewModel,
                hazeState = internalHazeState,
                topBarHeightDp = topBarHeightDp,
                sidePadding = sidePadding
            )
        }

        if (showTopBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(10f)
                    .onGloballyPositioned { coordinates -> topBarHeightPx = coordinates.size.height.toFloat() }
            ) {
                EmberrTopHeaderBar(
                    title = "Emberr Cloud",
                    hazeState = internalHazeState,
                    contentPadding = topHeaderBarPadding(bottom = 16.dp),
                    backTooltip = "Back",
                    onBackClick = { if (screenState.canGoBack) viewModel.goBack() else onNavigateBack() }
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicator(topBarHeightDp: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = topBarHeightDp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
private fun CloudAccountContent(
    state: CloudAccountScreenState,
    viewModel: CloudAccountViewModel,
    hazeState: HazeState,
    topBarHeightDp: Dp,
    sidePadding: Dp
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(state = hazeState)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = sidePadding,
            top = topBarHeightDp + 8.dp,
            end = sidePadding,
            bottom = 48.dp
        )
    ) {
        if (state.isServerAddressShown) {
            item {
                ServerAddressGroup(state = state, viewModel = viewModel)
            }
        }

        item {
            when (state.step) {
                CloudAccountStep.LOADING -> Unit
                CloudAccountStep.WELCOME -> WelcomeStep(state = state, viewModel = viewModel)
                CloudAccountStep.SIGN_UP_EMAIL -> SignUpEmailStep(state = state, viewModel = viewModel)
                CloudAccountStep.SIGN_UP_DETAILS -> SignUpDetailsStep(state = state, viewModel = viewModel)
                CloudAccountStep.SIGN_UP_RECOVERY_WORDS -> SignUpRecoveryWordsStep(state = state, viewModel = viewModel)
                CloudAccountStep.LOG_IN -> LogInStep(state = state, viewModel = viewModel)
                CloudAccountStep.RECOVERY_EMAIL -> RecoveryEmailStep(state = state, viewModel = viewModel)
                CloudAccountStep.RECOVERY_WORDS -> RecoveryWordsStep(state = state, viewModel = viewModel)
                CloudAccountStep.RECOVERY_NEW_PASSWORD -> RecoveryNewPasswordStep(state = state, viewModel = viewModel)
                CloudAccountStep.SIGNED_IN -> SignedInStep(state = state, viewModel = viewModel)
                CloudAccountStep.CHANGE_PASSWORD -> ChangePasswordStep(state = state, viewModel = viewModel)
                CloudAccountStep.CHANGE_PASSWORD_CODE -> ChangePasswordCodeStep(state = state, viewModel = viewModel)
                CloudAccountStep.CHANGE_EMAIL -> ChangeEmailStep(state = state, viewModel = viewModel)
                CloudAccountStep.CHANGE_EMAIL_CODE -> ChangeEmailCodeStep(state = state, viewModel = viewModel)
                CloudAccountStep.RECOVERY_WORDS_PASSWORD -> RecoveryWordsPasswordStep(state = state, viewModel = viewModel)
                CloudAccountStep.SHOW_RECOVERY_WORDS -> ShowRecoveryWordsStep(state = state, viewModel = viewModel)
                CloudAccountStep.DELETE_ACCOUNT -> DeleteAccountStep(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun StepGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    SettingsGroup(title = title) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun ServerAddressGroup(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Server Address") {
        EmberrTextField(
            value = state.serverAddressInput,
            onValueChange = viewModel::onServerAddressChanged,
            placeholder = "http://localhost:8080",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
            onSubmit = viewModel::saveServerAddress
        )
        state.serverAddressError?.let { message -> ErrorText(message = message) }
        EmberrButtonSecondary(
            text = "Save",
            onClick = viewModel::saveServerAddress,
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WelcomeStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Emberr Cloud") {
        HintText(text = "Sync your notes between devices with end-to-end encryption. Only your devices can read them.")
        state.otherSyncMethodMessage?.let { message -> HintText(text = message) }
        state.errorMessage?.let { message -> ErrorText(message = message) }
        EmberrButtonPrimary(
            text = "Create account",
            onClick = viewModel::startSignUp,
            enabled = state.canStartSignIn,
            modifier = Modifier.fillMaxWidth()
        )
        EmberrButtonSecondary(
            text = "Log in",
            onClick = viewModel::startLogIn,
            enabled = state.canStartSignIn,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SignUpEmailStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Create account") {
        EmailField(email = state.email, onEmailChanged = viewModel::onEmailChanged, onSubmit = viewModel::sendSignupCode)
        StepActions(
            state = state,
            primaryText = "Send code",
            isPrimaryEnabled = state.canSendCode,
            onPrimaryClick = viewModel::sendSignupCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun SignUpDetailsStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Create account") {
        CodeSentText(email = state.email)
        CodeField(code = state.code, onCodeChanged = viewModel::onCodeChanged)
        NewPasswordFields(state = state, viewModel = viewModel, onSubmit = viewModel::createAccount)
        HintText(text = "We don't store this password, so if you forget it, we can't decrypt your notes.")
        StepActions(
            state = state,
            primaryText = "Create account",
            isPrimaryEnabled = state.canCreateAccount,
            onPrimaryClick = viewModel::createAccount,
            onSendNewCode = viewModel::sendSignupCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun SignUpRecoveryWordsStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Recovery words") {
        HintText(
            text = "If you forget your password, these 24 words are the only way back into your account. " +
                "We don't store them, so save them somewhere safe."
        )
        WarningText(text = "Without your password and these words, your cloud notes can't be recovered.")
        RecoveryWordsParagraph(words = state.newRecoveryWords)
        CopyRecoveryWordsButton(words = state.newRecoveryWords)
        CheckboxRow(
            text = "I have saved these words somewhere safe",
            isChecked = state.hasSavedRecoveryWords,
            onCheckedChange = viewModel::onSavedRecoveryWordsChanged
        )
        EmberrButtonPrimary(
            text = "Continue",
            onClick = viewModel::finishSignUp,
            enabled = state.canFinishSignUp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LogInStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Log in") {
        EmailField(email = state.email, onEmailChanged = viewModel::onEmailChanged, onSubmit = viewModel::logIn)
        PasswordField(
            password = state.password,
            onPasswordChanged = viewModel::onPasswordChanged,
            placeholder = "Password",
            onSubmit = viewModel::logIn
        )
        StepActions(
            state = state,
            primaryText = "Log in",
            isPrimaryEnabled = state.canLogIn,
            onPrimaryClick = viewModel::logIn,
            onBack = viewModel::goBack
        )
        EmberrButtonSecondary(
            text = "Forgot password",
            onClick = viewModel::startRecovery,
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RecoveryEmailStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Forgot password") {
        HintText(text = "Enter your account's email. We'll send you a code.")
        HintText(text = "Signed in on another device? Change your password there.")
        EmailField(email = state.email, onEmailChanged = viewModel::onEmailChanged, onSubmit = viewModel::sendRecoveryCode)
        StepActions(
            state = state,
            primaryText = "Send code",
            isPrimaryEnabled = state.canSendCode,
            onPrimaryClick = viewModel::sendRecoveryCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun RecoveryWordsStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Forgot password") {
        CodeSentText(email = state.email)
        CodeField(code = state.code, onCodeChanged = viewModel::onCodeChanged)
        HintText(text = "Enter the 24 recovery words you saved when you created your account.")
        EmberrTextField(
            value = state.recoveryWordsInput,
            onValueChange = viewModel::onRecoveryWordsChanged,
            placeholder = "Your 24 recovery words",
            singleLine = false,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth()
        )
        StepActions(
            state = state,
            primaryText = "Continue",
            isPrimaryEnabled = state.canCheckRecoveryWords,
            onPrimaryClick = viewModel::checkRecoveryWords,
            onSendNewCode = viewModel::sendRecoveryCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun RecoveryNewPasswordStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    var isAskingAboutOtherDevices by remember { mutableStateOf(false) }
    val askAboutOtherDevices: () -> Unit = { if (state.canSetNewPassword) isAskingAboutOtherDevices = true }

    StepGroup(title = "Set a new password") {
        HintText(text = "Enter a new password we can use to encrypt your notes.")
        NewPasswordFields(state = state, viewModel = viewModel, onSubmit = askAboutOtherDevices)
        StepActions(
            state = state,
            primaryText = "Reset password",
            isPrimaryEnabled = state.canSetNewPassword,
            onPrimaryClick = askAboutOtherDevices,
            onBack = viewModel::goBack
        )
    }

    if (isAskingAboutOtherDevices) {
        EmberrAlertDialog(
            onDismissRequest = { isAskingAboutOtherDevices = false },
            title = "Sign out from other devices"
        ) {
            Text(
                text = "If you think someone might know your password, you can force all other devices " +
                    "using your account to sign out.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(20.dp))
            EmberrButtonPrimary(
                text = "Sign out other devices",
                onClick = {
                    isAskingAboutOtherDevices = false
                    viewModel.setNewPassword(logOutOtherDevices = true)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            EmberrButtonSecondary(
                text = "Do not sign out",
                onClick = {
                    isAskingAboutOtherDevices = false
                    viewModel.setNewPassword(logOutOtherDevices = false)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SignedInStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current
    val deviceLockPrompt = rememberDeviceLockPrompt()
    val coroutineScope = rememberCoroutineScope()
    var sessionToSignOut by remember { mutableStateOf<CloudSession?>(null) }
    var isConfirmingSignOutOfThisDevice by remember { mutableStateOf(false) }
    var isExplainingAccountDeletion by remember { mutableStateOf(false) }

    Column {
        StepGroup(title = "Account") {
            Text(
                text = "Signed in as ${state.signedInEmail}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            when {
                state.storageUsedBytes != null -> HintText(text = "Storage used: ${formatByteSize(state.storageUsedBytes)}")
                state.accountDetailsError != null -> ErrorText(message = state.accountDetailsError)
                else -> HintText(text = "Loading storage used...")
            }
            state.errorMessage?.let { message -> ErrorText(message = message) }
            if (state.isWorking) WorkingIndicator()
        }

        StepGroup(title = "Sync") {
            val lastSyncedAtMillis = state.syncStatus.lastSyncedAtMillis
            when {
                state.syncStatus.isSyncing -> HintText(text = "Syncing...")
                lastSyncedAtMillis != null ->
                    HintText(text = "Last synced ${formatDateAndTime(lastSyncedAtMillis, usesTwentyFourHourClock)}")
                else -> HintText(text = "Sends and receives spaces, folders, notes, lists and chats.")
            }
            state.syncProblemMessage?.let { message -> ErrorText(message = message) }
            EmberrButtonSecondary(
                text = "Sync now",
                onClick = viewModel::syncNow,
                enabled = !state.syncStatus.isSyncing,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SettingsGroup(title = "Devices") {
            state.sessions?.forEachIndexed { index, session ->
                if (index > 0) SessionDivider()
                SessionRow(
                    session = session,
                    isEnabled = !state.isWorking,
                    onClick = { sessionToSignOut = session }
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    state.sessionsError != null -> ErrorText(message = state.sessionsError)
                    state.sessions == null -> HintText(text = "Loading devices...")
                    else -> HintText(text = "Tap a device to sign it out.")
                }
                EmberrButtonSecondary(
                    text = "Sign out of this device",
                    onClick = { isConfirmingSignOutOfThisDevice = true },
                    enabled = !state.isWorking,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SettingsGroup(title = "Manage Account") {
            SettingsActionRow(
                icon = painterResource(Res.drawable.lock),
                title = "Change password",
                isEnabled = !state.isWorking,
                onClick = viewModel::startChangePassword
            )
            SettingsDivider()
            SettingsActionRow(
                icon = painterResource(Res.drawable.mail),
                title = "Change email",
                isEnabled = !state.isWorking,
                onClick = viewModel::startChangeEmail
            )
            SettingsDivider()
            SettingsActionRow(
                icon = painterResource(Res.drawable.eye3),
                title = "Recovery words",
                isEnabled = !state.isWorking,
                onClick = {
                    coroutineScope.launch {
                        val confirmation = deviceLockPrompt.confirm(
                            title = "Recovery words",
                            description = "Please authenticate to view your recovery words"
                        )
                        when (confirmation) {
                            DeviceLockConfirmation.CONFIRMED -> viewModel.showRecoveryWords()
                            DeviceLockConfirmation.NO_DEVICE_LOCK -> viewModel.askPasswordForRecoveryWords()
                            DeviceLockConfirmation.NOT_CONFIRMED -> Unit
                        }
                    }
                }
            )
            SettingsDivider()
            SettingsActionRow(
                icon = painterResource(Res.drawable.trash),
                title = "Delete account",
                isDestructive = true,
                isEnabled = !state.isWorking,
                onClick = { isExplainingAccountDeletion = true }
            )
        }
    }

    sessionToSignOut?.let { session ->
        if (session.isCurrent) {
            SignOutOfThisDeviceDialog(
                onDismiss = { sessionToSignOut = null },
                onConfirm = {
                    sessionToSignOut = null
                    viewModel.signOutThisDevice()
                }
            )
        } else {
            ConfirmActionDialog(
                title = "Sign Out Device?",
                message = "This will sign out the following device:\n\n${session.deviceName}",
                confirmText = "Sign out",
                onDismiss = { sessionToSignOut = null },
                onConfirm = {
                    sessionToSignOut = null
                    viewModel.signOutSession(session)
                }
            )
        }
    }

    if (isConfirmingSignOutOfThisDevice) {
        SignOutOfThisDeviceDialog(
            onDismiss = { isConfirmingSignOutOfThisDevice = false },
            onConfirm = {
                isConfirmingSignOutOfThisDevice = false
                viewModel.signOutThisDevice()
            }
        )
    }

    if (isExplainingAccountDeletion) {
        ConfirmActionDialog(
            title = "Delete Account?",
            message = "Your Emberr Cloud account and everything stored in it will be deleted for good. " +
                "This can't be undone. The notes on this device stay, unless you also choose to clear them. " +
                "We'll send a code to ${state.signedInEmail} to confirm.",
            confirmText = "Send code",
            onDismiss = { isExplainingAccountDeletion = false },
            onConfirm = {
                isExplainingAccountDeletion = false
                viewModel.sendDeleteAccountCode()
            }
        )
    }
}

@Composable
private fun SessionRow(session: CloudSession, isEnabled: Boolean, onClick: () -> Unit) {
    val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = session.deviceName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            HintText(text = "Last used ${formatLastUsed(session.lastUsedAtMicroseconds, usesTwentyFourHourClock)}")
        }
        if (session.isCurrent) SettingsValuePill(label = "This device")
    }
}

@Composable
private fun SessionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    )
}

private fun formatLastUsed(lastUsedAtMicroseconds: Long, usesTwentyFourHourClock: Boolean): String =
    formatDateAndTime(lastUsedAtMicroseconds / 1_000L, usesTwentyFourHourClock)

private fun formatDateAndTime(epochMillis: Long, usesTwentyFourHourClock: Boolean): String {
    val local = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${shortDateText(local.date)}, ${formatTimeOfDay(local.hour, local.minute, usesTwentyFourHourClock)}"
}

@Composable
private fun SignOutOfThisDeviceDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    ConfirmActionDialog(
        title = "Sign Out of This Device?",
        message = "This device stops using Emberr Cloud. Your notes on this device are not touched.",
        confirmText = "Sign out",
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    EmberrAlertDialog(onDismissRequest = onDismiss, title = title) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EmberrButtonSecondary(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            EmberrButtonPrimary(
                text = confirmText,
                onClick = onConfirm,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ChangePasswordStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Change password") {
        HintText(text = "We'll send a code to ${state.signedInEmail} to confirm.")
        StepActions(
            state = state,
            primaryText = "Send code",
            isPrimaryEnabled = !state.isWorking,
            onPrimaryClick = viewModel::sendPasswordChangeCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun ChangePasswordCodeStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Change password") {
        CodeSentText(email = state.email)
        CodeField(code = state.code, onCodeChanged = viewModel::onCodeChanged)
        HintText(text = "Enter a new password we can use to encrypt your notes.")
        NewPasswordFields(state = state, viewModel = viewModel, onSubmit = viewModel::changePassword)
        SwitchRow(
            text = "Sign out of other devices",
            isOn = state.logOutOtherDevices,
            onChange = viewModel::onLogOutOtherDevicesChanged
        )
        StepActions(
            state = state,
            primaryText = "Change password",
            isPrimaryEnabled = state.canChangePassword,
            onPrimaryClick = viewModel::changePassword,
            onSendNewCode = viewModel::sendPasswordChangeCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun ChangeEmailStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Change email") {
        HintText(text = "Enter your new email address. We'll send a code there.")
        EmailField(email = state.email, onEmailChanged = viewModel::onEmailChanged, onSubmit = viewModel::sendChangeEmailCode)
        StepActions(
            state = state,
            primaryText = "Send code",
            isPrimaryEnabled = state.canSendCode,
            onPrimaryClick = viewModel::sendChangeEmailCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun ChangeEmailCodeStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Change email") {
        CodeSentText(email = state.email)
        CodeField(code = state.code, onCodeChanged = viewModel::onCodeChanged, onSubmit = viewModel::changeEmail)
        StepActions(
            state = state,
            primaryText = "Change email",
            isPrimaryEnabled = state.canChangeEmail,
            onPrimaryClick = viewModel::changeEmail,
            onSendNewCode = viewModel::sendChangeEmailCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun RecoveryWordsPasswordStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Recovery words") {
        HintText(text = "Enter your password to view your recovery words.")
        PasswordField(
            password = state.password,
            onPasswordChanged = viewModel::onPasswordChanged,
            placeholder = "Password",
            onSubmit = viewModel::showRecoveryWordsAfterPasswordCheck
        )
        StepActions(
            state = state,
            primaryText = "Continue",
            isPrimaryEnabled = state.canCheckPassword,
            onPrimaryClick = viewModel::showRecoveryWordsAfterPasswordCheck,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun ShowRecoveryWordsStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Recovery words") {
        HintText(
            text = "If you forget your password, these 24 words are the only way back into your account. " +
                "We don't store them, so save them somewhere safe."
        )
        RecoveryWordsParagraph(words = state.recoveryWordsToShow)
        CopyRecoveryWordsButton(words = state.recoveryWordsToShow)
        EmberrButtonPrimary(
            text = "Done",
            onClick = viewModel::goBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DeleteAccountStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Delete account") {
        CodeSentText(email = state.email)
        CodeField(code = state.code, onCodeChanged = viewModel::onCodeChanged)
        PasswordField(
            password = state.password,
            onPasswordChanged = viewModel::onPasswordChanged,
            placeholder = "Password",
            onSubmit = viewModel::deleteAccount
        )
        CheckboxRow(
            text = "Also clear the notes on this device",
            isChecked = state.clearNotesOnThisDevice,
            onCheckedChange = viewModel::onClearNotesOnThisDeviceChanged
        )
        WarningText(text = "Your Emberr Cloud account will be deleted for good. This can't be undone.")
        StepActions(
            state = state,
            primaryText = "Delete account",
            isPrimaryEnabled = state.canDeleteAccount,
            onPrimaryClick = viewModel::deleteAccount,
            onSendNewCode = viewModel::sendDeleteAccountCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun StepActions(
    state: CloudAccountScreenState,
    primaryText: String,
    isPrimaryEnabled: Boolean,
    onPrimaryClick: () -> Unit,
    onBack: () -> Unit,
    onSendNewCode: (() -> Unit)? = null
) {
    state.errorMessage?.let { message -> ErrorText(message = message) }
    state.newCodeSentMessage?.let { message -> HintText(text = message) }
    EmberrButtonPrimary(
        text = primaryText,
        onClick = onPrimaryClick,
        enabled = isPrimaryEnabled,
        modifier = Modifier.fillMaxWidth()
    )
    if (state.isWorking) WorkingIndicator()
    if (onSendNewCode != null) {
        EmberrButtonSecondary(
            text = "Send a new code",
            onClick = onSendNewCode,
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth()
        )
    }
    EmberrButtonSecondary(
        text = "Back",
        onClick = onBack,
        enabled = !state.isWorking,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EmailField(email: String, onEmailChanged: (String) -> Unit, onSubmit: () -> Unit) {
    EmberrTextField(
        value = email,
        onValueChange = onEmailChanged,
        placeholder = "Email",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
        onSubmit = onSubmit
    )
}

@Composable
private fun CodeField(code: String, onCodeChanged: (String) -> Unit, onSubmit: (() -> Unit)? = null) {
    EmberrTextField(
        value = code,
        onValueChange = onCodeChanged,
        placeholder = "6-digit code",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
        onSubmit = onSubmit
    )
}

@Composable
private fun NewPasswordFields(state: CloudAccountScreenState, viewModel: CloudAccountViewModel, onSubmit: () -> Unit) {
    PasswordField(
        password = state.password,
        onPasswordChanged = viewModel::onPasswordChanged,
        placeholder = "Password",
        onSubmit = onSubmit
    )
    PasswordField(
        password = state.passwordAgain,
        onPasswordChanged = viewModel::onPasswordAgainChanged,
        placeholder = "Confirm password",
        onSubmit = onSubmit
    )
    if (state.isPasswordStrengthKnown) PasswordStrengthText(strength = state.passwordStrength)
    if (state.passwordAgain.isNotEmpty() && !state.doPasswordsMatch) ErrorText(message = "Passwords don't match.")
}

@Composable
private fun PasswordField(
    password: String,
    onPasswordChanged: (String) -> Unit,
    placeholder: String,
    onSubmit: () -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    EmberrTextField(
        value = password,
        onValueChange = onPasswordChanged,
        placeholder = placeholder,
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconTooltip(text = if (isPasswordVisible) "Hide password" else "Show password") {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        onSubmit = onSubmit
    )
}

@Composable
private fun PasswordStrengthText(strength: CloudPasswordStrength) {
    val (label, color) = when (strength) {
        CloudPasswordStrength.WEAK -> "Weak" to MaterialTheme.colorScheme.error
        CloudPasswordStrength.MODERATE -> "Moderate" to ModerateColor
        CloudPasswordStrength.STRONG -> "Strong" to StrongColor
    }
    Text(
        text = "Password strength: $label",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = color
    )
}

@Composable
private fun RecoveryWordsParagraph(words: String) {
    SelectionContainer {
        Text(
            text = words,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun CopyRecoveryWordsButton(words: String) {
    val clipboardManager = LocalClipboardManager.current

    EmberrButtonSecondary(
        text = "Copy words",
        onClick = {
            clipboardManager.setText(AnnotatedString(words))
            showNativeToast("Recovery words copied")
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CheckboxRow(text: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        EmberrCheckbox(checked = isChecked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SwitchRow(text: String, isOn: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onChange(!isOn) }.padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        EmberrSwitch(isOn = isOn)
    }
}

@Composable
private fun CodeSentText(email: String) {
    HintText(text = "We sent a code to $email. Check your inbox (and spam).")
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
}

@Composable
private fun WarningText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun WorkingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        HintText(text = "Please wait...")
    }
}

private val ModerateColor = Color(0xFFFFA726)

private val StrongColor = Color(0xFF4CAF50)
