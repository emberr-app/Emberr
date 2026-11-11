package com.emberr.presentation.sync

import com.emberr.domain.selfhost.sync.SelfHostSyncNetwork
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_sync_waiting_for_mobile_data
import emberr.shared.generated.resources.message_sync_waiting_for_network
import emberr.shared.generated.resources.message_sync_waiting_for_wifi
import org.jetbrains.compose.resources.StringResource

fun waitingForNetworkPhrase(syncNetwork: SelfHostSyncNetwork): StringResource = when (syncNetwork) {
    SelfHostSyncNetwork.WIFI_ONLY -> Res.string.message_sync_waiting_for_wifi
    SelfHostSyncNetwork.MOBILE_DATA_ONLY -> Res.string.message_sync_waiting_for_mobile_data
    SelfHostSyncNetwork.WIFI_AND_MOBILE_DATA -> Res.string.message_sync_waiting_for_network
}
