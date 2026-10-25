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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.domain.selfhost.sync.SelfHostSyncLog
import com.emberr.domain.selfhost.sync.SelfHostSyncNetwork
import com.emberr.domain.selfhost.sync.SelfHostSyncProgress
import com.emberr.domain.selfhost.sync.SelfHostSyncStage
import com.emberr.domain.util.system.DeviceLockConfirmation
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.rememberDeviceLockPrompt
import com.emberr.presentation.settings.SettingsGroup
import com.emberr.presentation.settings.SettingsSegmentedOptions
import com.emberr.presentation.settings.desktopSettingsSidePadding
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrCheckbox
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.topHeaderBarPadding
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.refresh_cw
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.jetbrains.compose.resources.painterResource
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
                    title = "Self-Host",
                    hazeState = internalHazeState,
                    contentPadding = topHeaderBarPadding(bottom = 16.dp),
                    backTooltip = "Back",
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
            SettingsGroup(title = "Server Details") {
                ServerDetailsCard(form = form, viewModel = viewModel)
            }
        }

        if (form.vaultMode == VaultMode.CREATE_VAULT) {
            item {
                SettingsGroup(title = "Recovery Passphrase") {
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
                SettingsGroup(title = "Unlock Vault") {
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
                            form.setupPhase == SetupPhase.FINALIZING -> "Setting up"
                            form.vaultMode == VaultMode.RESTORE_VAULT -> "Unlock and Sync"
                            else -> "Create Vault and Sync"
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
            title = "Server Address Rejected"
        ) {
            Text(
                text = form.configurationWarningMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(20.dp))
            EmberrButtonPrimary(
                text = "Got it",
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
    val deviceLockPrompt = rememberDeviceLockPrompt()
    val coroutineScope = rememberCoroutineScope()

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
            SettingsGroup(title = "Status") {
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
                            text = "Status: Connected",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = state.serverUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val isSyncRunning = state.syncStatus != ManualSyncStatus.IDLE

                    if (isSyncRunning) {
                        EmberrButtonSecondary(
                            text = if (state.syncStatus == ManualSyncStatus.CANCELLING) "Cancelling..." else "Cancel Sync",
                            onClick = {
                                SelfHostSyncLog.d("UI: Cancel Sync button tapped")
                                viewModel.cancelSync()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Cancelling pauses sync until you restart the app or tap Sync Now.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    } else {
                        EmberrButtonPrimary(
                            text = "Sync Now",
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
                                "Syncing..."
                            } else {
                                formatLastSynced(state.lastSyncedAtMillis, LocalUsesTwentyFourHourClock.current)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    state.noteSyncProgress?.let { progress ->
                        Spacer(modifier = Modifier.height(14.dp))
                        SyncProgressDetails(title = "Notes", progress = progress, describeAmount = ::describeNoteCount)
                    }

                    state.imageSyncProgress?.let { progress ->
                        Spacer(modifier = Modifier.height(14.dp))
                        SyncProgressDetails(title = "Images", progress = progress, describeAmount = ::describeImageBytes)
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
            SettingsGroup(title = "Recovery Passphrase") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.hasSavedRecoveryPassphrase) {
                        SettingsHint(text = "You need this passphrase to connect another phone or computer to this server.")
                        EmberrButtonSecondary(
                            text = "Show Passphrase",
                            onClick = {
                                coroutineScope.launch {
                                    val confirmation = deviceLockPrompt.confirm(
                                        title = "Recovery passphrase",
                                        description = "Unlock to see your recovery passphrase"
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
                            text = "This device was connected before Emberr saved the passphrase. " +
                                "Enter it once to save it here, so you can see it later."
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
                            text = if (state.isSavingPassphrase) "Checking" else "Save Passphrase",
                            onClick = viewModel::savePassphraseOnThisDevice,
                            enabled = state.canSavePassphrase,
                            showsWavingDots = state.isSavingPassphrase,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (!isDesktopPlatform) {
            item {
                SettingsGroup(title = "Sync network") {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        SettingsSegmentedOptions(
                            options = SyncNetworkLabels.map { it.second },
                            selectedOption = SyncNetworkLabels.first { it.first == state.syncNetwork }.second,
                            onOptionSelected = { selectedLabel ->
                                viewModel.onSyncNetworkSelected(SyncNetworkLabels.first { it.second == selectedLabel }.first)
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Notes and attachments only sync over the network you pick.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
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
                    text = "Disconnect Vault",
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
            title = "Recovery Passphrase"
        ) {
            Text(
                text = "Enter this on the other device when it asks to unlock the vault. Don't share it with anyone.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(16.dp))
            PassphraseBox(passphrase = passphrase)
            Spacer(Modifier.height(16.dp))
            EmberrButtonPrimary(
                text = "Done",
                onClick = viewModel::hideRecoveryPassphrase,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDisconnectConfirmation) {
        EmberrAlertDialog(
            onDismissRequest = { showDisconnectConfirmation = false },
            title = "Disconnect Vault?"
        ) {
            Text(
                text = "Your notes stay on this device and the server. You'll need your recovery passphrase to reconnect.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EmberrButtonSecondary(
                    text = "Cancel",
                    onClick = { showDisconnectConfirmation = false },
                    modifier = Modifier.weight(1f)
                )
                EmberrButtonPrimary(
                    text = "Disconnect",
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
private fun SyncProgressDetails(
    title: String,
    progress: SelfHostSyncProgress,
    describeAmount: (done: Long, total: Long) -> String
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        when (progress.stage) {
            SelfHostSyncStage.PREPARING -> SyncProgressHint("Checking for changes...")
            SelfHostSyncStage.FINISHING -> SyncProgressHint("Finishing up...")
            SelfHostSyncStage.TRANSFERRING -> {
                SyncDirectionProgress(
                    label = "Sending",
                    emptyMessage = "Nothing to send",
                    done = progress.sent,
                    total = progress.toSend,
                    describeAmount = describeAmount
                )
                SyncDirectionProgress(
                    label = "Receiving",
                    emptyMessage = "Nothing to receive",
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
    label: String,
    emptyMessage: String,
    done: Long,
    total: Long,
    describeAmount: (done: Long, total: Long) -> String
) {
    if (total == 0L) {
        SyncProgressHint(emptyMessage)
        return
    }
    val doneFraction = (done.toFloat() / total).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label ${(doneFraction * 100).roundToInt()}%",
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

private fun describeNoteCount(done: Long, total: Long): String =
    "${minOf(done, total)} of $total ${if (total == 1L) "note" else "notes"}"

private fun describeImageBytes(done: Long, total: Long): String =
    "${formatMegabytes(minOf(done, total))} of ${formatMegabytes(total)} MB"

private fun formatMegabytes(bytes: Long): String {
    val tenthsOfMegabyte = bytes * 10 / (1024 * 1024)
    return "${tenthsOfMegabyte / 10}.${tenthsOfMegabyte % 10}"
}

@Composable
private fun ServerDetailsCard(form: SelfHostSetupFormState, viewModel: SelfHostSetupViewModel) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val onSubmitServerDetails: () -> Unit = { if (form.canTestConnection) viewModel.testConnection() }

        SettingsHint(
            text = "Enter the WebDAV address and login of your server, for example Nextcloud or a NAS. " +
                "Emberr encrypts your notes before they leave this device."
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
            placeholder = "Username",
            modifier = Modifier.fillMaxWidth(),
            onSubmit = onSubmitServerDetails
        )

        EmberrTextField(
            value = form.password,
            onValueChange = viewModel::onPasswordChanged,
            placeholder = "Password",
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
            onSubmit = onSubmitServerDetails
        )

        Spacer(modifier = Modifier.height(4.dp))

        EmberrButtonPrimary(
            text = if (form.connectionTestStatus == ConnectionTestStatus.TESTING) "Testing" else "Test Connection",
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
                    text = "Contacting server...",
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
            text = "This server already has an Emberr vault. Enter the 16-character recovery passphrase " +
                "that was shown when the vault was created."
        )
        SettingsHint(
            text = "Can't find it? Open Settings → Self-Host → Show Passphrase on a device that is already connected."
        )

        PassphraseField(value = passphraseInput, onValueChange = onPassphraseChanged, onSubmit = onSubmit)
    }
}

@Composable
private fun PassphraseField(value: String, onValueChange: (String) -> Unit, onSubmit: (() -> Unit)?) {
    var isPassphraseVisible by remember { mutableStateOf(false) }

    EmberrTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = "16-character passphrase",
        visualTransformation = if (isPassphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconTooltip(text = if (isPassphraseVisible) "Hide passphrase" else "Show passphrase") {
                IconButton(onClick = { isPassphraseVisible = !isPassphraseVisible }) {
                    Icon(
                        imageVector = if (isPassphraseVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPassphraseVisible) "Hide passphrase" else "Show passphrase",
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
                text = "Save this passphrase now",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        PassphraseFact("You will type it on every other phone or computer you connect to this server.")
        PassphraseFact("It locks your notes on the server. No one can read them without it, not even Emberr.")
        PassphraseFact("Emberr can't reset it. Keep it in a password manager or write it down.")
        PassphraseFact("You can see it again later in Settings → Self-Host on any device that is connected.")

        PassphraseBox(
            passphrase = passphrase,
            extraAction = {
                IconTooltip(text = "New passphrase") {
                    IconButton(onClick = onRegenerate) {
                        Icon(
                            painter = painterResource(Res.drawable.refresh_cw),
                            contentDescription = "Generate a new passphrase",
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
                text = "I saved this passphrase somewhere safe",
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
            IconTooltip(text = "Copy passphrase") {
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(passphrase)) }) {
                    Icon(
                        painter = painterResource(Res.drawable.copy),
                        contentDescription = "Copy passphrase",
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

private val SyncNetworkLabels = listOf(
    SelfHostSyncNetwork.WIFI_AND_MOBILE_DATA to "Both",
    SelfHostSyncNetwork.WIFI_ONLY to "Wi-Fi only",
    SelfHostSyncNetwork.MOBILE_DATA_ONLY to "Data only"
)
