package com.emberr.core.security.secrets

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

internal fun secretStorePhrase(resource: StringResource, vararg arguments: Any): String =
    runBlocking { getString(resource, *arguments) }
