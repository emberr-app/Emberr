package com.emberr.presentation.settings

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.content.edit
import com.emberr.data.local.prefs.SyncConstants
import com.emberr.domain.update.AndroidUpdateChecker
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.logo_google_play
import emberr.shared.generated.resources.settings_play_testing
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.seconds

private val InviteDelayAfterLaunch = 2.seconds

@Composable
actual fun PlayTestingInvite(isDueThisLaunch: Boolean) {
    val updateChecker = koinInject<AndroidUpdateChecker>()
    val sharedPreferences = koinInject<SharedPreferences>()
    val uriHandler = LocalUriHandler.current
    var isDialogOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isDueThisLaunch) {
        if (!isDueThisLaunch || updateChecker.isInstalledFromPlayStore) return@LaunchedEffect
        if (sharedPreferences.getBoolean(SyncConstants.KEY_PLAY_TESTING_INVITE_SHOWN, false)) return@LaunchedEffect
        delay(InviteDelayAfterLaunch)
        sharedPreferences.edit { putBoolean(SyncConstants.KEY_PLAY_TESTING_INVITE_SHOWN, true) }
        isDialogOpen = true
    }

    if (isDialogOpen) {
        PlayTestingDialog(
            onJoinClick = {
                isDialogOpen = false
                uriHandler.openUri(PLAY_TESTING_GROUP_URL)
            },
            onDismissRequest = { isDialogOpen = false }
        )
    }
}

@Composable
actual fun PlayTestingSettingsRow() {
    val updateChecker = koinInject<AndroidUpdateChecker>()
    if (updateChecker.isInstalledFromPlayStore) return
    var isDialogOpen by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    SettingsDivider()
    SettingsActionRow(
        icon = painterResource(Res.drawable.logo_google_play),
        title = stringResource(Res.string.settings_play_testing),
        onClick = { isDialogOpen = true }
    )

    if (isDialogOpen) {
        PlayTestingDialog(
            onJoinClick = {
                isDialogOpen = false
                uriHandler.openUri(PLAY_TESTING_GROUP_URL)
            },
            onDismissRequest = { isDialogOpen = false }
        )
    }
}
