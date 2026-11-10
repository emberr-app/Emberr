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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.domain.cloud.account.CloudPasswordStrength
import com.emberr.domain.cloud.api.CloudAccountDetails
import com.emberr.domain.cloud.api.CloudSession
import com.emberr.domain.cloud.sync.CloudMediaPassDetails
import com.emberr.domain.cloud.sync.CloudMediaPassResult
import com.emberr.domain.cloud.sync.CloudSyncResult
import com.emberr.domain.util.format.formatByteSize
import com.emberr.domain.util.media.mimeTypeForExtension
import com.emberr.domain.util.system.DeviceLockConfirmation
import com.emberr.domain.util.system.rememberDeviceLockPrompt
import com.emberr.domain.util.system.showNativeToast
import com.emberr.domain.util.time.formatTimeOfDay
import com.emberr.presentation.settings.SettingsActionRow
import com.emberr.presentation.settings.SettingsDivider
import com.emberr.presentation.settings.SettingsGroup
import com.emberr.presentation.settings.SettingsValuePill
import com.emberr.presentation.settings.desktopSettingsSidePadding
import com.emberr.presentation.settings.selfhost.SyncProgressDetails
import com.emberr.presentation.settings.selfhost.describeImageBytes
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrCheckbox
import com.emberr.presentation.shared.components.EmberrSwitch
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.EmberrTextFieldIconButton
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.KmpBackHandler
import com.emberr.presentation.shared.components.topHeaderBarPadding
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import com.emberr.presentation.shared.components.orHighContrastLine
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.cloud_account
import emberr.shared.generated.resources.cloud_audio_in_note
import emberr.shared.generated.resources.cloud_change_email
import emberr.shared.generated.resources.cloud_change_email_hint
import emberr.shared.generated.resources.cloud_change_password
import emberr.shared.generated.resources.cloud_change_password_code_hint
import emberr.shared.generated.resources.cloud_clear_notes_on_this_device
import emberr.shared.generated.resources.cloud_code_placeholder
import emberr.shared.generated.resources.cloud_code_sent
import emberr.shared.generated.resources.cloud_confirm_password
import emberr.shared.generated.resources.cloud_continue
import emberr.shared.generated.resources.cloud_copy_words
import emberr.shared.generated.resources.cloud_create_account
import emberr.shared.generated.resources.cloud_delete_account
import emberr.shared.generated.resources.cloud_delete_account_message
import emberr.shared.generated.resources.cloud_delete_account_title
import emberr.shared.generated.resources.cloud_delete_account_warning
import emberr.shared.generated.resources.cloud_devices
import emberr.shared.generated.resources.cloud_do_not_sign_out
import emberr.shared.generated.resources.cloud_document_in_note
import emberr.shared.generated.resources.cloud_done
import emberr.shared.generated.resources.cloud_email
import emberr.shared.generated.resources.cloud_file_in_note
import emberr.shared.generated.resources.cloud_file_progress
import emberr.shared.generated.resources.cloud_files_not_received
import emberr.shared.generated.resources.cloud_files_not_sent
import emberr.shared.generated.resources.cloud_files_not_sent_and_not_received
import emberr.shared.generated.resources.cloud_files_received_count
import emberr.shared.generated.resources.cloud_files_sent_and_received_counts
import emberr.shared.generated.resources.cloud_files_sent_count
import emberr.shared.generated.resources.cloud_files_waiting_for_upload
import emberr.shared.generated.resources.cloud_forgot_password
import emberr.shared.generated.resources.cloud_hide_password
import emberr.shared.generated.resources.cloud_last_synced
import emberr.shared.generated.resources.cloud_last_used
import emberr.shared.generated.resources.cloud_loading_devices
import emberr.shared.generated.resources.cloud_loading_storage_used
import emberr.shared.generated.resources.cloud_log_in
import emberr.shared.generated.resources.cloud_manage_account
import emberr.shared.generated.resources.cloud_media
import emberr.shared.generated.resources.cloud_media_last_checked
import emberr.shared.generated.resources.cloud_new_password_hint
import emberr.shared.generated.resources.cloud_password
import emberr.shared.generated.resources.cloud_password_not_stored_hint
import emberr.shared.generated.resources.cloud_password_strength_moderate
import emberr.shared.generated.resources.cloud_password_strength_strong
import emberr.shared.generated.resources.cloud_password_strength_weak
import emberr.shared.generated.resources.cloud_passwords_do_not_match
import emberr.shared.generated.resources.cloud_photo_in_note
import emberr.shared.generated.resources.cloud_please_wait
import emberr.shared.generated.resources.cloud_receiving_file
import emberr.shared.generated.resources.cloud_recovery_email_hint
import emberr.shared.generated.resources.cloud_recovery_other_device_hint
import emberr.shared.generated.resources.cloud_recovery_words
import emberr.shared.generated.resources.cloud_recovery_words_copied
import emberr.shared.generated.resources.cloud_recovery_words_entry_hint
import emberr.shared.generated.resources.cloud_recovery_words_explanation
import emberr.shared.generated.resources.cloud_recovery_words_lock_description
import emberr.shared.generated.resources.cloud_recovery_words_password_hint
import emberr.shared.generated.resources.cloud_recovery_words_placeholder
import emberr.shared.generated.resources.cloud_recovery_words_saved_checkbox
import emberr.shared.generated.resources.cloud_recovery_words_warning
import emberr.shared.generated.resources.cloud_reset_password
import emberr.shared.generated.resources.cloud_send_code
import emberr.shared.generated.resources.cloud_send_new_code
import emberr.shared.generated.resources.cloud_sending_file
import emberr.shared.generated.resources.cloud_server_address
import emberr.shared.generated.resources.cloud_set_new_password
import emberr.shared.generated.resources.cloud_show_password
import emberr.shared.generated.resources.cloud_sign_out
import emberr.shared.generated.resources.cloud_sign_out_device_message
import emberr.shared.generated.resources.cloud_sign_out_device_title
import emberr.shared.generated.resources.cloud_sign_out_of_other_devices
import emberr.shared.generated.resources.cloud_sign_out_of_this_device
import emberr.shared.generated.resources.cloud_sign_out_of_this_device_message
import emberr.shared.generated.resources.cloud_sign_out_of_this_device_title
import emberr.shared.generated.resources.cloud_sign_out_other_devices_button
import emberr.shared.generated.resources.cloud_sign_out_other_devices_message
import emberr.shared.generated.resources.cloud_sign_out_other_devices_title
import emberr.shared.generated.resources.cloud_signed_in_as
import emberr.shared.generated.resources.cloud_sync
import emberr.shared.generated.resources.cloud_sync_explanation
import emberr.shared.generated.resources.cloud_sync_now
import emberr.shared.generated.resources.cloud_syncing
import emberr.shared.generated.resources.cloud_tap_device_to_sign_out
import emberr.shared.generated.resources.cloud_this_device
import emberr.shared.generated.resources.cloud_title
import emberr.shared.generated.resources.cloud_video_in_note
import emberr.shared.generated.resources.cloud_welcome_hint
import emberr.shared.generated.resources.common_back
import emberr.shared.generated.resources.common_cancel
import emberr.shared.generated.resources.common_save
import emberr.shared.generated.resources.eye_closed
import emberr.shared.generated.resources.eye_open
import emberr.shared.generated.resources.lock
import emberr.shared.generated.resources.mail
import emberr.shared.generated.resources.trash
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt
import kotlin.time.Instant
import com.emberr.domain.util.time.formatDate
import com.emberr.domain.util.time.DateTextStyle

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
    DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }

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
                    title = stringResource(Res.string.cloud_title),
                    hazeState = internalHazeState,
                    contentPadding = topHeaderBarPadding(bottom = 16.dp),
                    backTooltip = stringResource(Res.string.common_back),
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
    StepGroup(title = stringResource(Res.string.cloud_server_address)) {
        EmberrTextField(
            value = state.serverAddressInput,
            onValueChange = viewModel::onServerAddressChanged,
            placeholder = "http://localhost:8080",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
            onSubmit = viewModel::saveServerAddress
        )
        state.serverAddressError?.let { message -> ErrorText(message = stringResource(message)) }
        EmberrButtonSecondary(
            text = stringResource(Res.string.common_save),
            onClick = viewModel::saveServerAddress,
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun WelcomeStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = stringResource(Res.string.cloud_title)) {
        HintText(text = stringResource(Res.string.cloud_welcome_hint))
        state.otherSyncMethodMessage?.let { message -> HintText(text = message) }
        state.errorMessage?.let { message -> ErrorText(message = message) }
        EmberrButtonPrimary(
            text = stringResource(Res.string.cloud_create_account),
            onClick = viewModel::startSignUp,
            enabled = state.canStartSignIn,
            modifier = Modifier.fillMaxWidth()
        )
        EmberrButtonSecondary(
            text = stringResource(Res.string.cloud_log_in),
            onClick = viewModel::startLogIn,
            enabled = state.canStartSignIn,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SignUpEmailStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = stringResource(Res.string.cloud_create_account)) {
        EmailField(email = state.email, onEmailChanged = viewModel::onEmailChanged, onSubmit = viewModel::sendSignupCode)
        StepActions(
            state = state,
            primaryText = stringResource(Res.string.cloud_send_code),
            isPrimaryEnabled = state.canSendCode,
            onPrimaryClick = viewModel::sendSignupCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun SignUpDetailsStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = stringResource(Res.string.cloud_create_account)) {
        CodeSentText(email = state.email)
        CodeField(code = state.code, onCodeChanged = viewModel::onCodeChanged)
        NewPasswordFields(state = state, viewModel = viewModel, onSubmit = viewModel::createAccount)
        HintText(text = stringResource(Res.string.cloud_password_not_stored_hint))
        StepActions(
            state = state,
            primaryText = stringResource(Res.string.cloud_create_account),
            isPrimaryEnabled = state.canCreateAccount,
            onPrimaryClick = viewModel::createAccount,
            onSendNewCode = viewModel::sendSignupCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun SignUpRecoveryWordsStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = stringResource(Res.string.cloud_recovery_words)) {
        HintText(text = stringResource(Res.string.cloud_recovery_words_explanation))
        WarningText(text = stringResource(Res.string.cloud_recovery_words_warning))
        RecoveryWordsParagraph(words = state.newRecoveryWords)
        CopyRecoveryWordsButton(words = state.newRecoveryWords)
        CheckboxRow(
            text = stringResource(Res.string.cloud_recovery_words_saved_checkbox),
            isChecked = state.hasSavedRecoveryWords,
            onCheckedChange = viewModel::onSavedRecoveryWordsChanged
        )
        EmberrButtonPrimary(
            text = stringResource(Res.string.cloud_continue),
            onClick = viewModel::finishSignUp,
            enabled = state.canFinishSignUp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LogInStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = stringResource(Res.string.cloud_log_in)) {
        EmailField(email = state.email, onEmailChanged = viewModel::onEmailChanged, onSubmit = viewModel::logIn)
        PasswordField(
            password = state.password,
            onPasswordChanged = viewModel::onPasswordChanged,
            placeholder = stringResource(Res.string.cloud_password),
            onSubmit = viewModel::logIn
        )
        StepActions(
            state = state,
            primaryText = stringResource(Res.string.cloud_log_in),
            isPrimaryEnabled = state.canLogIn,
            onPrimaryClick = viewModel::logIn,
            onBack = viewModel::goBack
        )
        EmberrButtonSecondary(
            text = stringResource(Res.string.cloud_forgot_password),
            onClick = viewModel::startRecovery,
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RecoveryEmailStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = stringResource(Res.string.cloud_forgot_password)) {
        HintText(text = stringResource(Res.string.cloud_recovery_email_hint))
        HintText(text = stringResource(Res.string.cloud_recovery_other_device_hint))
        EmailField(email = state.email, onEmailChanged = viewModel::onEmailChanged, onSubmit = viewModel::sendRecoveryCode)
        StepActions(
            state = state,
            primaryText = stringResource(Res.string.cloud_send_code),
            isPrimaryEnabled = state.canSendCode,
            onPrimaryClick = viewModel::sendRecoveryCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun RecoveryWordsStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = stringResource(Res.string.cloud_forgot_password)) {
        CodeSentText(email = state.email)
        CodeField(code = state.code, onCodeChanged = viewModel::onCodeChanged)
        HintText(text = stringResource(Res.string.cloud_recovery_words_entry_hint))
        EmberrTextField(
            value = state.recoveryWordsInput,
            onValueChange = viewModel::onRecoveryWordsChanged,
            placeholder = stringResource(Res.string.cloud_recovery_words_placeholder),
            singleLine = false,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth()
        )
        StepActions(
            state = state,
            primaryText = stringResource(Res.string.cloud_continue),
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

    StepGroup(title = stringResource(Res.string.cloud_set_new_password)) {
        HintText(text = stringResource(Res.string.cloud_new_password_hint))
        NewPasswordFields(state = state, viewModel = viewModel, onSubmit = askAboutOtherDevices)
        StepActions(
            state = state,
            primaryText = stringResource(Res.string.cloud_reset_password),
            isPrimaryEnabled = state.canSetNewPassword,
            onPrimaryClick = askAboutOtherDevices,
            onBack = viewModel::goBack
        )
    }

    if (isAskingAboutOtherDevices) {
        EmberrAlertDialog(
            onDismissRequest = { isAskingAboutOtherDevices = false },
            title = stringResource(Res.string.cloud_sign_out_other_devices_title)
        ) {
            Text(
                text = stringResource(Res.string.cloud_sign_out_other_devices_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(20.dp))
            EmberrButtonPrimary(
                text = stringResource(Res.string.cloud_sign_out_other_devices_button),
                onClick = {
                    isAskingAboutOtherDevices = false
                    viewModel.setNewPassword(logOutOtherDevices = true)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            EmberrButtonSecondary(
                text = stringResource(Res.string.cloud_do_not_sign_out),
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
    val recoveryWordsTitle = stringResource(Res.string.cloud_recovery_words)
    val recoveryWordsLockDescription = stringResource(Res.string.cloud_recovery_words_lock_description)

    Column {
        StepGroup(title = stringResource(Res.string.cloud_account)) {
            Text(
                text = stringResource(Res.string.cloud_signed_in_as, state.signedInEmail),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            when {
                state.accountDetails != null -> PlanAndStorage(accountDetails = state.accountDetails, planText = state.planText?.asText())
                state.accountDetailsError != null -> ErrorText(message = state.accountDetailsError)
                else -> HintText(text = stringResource(Res.string.cloud_loading_storage_used))
            }
            state.errorMessage?.let { message -> ErrorText(message = message) }
            if (state.isWorking) WorkingIndicator()
        }

        StepGroup(title = stringResource(Res.string.cloud_sync)) {
            val lastSyncedAtMillis = state.syncStatus.lastSyncedAtMillis
            when {
                state.syncStatus.isSyncing -> HintText(text = stringResource(Res.string.cloud_syncing))
                lastSyncedAtMillis != null ->
                    HintText(text = stringResource(Res.string.cloud_last_synced, formatDateAndTime(lastSyncedAtMillis, usesTwentyFourHourClock)))
                else -> HintText(text = stringResource(Res.string.cloud_sync_explanation))
            }
            state.syncProblemMessage?.let { message -> ErrorText(message = message.asText()) }
            EmberrButtonSecondary(
                text = stringResource(Res.string.cloud_sync_now),
                onClick = viewModel::syncNow,
                enabled = !state.syncStatus.isSyncing,
                modifier = Modifier.fillMaxWidth()
            )
            state.mediaSyncProgress?.let { progress ->
                SyncProgressDetails(
                    title = stringResource(Res.string.cloud_media),
                    progress = progress,
                    describeAmount = { done, total -> describeImageBytes(done, total) }
                )
            }
            state.mediaPassDetails?.let { details -> MediaFileDetails(details) }
            val lastMediaPass = state.syncStatus.lastMediaPass
            if (state.mediaSyncProgress == null && lastMediaPass != null) {
                HintText(text = stringResource(Res.string.cloud_media_last_checked, formatDateAndTime(lastMediaPass.finishedAtMillis, usesTwentyFourHourClock)))
                lastMediaPass.uploadsRefusedReason
                    ?.takeIf { reason -> state.syncStatus.lastProblem != CloudSyncResult.UploadsRefused(reason) }
                    ?.let { reason -> ErrorText(message = uploadsRefusedMessage(reason, state.accountDetails).asText()) }
                filesTooLargeMessage(lastMediaPass.filesTooLargeCount)?.let { message -> ErrorText(message = message.asText()) }
                describeFilesNotMoved(lastMediaPass)?.let { message -> ErrorText(message = message.asText()) }
                lastMediaPass.lastProblem?.let { problem -> ErrorText(message = problem) }
            }
        }

        SettingsGroup(title = stringResource(Res.string.cloud_devices)) {
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
                    state.sessions == null -> HintText(text = stringResource(Res.string.cloud_loading_devices))
                    else -> HintText(text = stringResource(Res.string.cloud_tap_device_to_sign_out))
                }
                EmberrButtonSecondary(
                    text = stringResource(Res.string.cloud_sign_out_of_this_device),
                    onClick = { isConfirmingSignOutOfThisDevice = true },
                    enabled = !state.isWorking,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SettingsGroup(title = stringResource(Res.string.cloud_manage_account)) {
            SettingsActionRow(
                icon = painterResource(Res.drawable.lock),
                title = stringResource(Res.string.cloud_change_password),
                isEnabled = !state.isWorking,
                onClick = viewModel::startChangePassword
            )
            SettingsDivider()
            SettingsActionRow(
                icon = painterResource(Res.drawable.mail),
                title = stringResource(Res.string.cloud_change_email),
                isEnabled = !state.isWorking,
                onClick = viewModel::startChangeEmail
            )
            SettingsDivider()
            SettingsActionRow(
                icon = painterResource(Res.drawable.eye_open),
                title = recoveryWordsTitle,
                isEnabled = !state.isWorking,
                onClick = {
                    coroutineScope.launch {
                        val confirmation = deviceLockPrompt.confirm(
                            title = recoveryWordsTitle,
                            description = recoveryWordsLockDescription
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
                title = stringResource(Res.string.cloud_delete_account),
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
                title = stringResource(Res.string.cloud_sign_out_device_title),
                message = stringResource(Res.string.cloud_sign_out_device_message, session.deviceName),
                confirmText = stringResource(Res.string.cloud_sign_out),
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
            title = stringResource(Res.string.cloud_delete_account_title),
            message = stringResource(Res.string.cloud_delete_account_message, state.signedInEmail),
            confirmText = stringResource(Res.string.cloud_send_code),
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
            HintText(text = stringResource(Res.string.cloud_last_used, formatLastUsed(session.lastUsedAtMicroseconds, usesTwentyFourHourClock)))
        }
        if (session.isCurrent) SettingsValuePill(label = stringResource(Res.string.cloud_this_device))
    }
}

@Composable
private fun SessionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f).orHighContrastLine()
    )
}

private fun formatLastUsed(lastUsedAtMicroseconds: Long, usesTwentyFourHourClock: Boolean): String =
    formatDateAndTime(lastUsedAtMicroseconds / 1_000L, usesTwentyFourHourClock)

private fun formatDateAndTime(epochMillis: Long, usesTwentyFourHourClock: Boolean): String {
    val local = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${formatDate(local.date, DateTextStyle.SHORT_MONTH_DAY_YEAR)}, ${formatTimeOfDay(local.hour, local.minute, usesTwentyFourHourClock)}"
}

@Composable
private fun SignOutOfThisDeviceDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    ConfirmActionDialog(
        title = stringResource(Res.string.cloud_sign_out_of_this_device_title),
        message = stringResource(Res.string.cloud_sign_out_of_this_device_message),
        confirmText = stringResource(Res.string.cloud_sign_out),
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
                text = stringResource(Res.string.common_cancel),
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
    StepGroup(title = stringResource(Res.string.cloud_change_password)) {
        HintText(text = stringResource(Res.string.cloud_change_password_code_hint, state.signedInEmail))
        StepActions(
            state = state,
            primaryText = stringResource(Res.string.cloud_send_code),
            isPrimaryEnabled = !state.isWorking,
            onPrimaryClick = viewModel::sendPasswordChangeCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun ChangePasswordCodeStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = stringResource(Res.string.cloud_change_password)) {
        CodeSentText(email = state.email)
        CodeField(code = state.code, onCodeChanged = viewModel::onCodeChanged)
        HintText(text = stringResource(Res.string.cloud_new_password_hint))
        NewPasswordFields(state = state, viewModel = viewModel, onSubmit = viewModel::changePassword)
        SwitchRow(
            text = stringResource(Res.string.cloud_sign_out_of_other_devices),
            isOn = state.logOutOtherDevices,
            onChange = viewModel::onLogOutOtherDevicesChanged
        )
        StepActions(
            state = state,
            primaryText = stringResource(Res.string.cloud_change_password),
            isPrimaryEnabled = state.canChangePassword,
            onPrimaryClick = viewModel::changePassword,
            onSendNewCode = viewModel::sendPasswordChangeCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun ChangeEmailStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = stringResource(Res.string.cloud_change_email)) {
        HintText(text = stringResource(Res.string.cloud_change_email_hint))
        EmailField(email = state.email, onEmailChanged = viewModel::onEmailChanged, onSubmit = viewModel::sendChangeEmailCode)
        StepActions(
            state = state,
            primaryText = stringResource(Res.string.cloud_send_code),
            isPrimaryEnabled = state.canSendCode,
            onPrimaryClick = viewModel::sendChangeEmailCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun ChangeEmailCodeStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = stringResource(Res.string.cloud_change_email)) {
        CodeSentText(email = state.email)
        CodeField(code = state.code, onCodeChanged = viewModel::onCodeChanged, onSubmit = viewModel::changeEmail)
        StepActions(
            state = state,
            primaryText = stringResource(Res.string.cloud_change_email),
            isPrimaryEnabled = state.canChangeEmail,
            onPrimaryClick = viewModel::changeEmail,
            onSendNewCode = viewModel::sendChangeEmailCode,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun RecoveryWordsPasswordStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = stringResource(Res.string.cloud_recovery_words)) {
        HintText(text = stringResource(Res.string.cloud_recovery_words_password_hint))
        PasswordField(
            password = state.password,
            onPasswordChanged = viewModel::onPasswordChanged,
            placeholder = stringResource(Res.string.cloud_password),
            onSubmit = viewModel::showRecoveryWordsAfterPasswordCheck
        )
        StepActions(
            state = state,
            primaryText = stringResource(Res.string.cloud_continue),
            isPrimaryEnabled = state.canCheckPassword,
            onPrimaryClick = viewModel::showRecoveryWordsAfterPasswordCheck,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun ShowRecoveryWordsStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = stringResource(Res.string.cloud_recovery_words)) {
        HintText(text = stringResource(Res.string.cloud_recovery_words_explanation))
        RecoveryWordsParagraph(words = state.recoveryWordsToShow)
        CopyRecoveryWordsButton(words = state.recoveryWordsToShow)
        EmberrButtonPrimary(
            text = stringResource(Res.string.cloud_done),
            onClick = viewModel::goBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DeleteAccountStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = stringResource(Res.string.cloud_delete_account)) {
        CodeSentText(email = state.email)
        CodeField(code = state.code, onCodeChanged = viewModel::onCodeChanged)
        PasswordField(
            password = state.password,
            onPasswordChanged = viewModel::onPasswordChanged,
            placeholder = stringResource(Res.string.cloud_password),
            onSubmit = viewModel::deleteAccount
        )
        CheckboxRow(
            text = stringResource(Res.string.cloud_clear_notes_on_this_device),
            isChecked = state.clearNotesOnThisDevice,
            onCheckedChange = viewModel::onClearNotesOnThisDeviceChanged
        )
        WarningText(text = stringResource(Res.string.cloud_delete_account_warning))
        StepActions(
            state = state,
            primaryText = stringResource(Res.string.cloud_delete_account),
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
            text = stringResource(Res.string.cloud_send_new_code),
            onClick = onSendNewCode,
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth()
        )
    }
    EmberrButtonSecondary(
        text = stringResource(Res.string.common_back),
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
        placeholder = stringResource(Res.string.cloud_email),
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
        placeholder = stringResource(Res.string.cloud_code_placeholder),
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
        placeholder = stringResource(Res.string.cloud_password),
        onSubmit = onSubmit
    )
    PasswordField(
        password = state.passwordAgain,
        onPasswordChanged = viewModel::onPasswordAgainChanged,
        placeholder = stringResource(Res.string.cloud_confirm_password),
        onSubmit = onSubmit
    )
    if (state.isPasswordStrengthKnown) PasswordStrengthText(strength = state.passwordStrength)
    if (state.passwordAgain.isNotEmpty() && !state.doPasswordsMatch) ErrorText(message = stringResource(Res.string.cloud_passwords_do_not_match))
}

@Composable
internal fun PasswordField(
    password: String,
    onPasswordChanged: (String) -> Unit,
    placeholder: String,
    onSubmit: () -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    val passwordVisibilityLabel =
        if (isPasswordVisible) stringResource(Res.string.cloud_hide_password) else stringResource(Res.string.cloud_show_password)

    EmberrTextField(
        value = password,
        onValueChange = onPasswordChanged,
        placeholder = placeholder,
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconTooltip(text = passwordVisibilityLabel) {
                EmberrTextFieldIconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        painter = painterResource(if (isPasswordVisible) Res.drawable.eye_closed else Res.drawable.eye_open),
                        contentDescription = passwordVisibilityLabel,
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
        CloudPasswordStrength.WEAK -> Res.string.cloud_password_strength_weak to MaterialTheme.colorScheme.error
        CloudPasswordStrength.MODERATE -> Res.string.cloud_password_strength_moderate to ModerateColor
        CloudPasswordStrength.STRONG -> Res.string.cloud_password_strength_strong to StrongColor
    }
    Text(
        text = stringResource(label),
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
    val copiedMessage = stringResource(Res.string.cloud_recovery_words_copied)

    EmberrButtonSecondary(
        text = stringResource(Res.string.cloud_copy_words),
        onClick = {
            clipboardManager.setText(AnnotatedString(words))
            showNativeToast(copiedMessage)
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
    HintText(text = stringResource(Res.string.cloud_code_sent, email))
}

@Composable
private fun PlanAndStorage(accountDetails: CloudAccountDetails, planText: String?) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        planText?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        HintText(text = cloudStorageUsedText(accountDetails).asText())
        LinearProgressIndicator(
            progress = { cloudStorageUsedFraction(accountDetails) },
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MediaFileDetails(details: CloudMediaPassDetails) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        describeFileCounts(details)?.let { counts -> HintText(text = counts.asText()) }
        details.fileInTransfer?.let { file ->
            val fileNumber = if (file.isUpload) details.filesSent + 1 else details.filesReceived + 1
            val fileCount = if (file.isUpload) details.filesToSend else details.filesToReceive
            val doneFraction = if (file.sizeBytes > 0) (file.bytesDone.toFloat() / file.sizeBytes).coerceIn(0f, 1f) else 1f
            Text(
                text = stringResource(
                    if (file.isUpload) Res.string.cloud_sending_file else Res.string.cloud_receiving_file,
                    minOf(fileNumber, fileCount),
                    fileCount
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Text(
                text = stringResource(mediaInNoteText(file.fileName), file.noteTitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            HintText(
                text = stringResource(Res.string.cloud_file_progress, (doneFraction * 100).roundToInt(), formatByteSize(file.bytesDone), formatByteSize(file.sizeBytes))
            )
            LinearProgressIndicator(
                progress = { doneFraction },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (details.filesWaitingForUpload > 0) {
            HintText(
                text = pluralStringResource(
                    Res.plurals.cloud_files_waiting_for_upload,
                    details.filesWaitingForUpload,
                    details.filesWaitingForUpload
                )
            )
        }
    }
}

private fun describeFilesNotMoved(passResult: CloudMediaPassResult): CloudText? {
    val notSent = passResult.filesNotSentCount
    val notReceived = passResult.filesNotReceivedCount
    return when {
        notSent > 0 && notReceived > 0 ->
            CloudText.Phrase(Res.string.cloud_files_not_sent_and_not_received, listOf(notSent, notReceived))
        notSent > 0 -> CloudText.Phrase(Res.string.cloud_files_not_sent, listOf(notSent))
        notReceived > 0 -> CloudText.Phrase(Res.string.cloud_files_not_received, listOf(notReceived))
        else -> null
    }
}

private fun describeFileCounts(details: CloudMediaPassDetails): CloudText? {
    val sentCounts = listOf(details.filesSent, details.filesToSend)
    val receivedCounts = listOf(details.filesReceived, details.filesToReceive)
    return when {
        details.filesToSend > 0 && details.filesToReceive > 0 ->
            CloudText.Phrase(Res.string.cloud_files_sent_and_received_counts, sentCounts + receivedCounts)
        details.filesToSend > 0 -> CloudText.Phrase(Res.string.cloud_files_sent_count, sentCounts)
        details.filesToReceive > 0 -> CloudText.Phrase(Res.string.cloud_files_received_count, receivedCounts)
        else -> null
    }
}

private fun mediaInNoteText(fileName: String): StringResource {
    val mimeType = mimeTypeForExtension(fileName.substringAfterLast('.', "")) ?: return Res.string.cloud_file_in_note
    return when {
        mimeType.startsWith("image/") -> Res.string.cloud_photo_in_note
        mimeType.startsWith("video/") -> Res.string.cloud_video_in_note
        mimeType.startsWith("audio/") -> Res.string.cloud_audio_in_note
        else -> Res.string.cloud_document_in_note
    }
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
        HintText(text = stringResource(Res.string.cloud_please_wait))
    }
}

internal val ModerateColor = Color(0xFFFFA726)

internal val StrongColor = Color(0xFF4CAF50)
