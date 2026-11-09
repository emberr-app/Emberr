package com.emberr.domain.sync

import androidx.work.Constraints
import androidx.work.NetworkType
import com.emberr.domain.selfhost.sync.SelfHostSyncNetwork

fun syncNetworkConstraints(syncNetwork: SelfHostSyncNetwork): Constraints {
    val networkType = when (syncNetwork) {
        SelfHostSyncNetwork.WIFI_AND_MOBILE_DATA -> NetworkType.CONNECTED
        SelfHostSyncNetwork.WIFI_ONLY -> NetworkType.UNMETERED
        SelfHostSyncNetwork.MOBILE_DATA_ONLY -> NetworkType.METERED
    }
    return Constraints.Builder().setRequiredNetworkType(networkType).build()
}
