package com.emberr.presentation.sync

import androidx.compose.runtime.Composable
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.lan_sync_status_already_in_progress
import emberr.shared.generated.resources.lan_sync_status_auto_syncing
import emberr.shared.generated.resources.lan_sync_status_desktop_offline
import emberr.shared.generated.resources.lan_sync_status_failed
import emberr.shared.generated.resources.lan_sync_status_fetching_from_desktop
import emberr.shared.generated.resources.lan_sync_status_idle
import emberr.shared.generated.resources.lan_sync_status_not_paired
import emberr.shared.generated.resources.lan_sync_status_partial
import emberr.shared.generated.resources.lan_sync_status_success
import emberr.shared.generated.resources.lan_sync_status_sync_error
import emberr.shared.generated.resources.lan_sync_status_synced_successfully
import emberr.shared.generated.resources.lan_sync_status_synced_with_unsaved_items
import emberr.shared.generated.resources.lan_sync_status_syncing
import emberr.shared.generated.resources.lan_sync_status_update_both_devices
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

sealed interface LanSyncStatus {
    data object Idle : LanSyncStatus
    data object NotPaired : LanSyncStatus
    data object AlreadyInProgress : LanSyncStatus
    data object Syncing : LanSyncStatus
    data object AutoSyncing : LanSyncStatus
    data object FetchingFromDesktop : LanSyncStatus
    data object Success : LanSyncStatus
    data object SyncedSuccessfully : LanSyncStatus
    data class SyncedWithUnsavedItems(val unsavedItemCount: Int) : LanSyncStatus
    data object PartialSync : LanSyncStatus
    data object UpdateBothDevices : LanSyncStatus
    data object DesktopOffline : LanSyncStatus
    data class Failed(val errorMessage: String?) : LanSyncStatus
    data class SyncError(val errorName: String) : LanSyncStatus

    val shouldShowAsMessage: Boolean
        get() = this != Idle && this != Syncing
}

@Composable
fun LanSyncStatus.text(): String = when (this) {
    LanSyncStatus.Idle -> stringResource(Res.string.lan_sync_status_idle)
    LanSyncStatus.NotPaired -> stringResource(Res.string.lan_sync_status_not_paired)
    LanSyncStatus.AlreadyInProgress -> stringResource(Res.string.lan_sync_status_already_in_progress)
    LanSyncStatus.Syncing -> stringResource(Res.string.lan_sync_status_syncing)
    LanSyncStatus.AutoSyncing -> stringResource(Res.string.lan_sync_status_auto_syncing)
    LanSyncStatus.FetchingFromDesktop -> stringResource(Res.string.lan_sync_status_fetching_from_desktop)
    LanSyncStatus.Success -> stringResource(Res.string.lan_sync_status_success)
    LanSyncStatus.SyncedSuccessfully -> stringResource(Res.string.lan_sync_status_synced_successfully)
    is LanSyncStatus.SyncedWithUnsavedItems -> pluralStringResource(
        Res.plurals.lan_sync_status_synced_with_unsaved_items,
        unsavedItemCount,
        unsavedItemCount
    )
    LanSyncStatus.PartialSync -> stringResource(Res.string.lan_sync_status_partial)
    LanSyncStatus.UpdateBothDevices -> stringResource(Res.string.lan_sync_status_update_both_devices)
    LanSyncStatus.DesktopOffline -> stringResource(Res.string.lan_sync_status_desktop_offline)
    is LanSyncStatus.Failed -> stringResource(Res.string.lan_sync_status_failed, errorMessage.toString())
    is LanSyncStatus.SyncError -> stringResource(Res.string.lan_sync_status_sync_error, errorName)
}
