package com.emberr.presentation.settings.selfhost

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.domain.selfhost.sync.SelfHostSyncLog
import com.emberr.domain.selfhost.sync.SelfHostSyncProgress
import com.emberr.domain.selfhost.sync.SelfHostSyncStage
import com.emberr.domain.util.format.decimalText
import com.emberr.domain.util.system.DeviceLockConfirmation
import com.emberr.domain.util.system.rememberDeviceLockPrompt
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.settings.SettingsGroup
import com.emberr.presentation.settings.desktopSettingsSidePadding
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrCheckbox
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.EmberrTextFieldIconButton
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.topHeaderBarPadding
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.eye_closed
import emberr.shared.generated.resources.eye_open
import emberr.shared.generated.resources.refresh_cw
import emberr.shared.generated.resources.common_back
import emberr.shared.generated.resources.common_cancel
import emberr.shared.generated.resources.selfhost_cancel_sync
import emberr.shared.generated.resources.selfhost_cancel_sync_hint
import emberr.shared.generated.resources.selfhost_cancelling
import emberr.shared.generated.resources.selfhost_checking
import emberr.shared.generated.resources.selfhost_checking_for_changes
import emberr.shared.generated.resources.selfhost_contacting_server
import emberr.shared.generated.resources.selfhost_copied_address
import emberr.shared.generated.resources.selfhost_copy_address
import emberr.shared.generated.resources.selfhost_copy_passphrase
import emberr.shared.generated.resources.selfhost_create_vault_and_sync
import emberr.shared.generated.resources.selfhost_disconnect
import emberr.shared.generated.resources.selfhost_disconnect_message
import emberr.shared.generated.resources.selfhost_disconnect_vault
import emberr.shared.generated.resources.selfhost_disconnect_vault_question
import emberr.shared.generated.resources.selfhost_done
import emberr.shared.generated.resources.selfhost_fact_cannot_reset
import emberr.shared.generated.resources.selfhost_fact_locks_notes
import emberr.shared.generated.resources.selfhost_fact_see_again_later
import emberr.shared.generated.resources.selfhost_fact_type_on_other_devices
import emberr.shared.generated.resources.selfhost_finishing_up
import emberr.shared.generated.resources.selfhost_generate_new_passphrase
import emberr.shared.generated.resources.selfhost_got_it
import emberr.shared.generated.resources.selfhost_group_recovery_passphrase
import emberr.shared.generated.resources.selfhost_group_server_details
import emberr.shared.generated.resources.selfhost_group_status
import emberr.shared.generated.resources.selfhost_group_unlock_vault
import emberr.shared.generated.resources.selfhost_hide_passphrase
import emberr.shared.generated.resources.selfhost_hide_password
import emberr.shared.generated.resources.selfhost_megabytes_progress
import emberr.shared.generated.resources.selfhost_new_passphrase
import emberr.shared.generated.resources.selfhost_note_count_progress
import emberr.shared.generated.resources.selfhost_notes_waiting_to_sync
import emberr.shared.generated.resources.selfhost_nothing_to_receive
import emberr.shared.generated.resources.selfhost_nothing_to_send
import emberr.shared.generated.resources.selfhost_passphrase_needed_hint
import emberr.shared.generated.resources.selfhost_passphrase_placeholder
import emberr.shared.generated.resources.selfhost_password
import emberr.shared.generated.resources.selfhost_progress_images
import emberr.shared.generated.resources.selfhost_progress_notes
import emberr.shared.generated.resources.selfhost_receiving_percent
import emberr.shared.generated.resources.selfhost_recovery_dialog_message
import emberr.shared.generated.resources.selfhost_restore_find_hint
import emberr.shared.generated.resources.selfhost_restore_hint
import emberr.shared.generated.resources.selfhost_save_passphrase
import emberr.shared.generated.resources.selfhost_save_passphrase_hint
import emberr.shared.generated.resources.selfhost_save_passphrase_now
import emberr.shared.generated.resources.selfhost_saved_passphrase_confirmation
import emberr.shared.generated.resources.selfhost_sending_percent
import emberr.shared.generated.resources.selfhost_server_address_rejected
import emberr.shared.generated.resources.selfhost_server_details_hint
import emberr.shared.generated.resources.selfhost_setting_up
import emberr.shared.generated.resources.selfhost_show_passphrase
import emberr.shared.generated.resources.selfhost_show_passphrase_button
import emberr.shared.generated.resources.selfhost_show_password
import emberr.shared.generated.resources.selfhost_status_connected
import emberr.shared.generated.resources.selfhost_sync_now
import emberr.shared.generated.resources.selfhost_syncing
import emberr.shared.generated.resources.selfhost_test_connection
import emberr.shared.generated.resources.selfhost_testing
import emberr.shared.generated.resources.selfhost_title
import emberr.shared.generated.resources.selfhost_unlock_and_sync
import emberr.shared.generated.resources.selfhost_unlock_prompt_description
import emberr.shared.generated.resources.selfhost_unlock_prompt_title
import emberr.shared.generated.resources.selfhost_username
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock

@Composable
fun SelfHostSetupScreen(
    onNavigateBack: () -> Unit,
    showTopBar: Boolean = true,
    viewModel: SelfHostSetupViewModel = koinViewModel()
) {
    val screenState by viewModel.screenState.collectAsState()

    val internalHazeState = remember { HazeState() }
    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().imePadding()) {
        val sidePadding = desktopSettingsSidePadding(maxWidth)
        when (val state = screenState) {
            SelfHostScreenState.Checking -> CheckingIndicator(topBarHeightDp = topBarHeightDp)
            is SelfHostScreenState.Unconfigured -> SetupForm(
                form = state.form,
                viewModel = viewModel,
                hazeState = internalHazeState,
                topBarHeightDp = topBarHeightDp,
                sidePadding = sidePadding
            )
            is SelfHostScreenState.Connected -> ConnectedDashboard(
                state = state.connectedState,
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
                    title = stringResource(Res.string.selfhost_title),
                    hazeState = internalHazeState,
                    contentPadding = topHeaderBarPadding(bottom = 16.dp),
                    backTooltip = stringResource(Res.string.common_back),
                    onBackClick = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun CheckingIndicator(topBarHeightDp: Dp) {
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
private fun SetupForm(
    form: SelfHostSetupFormState,
    viewModel: SelfHostSetupViewModel,
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
        item {
            SettingsGroup(title = stringResource(Res.string.selfhost_group_server_details)) {
                ServerDetailsCard(form = form, viewModel = viewModel)
            }
        }

        if (form.vaultMode == VaultMode.CREATE_VAULT) {
            item {
                SettingsGroup(title = stringResource(Res.string.selfhost_group_recovery_passphrase)) {
                    NewPassphraseCard(
                        passphrase = form.passphrase,
                        hasSavedPassphrase = form.hasAcknowledgedRisk,
                        onHasSavedPassphraseChanged = viewModel::onAcknowledgeRiskChanged,
                        onRegenerate = viewModel::regeneratePassphrase
                    )
                }
            }
        }

        if (form.vaultMode == VaultMode.RESTORE_VAULT) {
            item {
                SettingsGroup(title = stringResource(Res.string.selfhost_group_unlock_vault)) {
                    RestorePassphraseCard(
                        passphraseInput = form.existingPassphraseInput,
                        onPassphraseChanged = viewModel::onExistingPassphraseChanged,
                        onSubmit = { if (form.canFinishSetup) viewModel.completeSetup() }
                    )
                }
            }
        }

        if (form.vaultMode != VaultMode.UNKNOWN) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    EmberrButtonPrimary(
                        text = when {
                            form.setupPhase == SetupPhase.FINALIZING -> stringResource(Res.string.selfhost_setting_up)
                            form.vaultMode == VaultMode.RESTORE_VAULT -> stringResource(Res.string.selfhost_unlock_and_sync)
                            else -> stringResource(Res.string.selfhost_create_vault_and_sync)
                        },
                        onClick = viewModel::completeSetup,
                        enabled = form.canFinishSetup,
                        showsWavingDots = form.setupPhase == SetupPhase.FINALIZING,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        form.errorMessage?.let { message ->
            item {
                ErrorMessageCard(message = message)
            }
        }
    }

    if (form.configurationWarningMessage != null) {
        EmberrAlertDialog(
            onDismissRequest = viewModel::dismissConfigurationWarning,
            title = stringResource(Res.string.selfhost_server_address_rejected)
        ) {
            Text(
                text = form.configurationWarningMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            form.movedServerAddress?.let { movedServerAddress ->
                Spacer(Modifier.height(16.dp))
                ServerAddressBox(serverUrl = movedServerAddress)
            }
            Spacer(Modifier.height(20.dp))
            EmberrButtonPrimary(
                text = stringResource(Res.string.selfhost_got_it),
                onClick = viewModel::dismissConfigurationWarning,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConnectedDashboard(
    state: SelfHostConnectedState,
    viewModel: SelfHostSetupViewModel,
    hazeState: HazeState,
    topBarHeightDp: Dp,
    sidePadding: Dp
) {
    var showDisconnectConfirmation by remember { mutableStateOf(false) }
    val notesWaitingToSync by viewModel.notesWaitingToSyncCount.collectAsState()
    val deviceLockPrompt = rememberDeviceLockPrompt()
    val coroutineScope = rememberCoroutineScope()
    val unlockPromptTitle = stringResource(Res.string.selfhost_unlock_prompt_title)
    val unlockPromptDescription = stringResource(Res.string.selfhost_unlock_prompt_description)

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
        item {
            SettingsGroup(title = stringResource(Res.string.selfhost_group_status)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.selfhost_status_connected),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ServerAddressBox(serverUrl = state.serverUrl)

                    Spacer(modifier = Modifier.height(16.dp))

                    val isSyncRunning = state.syncStatus != ManualSyncStatus.IDLE

                    if (isSyncRunning) {
                        EmberrButtonSecondary(
                            text = if (state.syncStatus == ManualSyncStatus.CANCELLING) {
                                stringResource(Res.string.selfhost_cancelling)
                            } else {
                                stringResource(Res.string.selfhost_cancel_sync)
                            },
                            onClick = {
                                SelfHostSyncLog.d("UI: Cancel Sync button tapped")
                                viewModel.cancelSync()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(Res.string.selfhost_cancel_sync_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    } else {
                        EmberrButtonPrimary(
                            text = stringResource(Res.string.selfhost_sync_now),
                            onClick = {
                                SelfHostSyncLog.d("UI: Sync Now button tapped")
                                viewModel.syncNow()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSyncRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isSyncRunning) {
                                stringResource(Res.string.selfhost_syncing)
                            } else {
                                formatLastSynced(state.lastSyncedAtMillis, LocalUsesTwentyFourHourClock.current)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    if (!isSyncRunning && notesWaitingToSync > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        SyncProgressHint(
                            text = pluralStringResource(
                                Res.plurals.selfhost_notes_waiting_to_sync,
                                notesWaitingToSync,
                                notesWaitingToSync
                            )
                        )
                    }

                    state.noteSyncProgress?.let { progress ->
                        Spacer(modifier = Modifier.height(14.dp))
                        SyncProgressDetails(
                            title = stringResource(Res.string.selfhost_progress_notes),
                            progress = progress,
                            describeAmount = { done, total -> describeNoteCount(done, total) }
                        )
                    }

                    state.imageSyncProgress?.let { progress ->
                        Spacer(modifier = Modifier.height(14.dp))
                        SyncProgressDetails(
                            title = stringResource(Res.string.selfhost_progress_images),
                            progress = progress,
                            describeAmount = { done, total -> describeImageBytes(done, total) }
                        )
                    }

                    state.syncError?.let { error ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        item {
            SettingsGroup(title = stringResource(Res.string.selfhost_group_recovery_passphrase)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.hasSavedRecoveryPassphrase) {
                        SettingsHint(text = stringResource(Res.string.selfhost_passphrase_needed_hint))
                        EmberrButtonSecondary(
                            text = stringResource(Res.string.selfhost_show_passphrase_button),
                            onClick = {
                                coroutineScope.launch {
                                    val confirmation = deviceLockPrompt.confirm(
                                        title = unlockPromptTitle,
                                        description = unlockPromptDescription
                                    )
                                    if (confirmation != DeviceLockConfirmation.NOT_CONFIRMED) {
                                        viewModel.showRecoveryPassphrase()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        SettingsHint(
                            text = stringResource(Res.string.selfhost_save_passphrase_hint)
                        )
                        PassphraseField(
                            value = state.passphraseToSaveInput,
                            onValueChange = viewModel::onPassphraseToSaveChanged,
                            onSubmit = viewModel::savePassphraseOnThisDevice
                        )
                        state.savePassphraseError?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        EmberrButtonSecondary(
                            text = if (state.isSavingPassphrase) {
                                stringResource(Res.string.selfhost_checking)
                            } else {
                                stringResource(Res.string.selfhost_save_passphrase)
                            },
                            onClick = viewModel::savePassphraseOnThisDevice,
                            enabled = state.canSavePassphrase,
                            showsWavingDots = state.isSavingPassphrase,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.07f))
                    .clickable(enabled = !state.isDisconnecting) { showDisconnectConfirmation = true }
                    .padding(horizontal = 14.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isDisconnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LinkOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(Res.string.selfhost_disconnect_vault),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    state.shownRecoveryPassphrase?.let { passphrase ->
        EmberrAlertDialog(
            onDismissRequest = viewModel::hideRecoveryPassphrase,
            title = stringResource(Res.string.selfhost_group_recovery_passphrase)
        ) {
            Text(
                text = stringResource(Res.string.selfhost_recovery_dialog_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(16.dp))
            PassphraseBox(passphrase = passphrase)
            Spacer(Modifier.height(16.dp))
            EmberrButtonPrimary(
                text = stringResource(Res.string.selfhost_done),
                onClick = viewModel::hideRecoveryPassphrase,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDisconnectConfirmation) {
        EmberrAlertDialog(
            onDismissRequest = { showDisconnectConfirmation = false },
            title = stringResource(Res.string.selfhost_disconnect_vault_question)
        ) {
            Text(
                text = stringResource(Res.string.selfhost_disconnect_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EmberrButtonSecondary(
                    text = stringResource(Res.string.common_cancel),
                    onClick = { showDisconnectConfirmation = false },
                    modifier = Modifier.weight(1f)
                )
                EmberrButtonPrimary(
                    text = stringResource(Res.string.selfhost_disconnect),
                    onClick = {
                        showDisconnectConfirmation = false
                        viewModel.disconnectVault()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun SyncProgressDetails(
    title: String,
    progress: SelfHostSyncProgress,
    describeAmount: @Composable (done: Long, total: Long) -> String
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        when (progress.stage) {
            SelfHostSyncStage.PREPARING -> SyncProgressHint(stringResource(Res.string.selfhost_checking_for_changes))
            SelfHostSyncStage.FINISHING -> SyncProgressHint(stringResource(Res.string.selfhost_finishing_up))
            SelfHostSyncStage.TRANSFERRING -> {
                SyncDirectionProgress(
                    labelWithPercent = Res.string.selfhost_sending_percent,
                    emptyMessage = stringResource(Res.string.selfhost_nothing_to_send),
                    done = progress.sent,
                    total = progress.toSend,
                    describeAmount = describeAmount
                )
                SyncDirectionProgress(
                    labelWithPercent = Res.string.selfhost_receiving_percent,
                    emptyMessage = stringResource(Res.string.selfhost_nothing_to_receive),
                    done = progress.received,
                    total = progress.toReceive,
                    describeAmount = describeAmount
                )
            }
        }
    }
}

@Composable
private fun SyncDirectionProgress(
    labelWithPercent: StringResource,
    emptyMessage: String,
    done: Long,
    total: Long,
    describeAmount: @Composable (done: Long, total: Long) -> String
) {
    if (total == 0L) {
        SyncProgressHint(emptyMessage)
        return
    }
    val doneFraction = (done.toFloat() / total).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(labelWithPercent, (doneFraction * 100).roundToInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = describeAmount(done, total),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        LinearProgressIndicator(
            progress = { doneFraction },
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SyncProgressHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    )
}

@Composable
private fun describeNoteCount(done: Long, total: Long): String =
    pluralStringResource(Res.plurals.selfhost_note_count_progress, total.toInt(), minOf(done, total), total)

@Composable
internal fun describeImageBytes(done: Long, total: Long): String =
    stringResource(Res.string.selfhost_megabytes_progress, formatMegabytes(minOf(done, total)), formatMegabytes(total))

private fun formatMegabytes(bytes: Long): String {
    val tenthsOfMegabyte = bytes * 10 / (1024 * 1024)
    return decimalText(tenthsOfMegabyte / 10.0, minimumFractionDigits = 1, maximumFractionDigits = 1)
}

@Composable
private fun ServerDetailsCard(form: SelfHostSetupFormState, viewModel: SelfHostSetupViewModel) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    val passwordVisibilityLabel = if (isPasswordVisible) {
        stringResource(Res.string.selfhost_hide_password)
    } else {
        stringResource(Res.string.selfhost_show_password)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val onSubmitServerDetails: () -> Unit = { if (form.canTestConnection) viewModel.testConnection() }

        SettingsHint(
            text = stringResource(Res.string.selfhost_server_details_hint)
        )

        EmberrTextField(
            value = form.serverUrl,
            onValueChange = viewModel::onServerUrlChanged,
            placeholder = "https://cloud.example.com/remote.php/dav",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
            onSubmit = onSubmitServerDetails
        )

        EmberrTextField(
            value = form.username,
            onValueChange = viewModel::onUsernameChanged,
            placeholder = stringResource(Res.string.selfhost_username),
            modifier = Modifier.fillMaxWidth(),
            onSubmit = onSubmitServerDetails
        )

        EmberrTextField(
            value = form.password,
            onValueChange = viewModel::onPasswordChanged,
            placeholder = stringResource(Res.string.selfhost_password),
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
            onSubmit = onSubmitServerDetails
        )

        Spacer(modifier = Modifier.height(4.dp))

        EmberrButtonPrimary(
            text = if (form.connectionTestStatus == ConnectionTestStatus.TESTING) {
                stringResource(Res.string.selfhost_testing)
            } else {
                stringResource(Res.string.selfhost_test_connection)
            },
            onClick = viewModel::testConnection,
            enabled = form.canTestConnection,
            showsWavingDots = form.connectionTestStatus == ConnectionTestStatus.TESTING,
            modifier = Modifier.fillMaxWidth()
        )

        if (form.connectionTestStatus == ConnectionTestStatus.TESTING) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.selfhost_contacting_server),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        form.connectionTestMessage?.let { message ->
            val isSuccess = form.connectionTestStatus == ConnectionTestStatus.VERIFIED
            val tint = if (isSuccess) SuccessColor else MaterialTheme.colorScheme.error

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint
                )
            }
        }
    }
}

@Composable
private fun RestorePassphraseCard(
    passphraseInput: String,
    onPassphraseChanged: (String) -> Unit,
    onSubmit: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsHint(
            text = stringResource(Res.string.selfhost_restore_hint)
        )
        SettingsHint(
            text = stringResource(Res.string.selfhost_restore_find_hint)
        )

        PassphraseField(value = passphraseInput, onValueChange = onPassphraseChanged, onSubmit = onSubmit)
    }
}

@Composable
private fun PassphraseField(value: String, onValueChange: (String) -> Unit, onSubmit: (() -> Unit)?) {
    var isPassphraseVisible by remember { mutableStateOf(false) }
    val passphraseVisibilityLabel = if (isPassphraseVisible) {
        stringResource(Res.string.selfhost_hide_passphrase)
    } else {
        stringResource(Res.string.selfhost_show_passphrase)
    }

    EmberrTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = stringResource(Res.string.selfhost_passphrase_placeholder),
        visualTransformation = if (isPassphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconTooltip(text = passphraseVisibilityLabel) {
                EmberrTextFieldIconButton(onClick = { isPassphraseVisible = !isPassphraseVisible }) {
                    Icon(
                        painter = painterResource(if (isPassphraseVisible) Res.drawable.eye_closed else Res.drawable.eye_open),
                        contentDescription = passphraseVisibilityLabel,
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
private fun NewPassphraseCard(
    passphrase: String,
    hasSavedPassphrase: Boolean,
    onHasSavedPassphraseChanged: (Boolean) -> Unit,
    onRegenerate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(Res.string.selfhost_save_passphrase_now),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        PassphraseFact(stringResource(Res.string.selfhost_fact_type_on_other_devices))
        PassphraseFact(stringResource(Res.string.selfhost_fact_locks_notes))
        PassphraseFact(stringResource(Res.string.selfhost_fact_cannot_reset))
        PassphraseFact(stringResource(Res.string.selfhost_fact_see_again_later))

        PassphraseBox(
            passphrase = passphrase,
            extraAction = {
                IconTooltip(text = stringResource(Res.string.selfhost_new_passphrase)) {
                    IconButton(onClick = onRegenerate) {
                        Icon(
                            painter = painterResource(Res.drawable.refresh_cw),
                            contentDescription = stringResource(Res.string.selfhost_generate_new_passphrase),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            EmberrCheckbox(
                checked = hasSavedPassphrase,
                onCheckedChange = onHasSavedPassphraseChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.selfhost_saved_passphrase_confirmation),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PassphraseFact(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "•",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PassphraseBox(passphrase: String, extraAction: @Composable () -> Unit = {}) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = passphrase,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconTooltip(text = stringResource(Res.string.selfhost_copy_passphrase)) {
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(passphrase)) }) {
                    Icon(
                        painter = painterResource(Res.drawable.copy),
                        contentDescription = stringResource(Res.string.selfhost_copy_passphrase),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            extraAction()
        }
    }
}

@Composable
private fun ServerAddressBox(serverUrl: String) {
    val clipboardManager = LocalClipboardManager.current
    val copiedAddressMessage = stringResource(Res.string.selfhost_copied_address)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionContainer(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                text = serverUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconTooltip(text = stringResource(Res.string.selfhost_copy_address)) {
            IconButton(onClick = {
                clipboardManager.setText(AnnotatedString(serverUrl))
                showNativeToast(copiedAddressMessage)
            }) {
                Icon(
                    painter = painterResource(Res.drawable.copy),
                    contentDescription = stringResource(Res.string.selfhost_copy_address),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
}

@Composable
private fun ErrorMessageCard(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.07f))
            .padding(14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

private val SuccessColor
    @Composable get() = Color(0xFF4CAF50)
