package com.emberr.domain.sync

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.selfhost.sync.MeteredNetworkChecker
import com.emberr.domain.selfhost.sync.SelfHostSyncNetwork
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_sync_waiting_for_mobile_data
import emberr.shared.generated.resources.message_sync_waiting_for_network
import emberr.shared.generated.resources.message_sync_waiting_for_wifi
import org.jetbrains.compose.resources.StringResource

class SyncNetworkRule(
    private val settingsManager: SettingsManager,
    private val meteredNetworkChecker: MeteredNetworkChecker
) {

    fun chosenNetwork(): SelfHostSyncNetwork =
        SelfHostSyncNetwork.fromStoredName(settingsManager.getSelfHostSyncNetwork())

    fun isOnAllowedNetwork(): Boolean =
        chosenNetwork().allowsSync(meteredNetworkChecker.isOnMeteredNetwork())
}

fun waitingForNetworkPhrase(syncNetwork: SelfHostSyncNetwork): StringResource = when (syncNetwork) {
    SelfHostSyncNetwork.WIFI_ONLY -> Res.string.message_sync_waiting_for_wifi
    SelfHostSyncNetwork.MOBILE_DATA_ONLY -> Res.string.message_sync_waiting_for_mobile_data
    SelfHostSyncNetwork.WIFI_AND_MOBILE_DATA -> Res.string.message_sync_waiting_for_network
}
