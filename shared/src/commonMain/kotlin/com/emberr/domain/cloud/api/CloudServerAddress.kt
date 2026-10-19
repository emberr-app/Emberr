package com.emberr.domain.cloud.api

import io.ktor.http.parseUrl

fun normalizedCloudServerAddress(enteredAddress: String): String? {
    val address = enteredAddress.trim().trimEnd('/')
    val startsWithWebScheme = address.startsWith("http://", ignoreCase = true) ||
        address.startsWith("https://", ignoreCase = true)
    if (!startsWithWebScheme || parseUrl(address) == null) return null
    return address
}
