package com.emberr.domain.selfhost.webdav

sealed interface JsonDownloadIfChanged {
    data object Unchanged : JsonDownloadIfChanged
    data class Changed(val jsonWithEtag: Pair<String, String?>?) : JsonDownloadIfChanged
}
