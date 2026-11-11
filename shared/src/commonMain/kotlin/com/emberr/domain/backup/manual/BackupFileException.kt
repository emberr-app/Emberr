package com.emberr.domain.backup.manual

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

class BackupFileException(val reason: StringResource) : IllegalStateException(reason.key)

suspend fun backupProblemText(error: Throwable): String? =
    if (error is BackupFileException) getString(error.reason) else error.message
