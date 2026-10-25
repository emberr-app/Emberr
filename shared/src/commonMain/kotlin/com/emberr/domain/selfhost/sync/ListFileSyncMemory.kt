package com.emberr.domain.selfhost.sync

import com.emberr.domain.selfhost.webdav.WebDavResourceInfo
import com.emberr.domain.selfhost.webdav.isWeakETag

internal const val LIST_FILE_NOT_ON_SERVER = "not-on-server"

internal fun listFileStatesOnServer(listedFiles: List<WebDavResourceInfo>, listFilePaths: List<String>): Map<String, String> =
    listFilePaths.mapNotNull { path ->
        val fileName = path.substringAfterLast('/')
        val listedFile = listedFiles.firstOrNull { !it.isCollection && it.href.substringAfterLast('/') == fileName }
        val serverState = when {
            listedFile == null -> LIST_FILE_NOT_ON_SERVER
            listedFile.etag == null || isWeakETag(listedFile.etag) -> null
            else -> listedFile.etag
        }
        serverState?.let { path to it }
    }.toMap()

internal class ListFileSyncMemory {

    private class StateKnownToBeInSync(val serverState: String, val localState: Any)

    private val statesKnownToBeInSyncByPath = mutableMapOf<String, StateKnownToBeInSync>()
    private var serverStatesThisPass: Map<String, String> = emptyMap()

    fun startPass(serverStates: Map<String, String>) {
        serverStatesThisPass = serverStates
    }

    fun isAlreadyInSync(path: String, localState: Any): Boolean {
        val knownState = statesKnownToBeInSyncByPath[path] ?: return false
        return knownState.serverState == serverStatesThisPass[path] && knownState.localState == localState
    }

    fun rememberIfNothingChanged(
        path: String,
        downloaded: Pair<String, String?>?,
        localState: Any,
        changedNothing: Boolean
    ) {
        if (!changedNothing) return
        val serverState = if (downloaded == null) LIST_FILE_NOT_ON_SERVER else downloaded.second ?: return
        if (isWeakETag(serverState)) return
        statesKnownToBeInSyncByPath[path] = StateKnownToBeInSync(serverState, localState)
    }

    fun forgetAll() {
        statesKnownToBeInSyncByPath.clear()
        serverStatesThisPass = emptyMap()
    }
}
