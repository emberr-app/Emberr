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
