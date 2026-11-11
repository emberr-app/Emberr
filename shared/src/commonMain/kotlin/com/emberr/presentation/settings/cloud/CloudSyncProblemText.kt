package com.emberr.presentation.settings.cloud

import androidx.compose.runtime.Composable
import com.emberr.domain.cloud.sync.CloudMediaFailure
import com.emberr.domain.cloud.sync.CloudMediaProblem
import com.emberr.domain.cloud.sync.CloudMediaStep
import com.emberr.domain.cloud.sync.CloudSyncProblem
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_cloud_could_not_decrypt
import emberr.shared.generated.resources.message_cloud_media_could_not_ask_download
import emberr.shared.generated.resources.message_cloud_media_could_not_link
import emberr.shared.generated.resources.message_cloud_media_could_not_receive
import emberr.shared.generated.resources.message_cloud_media_could_not_send
import emberr.shared.generated.resources.message_cloud_media_could_not_unlink
import emberr.shared.generated.resources.message_cloud_media_no_account_stream
import emberr.shared.generated.resources.message_cloud_media_reason_cloud_answered
import emberr.shared.generated.resources.message_cloud_media_reason_key_missing
import emberr.shared.generated.resources.message_cloud_media_reason_network_not_picked
import emberr.shared.generated.resources.message_cloud_media_reason_not_decrypted
import emberr.shared.generated.resources.message_cloud_media_reason_storage_answered
import emberr.shared.generated.resources.message_cloud_media_reason_unreachable
import emberr.shared.generated.resources.message_cloud_media_stopped
import emberr.shared.generated.resources.message_cloud_note_unreadable
import emberr.shared.generated.resources.message_cloud_refused_sync
import emberr.shared.generated.resources.message_cloud_server_unreachable
import emberr.shared.generated.resources.message_cloud_signed_out_records_not_cleared
import emberr.shared.generated.resources.message_cloud_storage_link_refused
import emberr.shared.generated.resources.message_cloud_sync_went_wrong
import emberr.shared.generated.resources.message_unknown_error
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal fun CloudSyncProblem.asCloudText(): CloudText = when (this) {
    CloudSyncProblem.SignedOutButRecordsNotCleared -> CloudText.Phrase(Res.string.message_cloud_signed_out_records_not_cleared)
    is CloudSyncProblem.CloudRefusedSync -> CloudText.Phrase(Res.string.message_cloud_refused_sync, listOf(errorCode))
    CloudSyncProblem.ServerUnreachable -> CloudText.Phrase(Res.string.message_cloud_server_unreachable)
    is CloudSyncProblem.StorageLinkRefused -> CloudText.Phrase(Res.string.message_cloud_storage_link_refused, listOf(httpStatus))
    CloudSyncProblem.CouldNotDecrypt -> CloudText.Phrase(Res.string.message_cloud_could_not_decrypt)
    CloudSyncProblem.NoteUnreadable -> CloudText.Phrase(Res.string.message_cloud_note_unreadable)
    CloudSyncProblem.SomethingWentWrong -> CloudText.Phrase(Res.string.message_cloud_sync_went_wrong)
}

private val CloudMediaStep.phrase: StringResource
    get() = when (this) {
        CloudMediaStep.WHOLE_PASS -> Res.string.message_cloud_media_stopped
        CloudMediaStep.ASKING_FOR_DOWNLOAD -> Res.string.message_cloud_media_could_not_ask_download
        CloudMediaStep.SENDING -> Res.string.message_cloud_media_could_not_send
        CloudMediaStep.LINKING_TO_NOTE -> Res.string.message_cloud_media_could_not_link
        CloudMediaStep.UNLINKING_REMOVED_FILE -> Res.string.message_cloud_media_could_not_unlink
        CloudMediaStep.RECEIVING -> Res.string.message_cloud_media_could_not_receive
    }

@Composable
private fun CloudMediaFailure.text(): String = when (this) {
    is CloudMediaFailure.CloudAnswered -> stringResource(Res.string.message_cloud_media_reason_cloud_answered, errorCode, httpStatus)
    is CloudMediaFailure.StorageAnswered -> stringResource(Res.string.message_cloud_media_reason_storage_answered, httpStatus)
    CloudMediaFailure.ServerUnreachable -> stringResource(Res.string.message_cloud_media_reason_unreachable)
    CloudMediaFailure.NotDecrypted -> stringResource(Res.string.message_cloud_media_reason_not_decrypted)
    CloudMediaFailure.KeyMissing -> stringResource(Res.string.message_cloud_media_reason_key_missing)
    CloudMediaFailure.NetworkNotPicked -> stringResource(Res.string.message_cloud_media_reason_network_not_picked)
    is CloudMediaFailure.Other -> errorName ?: stringResource(Res.string.message_unknown_error)
}

@Composable
internal fun CloudMediaProblem.text(): String = when (this) {
    CloudMediaProblem.NoAccountStream -> stringResource(Res.string.message_cloud_media_no_account_stream)
    is CloudMediaProblem.StepFailed -> stringResource(step.phrase, failure.text())
}
