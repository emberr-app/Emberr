package com.emberr.domain.selfhost.sync

enum class SelfHostSyncStage { PREPARING, TRANSFERRING, FINISHING }

data class SelfHostSyncProgress(
    val stage: SelfHostSyncStage = SelfHostSyncStage.PREPARING,
    val sent: Long = 0,
    val toSend: Long = 0,
    val received: Long = 0,
    val toReceive: Long = 0
)
