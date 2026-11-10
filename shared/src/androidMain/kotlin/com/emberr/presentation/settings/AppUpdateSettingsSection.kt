package com.emberr.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.emberr.domain.update.AndroidUpdateCheckState
import com.emberr.domain.update.AndroidUpdateChecker
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.common_version_label
import emberr.shared.generated.resources.settings_group_updates
import emberr.shared.generated.resources.settings_update_check_now
import emberr.shared.generated.resources.settings_update_check_on_launch
import emberr.shared.generated.resources.settings_update_checking
import emberr.shared.generated.resources.settings_update_download_version
import emberr.shared.generated.resources.settings_update_failed
import emberr.shared.generated.resources.settings_update_new
import emberr.shared.generated.resources.settings_update_up_to_date
import emberr.shared.generated.resources.download
import emberr.shared.generated.resources.refresh_cw
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
actual fun AppUpdateSettingsSection() {
    val updateChecker = koinInject<AndroidUpdateChecker>()
    if (updateChecker.installedVersion == null || updateChecker.isInstalledFromPlayStore) return

    val checkState by updateChecker.state.collectAsState()
    val isAutomaticCheckEnabled by updateChecker.automaticCheckEnabledFlow
        .collectAsState(initial = updateChecker.isAutomaticCheckEnabled())

    SettingsGroup(title = stringResource(Res.string.settings_group_updates)) {
        SettingsActionRow(
            icon = painterResource(Res.drawable.download),
            title = actionTitleFor(checkState),
            trailingLabel = statusLabelFor(checkState, updateChecker.installedVersion),
            onClick = {
                when (checkState) {
                    is AndroidUpdateCheckState.Available -> updateChecker.openReleasePage()
                    is AndroidUpdateCheckState.Checking -> Unit
                    else -> updateChecker.checkNow()
                }
            }
        )
        SettingsDivider()
        SettingsToggleRow(
            icon = painterResource(Res.drawable.refresh_cw),
            title = stringResource(Res.string.settings_update_check_on_launch),
            isChecked = isAutomaticCheckEnabled,
            onCheckedChange = updateChecker::setAutomaticCheckEnabled
        )
    }
}

@Composable
private fun actionTitleFor(checkState: AndroidUpdateCheckState): String = when (checkState) {
    is AndroidUpdateCheckState.Checking -> stringResource(Res.string.settings_update_checking)
    is AndroidUpdateCheckState.Available -> stringResource(Res.string.settings_update_download_version, checkState.version)
    else -> stringResource(Res.string.settings_update_check_now)
}

@Composable
private fun statusLabelFor(checkState: AndroidUpdateCheckState, installedVersion: String?): String? = when (checkState) {
    is AndroidUpdateCheckState.UpToDate -> stringResource(Res.string.settings_update_up_to_date)
    is AndroidUpdateCheckState.Available -> stringResource(Res.string.settings_update_new)
    is AndroidUpdateCheckState.Failed -> stringResource(Res.string.settings_update_failed)
    else -> installedVersion?.let { stringResource(Res.string.common_version_label, it) }
}
