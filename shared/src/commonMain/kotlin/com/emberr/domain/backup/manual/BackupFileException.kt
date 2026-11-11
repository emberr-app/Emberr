package com.emberr.domain.backup.manual

import org.jetbrains.compose.resources.StringResource

class BackupFileException(val reason: StringResource) : IllegalStateException(reason.key)

sealed interface BackupProblem {
    data class Known(val reason: StringResource) : BackupProblem
    data class Unexpected(val detail: String?) : BackupProblem
}

fun backupProblemOf(error: Throwable): BackupProblem =
    if (error is BackupFileException) BackupProblem.Known(error.reason) else BackupProblem.Unexpected(error.message)
