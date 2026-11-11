package com.emberr.presentation.settings.importer

import androidx.compose.runtime.Composable
import com.emberr.domain.backup.manual.BackupFileException
import com.emberr.domain.backup.manual.BackupProblem
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.import_emberr_unknown_error
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

suspend fun backupProblemText(error: Throwable): String? =
    if (error is BackupFileException) getString(error.reason) else error.message

@Composable
fun BackupProblem.text(): String = when (this) {
    is BackupProblem.Known -> stringResource(reason)
    is BackupProblem.Unexpected -> detail ?: stringResource(Res.string.import_emberr_unknown_error)
}
