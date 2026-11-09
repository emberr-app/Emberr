package com.emberr.domain.cloud.sync

fun interface CloudMediaPassLauncher {
    fun tryToLaunchInItsOwnJob(): Boolean
}
