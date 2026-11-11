package com.emberr.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.emberr.domain.update.AppUpdateController
import com.emberr.domain.update.AppUpdateState
import com.emberr.domain.util.system.restartApplication
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.common_percent_value
import emberr.shared.generated.resources.common_version_label
import emberr.shared.generated.resources.settings_group_updates
import emberr.shared.generated.resources.settings_update_check_now
import emberr.shared.generated.resources.settings_update_check_on_launch
import emberr.shared.generated.resources.settings_update_checking
import emberr.shared.generated.resources.settings_update_download_version
import emberr.shared.generated.resources.settings_update_downloading
import emberr.shared.generated.resources.settings_update_failed
import emberr.shared.generated.resources.settings_update_new
import emberr.shared.generated.resources.settings_update_restart_to_update
import emberr.shared.generated.resources.settings_update_up_to_date
import emberr.shared.generated.resources.download
import emberr.shared.generated.resources.refresh_cw
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
actual fun AppUpdateSettingsSection() {
    val updateController = koinInject<AppUpdateController>()
    if (!updateController.canUpdateInApp) return

    val updateState by updateController.state.collectAsState()
    val isAutomaticCheckEnabled by updateController.automaticCheckEnabledFlow
        .collectAsState(initial = updateController.isAutomaticCheckEnabled())

    SettingsGroup(title = stringResource(Res.string.settings_group_updates)) {
        SettingsActionRow(
            icon = painterResource(Res.drawable.download),
            title = actionTitleFor(updateState),
            trailingLabel = statusLabelFor(updateState, updateController.installedVersion),
            onClick = {
                when (updateState) {
                    is AppUpdateState.Available -> updateController.startDownload()
                    is AppUpdateState.ReadyToRestart -> if (updateController.startInstallerBeforeRestart()) restartApplication()
                    is AppUpdateState.Checking, is AppUpdateState.Downloading -> Unit
                    else -> updateController.checkNow()
                }
            }
        )
        SettingsDivider()
        SettingsToggleRow(
            icon = painterResource(Res.drawable.refresh_cw),
            title = stringResource(Res.string.settings_update_check_on_launch),
            isChecked = isAutomaticCheckEnabled,
            onCheckedChange = updateController::setAutomaticCheckEnabled
        )
    }
}

@Composable
private fun actionTitleFor(updateState: AppUpdateState): String = when (updateState) {
    is AppUpdateState.Checking -> stringResource(Res.string.settings_update_checking)
    is AppUpdateState.Available -> stringResource(Res.string.settings_update_download_version, updateState.release.version)
    is AppUpdateState.Downloading -> stringResource(Res.string.settings_update_downloading)
    is AppUpdateState.ReadyToRestart -> stringResource(Res.string.settings_update_restart_to_update)
    else -> stringResource(Res.string.settings_update_check_now)
}

@Composable
private fun statusLabelFor(updateState: AppUpdateState, installedVersion: String?): String? = when (updateState) {
    is AppUpdateState.UpToDate -> stringResource(Res.string.settings_update_up_to_date)
    is AppUpdateState.Available -> stringResource(Res.string.settings_update_new)
    is AppUpdateState.Downloading -> stringResource(Res.string.common_percent_value, updateState.progressPercent)
    is AppUpdateState.ReadyToRestart -> stringResource(Res.string.common_version_label, updateState.version)
    is AppUpdateState.Failed -> stringResource(Res.string.settings_update_failed)
    else -> installedVersion?.let { stringResource(Res.string.common_version_label, it) }
}
