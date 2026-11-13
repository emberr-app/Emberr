package com.emberr.domain.selfhost.webdav

import com.emberr.domain.util.network.LocalNetworkHostValidator
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.takeFrom

object WebDavServerUrlValidator {

    fun isLocalNetworkHost(host: String): Boolean = LocalNetworkHostValidator.isLocalNetworkHost(host)

    fun problemWith(serverUrl: String): WebDavAddressProblem? {
        val url = try {
            Url(serverUrl)
        } catch (_: Exception) {
            return WebDavAddressProblem.NOT_A_URL
        }

        val isHttps = url.protocol.name.equals("https", ignoreCase = true)
        val isPlainHttpOnLocalNetwork = url.protocol.name.equals("http", ignoreCase = true) &&
            isLocalNetworkHost(url.host)

        return if (!isHttps && !isPlainHttpOnLocalNetwork) WebDavAddressProblem.PLAIN_HTTP_OUTSIDE_LOCAL_NETWORK else null
    }

    fun suggestedServerUrlFromRedirect(requestUrl: String, redirectLocation: String, typedServerUrl: String): String? {
        val redirectedFolderUrl = try {
            URLBuilder(requestUrl).takeFrom(redirectLocation).buildString()
        } catch (_: Exception) {
            return null
        }
        val syncFolderEnding = "${WebDavSyncPaths.ROOT}/"
        val redirectedFolderWithSlash = if (redirectedFolderUrl.endsWith("/")) redirectedFolderUrl else "$redirectedFolderUrl/"
        if (!redirectedFolderWithSlash.endsWith(syncFolderEnding)) return null
        val suggestedServerUrl = redirectedFolderWithSlash.removeSuffix(syncFolderEnding)
        return suggestedServerUrl.takeIf { it != typedServerUrl.trimEnd('/') }
    }

    fun validate(serverUrl: String) {
        when (problemWith(serverUrl)) {
            WebDavAddressProblem.NOT_A_URL ->
                throw WebDavConfigurationException("Server URL '$serverUrl' is not a valid URL")
            WebDavAddressProblem.PLAIN_HTTP_OUTSIDE_LOCAL_NETWORK -> throw WebDavConfigurationException(
                "Server URL '$serverUrl' must use https:// — plain http:// is only permitted for " +
                        "local network addresses such as localhost, 127.0.0.1, or 192.168.x.x"
            )
            null -> Unit
        }
    }
}

enum class WebDavAddressProblem {
    NOT_A_URL,
    PLAIN_HTTP_OUTSIDE_LOCAL_NETWORK
}