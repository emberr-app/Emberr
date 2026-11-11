package com.emberr.domain.sync

sealed interface SyncServerStatus {
    data object Idle : SyncServerStatus
    data class Running(val port: Int) : SyncServerStatus
    data class Unavailable(val port: Int, val isPortAlreadyTaken: Boolean, val errorDetail: String?) : SyncServerStatus
}
