package com.emberr.domain.sync

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.selfhost.sync.MeteredNetworkChecker
import com.emberr.domain.selfhost.sync.SelfHostSyncNetwork

class SyncNetworkRule(
    private val settingsManager: SettingsManager,
    private val meteredNetworkChecker: MeteredNetworkChecker
) {

    fun chosenNetwork(): SelfHostSyncNetwork =
        SelfHostSyncNetwork.fromStoredName(settingsManager.getSelfHostSyncNetwork())

    fun isOnAllowedNetwork(): Boolean =
        chosenNetwork().allowsSync(meteredNetworkChecker.isOnMeteredNetwork())
}

fun waitingForNetworkMessage(syncNetwork: SelfHostSyncNetwork): String = when (syncNetwork) {
    SelfHostSyncNetwork.WIFI_ONLY -> "Sync is set to Wi-Fi only. It will run when you connect to Wi-Fi."
    SelfHostSyncNetwork.MOBILE_DATA_ONLY -> "Sync is set to mobile data only. It will run when you are off Wi-Fi."
    SelfHostSyncNetwork.WIFI_AND_MOBILE_DATA -> "Sync will run when you are back online."
}
