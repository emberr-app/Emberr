package com.emberr.domain.update

import org.jetbrains.compose.resources.StringResource

sealed interface AppUpdateState {
    data object Idle : AppUpdateState
    data object Checking : AppUpdateState
    data object UpToDate : AppUpdateState
    data class Available(val release: AppUpdateManifest) : AppUpdateState
    data class Downloading(val release: AppUpdateManifest, val progressPercent: Int) : AppUpdateState
    data class ReadyToRestart(val version: String) : AppUpdateState
    data object Failed : AppUpdateState
}

sealed interface AppUpdatePrompt {
    data class OfferDownload(val version: String) : AppUpdatePrompt
    data class OfferRestart(val version: String) : AppUpdatePrompt
    data class ReportFailure(val reason: StringResource) : AppUpdatePrompt
}
