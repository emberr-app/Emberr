package com.emberr.presentation.settings.cloud

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
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
import com.emberr.domain.cloud.account.CloudPasswordStrength
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.settings.SettingsGroup
import com.emberr.presentation.settings.desktopSettingsSidePadding
import com.emberr.presentation.shared.components.EmberrAlertDialog
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrCheckbox
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.KmpBackHandler
import com.emberr.presentation.shared.components.topHeaderBarPadding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.compose.viewmodel.koinViewModel

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
                CloudAccountStep.LOG_IN_CODE -> LogInCodeStep(state = state, viewModel = viewModel)
                CloudAccountStep.RECOVERY_EMAIL -> RecoveryEmailStep(state = state, viewModel = viewModel)
                CloudAccountStep.RECOVERY_WORDS -> RecoveryWordsStep(state = state, viewModel = viewModel)
                CloudAccountStep.RECOVERY_NEW_PASSWORD -> RecoveryNewPasswordStep(state = state, viewModel = viewModel)
                CloudAccountStep.SIGNED_IN -> SignedInStep(state = state, viewModel = viewModel)
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
        state.errorMessage?.let { message -> ErrorText(message = message) }
        EmberrButtonPrimary(
            text = "Create account",
            onClick = viewModel::startSignUp,
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth()
        )
        EmberrButtonSecondary(
            text = "Log in",
            onClick = viewModel::startLogIn,
            enabled = !state.isWorking,
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
    val clipboardManager = LocalClipboardManager.current

    StepGroup(title = "Recovery words") {
        HintText(
            text = "If you forget your password, these 24 words are the only way back into your account. " +
                "We don't store them, so save them somewhere safe."
        )
        WarningText(text = "Without your password and these words, your cloud notes can't be recovered.")
        NumberedRecoveryWords(words = state.newRecoveryWords)
        EmberrButtonSecondary(
            text = "Copy words",
            onClick = {
                clipboardManager.setText(AnnotatedString(state.newRecoveryWords.joinToString(" ")))
                showNativeToast("Recovery words copied")
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            EmberrCheckbox(
                checked = state.hasSavedRecoveryWords,
                onCheckedChange = viewModel::onSavedRecoveryWordsChanged
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "I have saved these words somewhere safe",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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
private fun LogInCodeStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Log in") {
        CodeSentText(email = state.email)
        CodeField(code = state.code, onCodeChanged = viewModel::onCodeChanged, onSubmit = viewModel::verifyLoginCode)
        StepActions(
            state = state,
            primaryText = "Verify",
            isPrimaryEnabled = state.canVerifyLoginCode,
            onPrimaryClick = viewModel::verifyLoginCode,
            onSendNewCode = viewModel::logIn,
            onBack = viewModel::goBack
        )
    }
}

@Composable
private fun RecoveryEmailStep(state: CloudAccountScreenState, viewModel: CloudAccountViewModel) {
    StepGroup(title = "Forgot password") {
        HintText(text = "Enter your account's email. We'll send you a code.")
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
    var isConfirmingSignOut by remember { mutableStateOf(false) }

    StepGroup(title = "Account") {
        Text(
            text = "Signed in as ${state.signedInEmail}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        state.errorMessage?.let { message -> ErrorText(message = message) }
        EmberrButtonSecondary(
            text = "Sign out of this device",
            onClick = { isConfirmingSignOut = true },
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth()
        )
        if (state.isWorking) WorkingIndicator()
    }

    if (isConfirmingSignOut) {
        EmberrAlertDialog(
            onDismissRequest = { isConfirmingSignOut = false },
            title = "Sign Out of This Device?"
        ) {
            Text(
                text = "This device stops using Emberr Cloud. Your notes on this device are not touched.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EmberrButtonSecondary(
                    text = "Cancel",
                    onClick = { isConfirmingSignOut = false },
                    modifier = Modifier.weight(1f)
                )
                EmberrButtonPrimary(
                    text = "Sign out",
                    onClick = {
                        isConfirmingSignOut = false
                        viewModel.signOutThisDevice()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
private fun NumberedRecoveryWords(words: List<String>) {
    val wordsPerRow = if (isDesktopPlatform) 3 else 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        words.chunked(wordsPerRow).forEachIndexed { rowIndex, rowWords ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowWords.forEachIndexed { columnIndex, word ->
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${rowIndex * wordsPerRow + columnIndex + 1}.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text = word,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                repeat(wordsPerRow - rowWords.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
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
