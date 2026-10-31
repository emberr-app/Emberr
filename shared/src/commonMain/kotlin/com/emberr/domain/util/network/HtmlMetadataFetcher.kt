package com.emberr.domain.util.network

data class UrlMetadata(
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val iconUrl: String? = null
)

expect object HtmlMetadataFetcher {
    suspend fun fetchMetadata(url: String): UrlMetadata
}