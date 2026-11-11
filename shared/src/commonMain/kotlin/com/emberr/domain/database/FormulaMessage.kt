package com.emberr.domain.database

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

sealed interface FormulaMessage {
    val arguments: List<Any>

    data class Phrase(val resource: StringResource, override val arguments: List<Any> = emptyList()) : FormulaMessage

    data class Counted(
        val resource: PluralStringResource,
        val count: Int,
        override val arguments: List<Any> = emptyList()
    ) : FormulaMessage
}
