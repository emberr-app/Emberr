package com.emberr.domain.selfhost.webdav

import com.emberr.domain.util.network.LocalNetworkHostValidator
import io.ktor.http.Url
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_webdav_needs_https
import emberr.shared.generated.resources.message_webdav_not_a_url
import org.jetbrains.compose.resources.StringResource

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

enum class WebDavAddressProblem(val phrase: StringResource) {
    NOT_A_URL(Res.string.message_webdav_not_a_url),
    PLAIN_HTTP_OUTSIDE_LOCAL_NETWORK(Res.string.message_webdav_needs_https)
}