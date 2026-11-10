package com.emberr.presentation.settings.cloud

import androidx.compose.runtime.Composable
import com.emberr.domain.cloud.api.CloudAccountDetails
import com.emberr.domain.cloud.api.CloudSubscription
import com.emberr.domain.cloud.sync.CloudUploadsRefusedReason
import com.emberr.domain.util.format.formatByteSize
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.cloud_copy_removed
import emberr.shared.generated.resources.cloud_files_too_large
import emberr.shared.generated.resources.cloud_full
import emberr.shared.generated.resources.cloud_full_with_usage
import emberr.shared.generated.resources.cloud_storage_used
import emberr.shared.generated.resources.cloud_trial_days_left
import emberr.shared.generated.resources.cloud_trial_ended
import emberr.shared.generated.resources.cloud_trial_ended_read_only
import emberr.shared.generated.resources.cloud_trial_ends_today
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val READ_ONLY_TIME_AFTER_TRIAL_ENDS = 30.days

sealed interface CloudText {
    data class Phrase(val resource: StringResource, val arguments: List<Any> = emptyList()) : CloudText
    data class Counted(val resource: PluralStringResource, val count: Int) : CloudText
    data class Plain(val text: String) : CloudText
}

@Composable
internal fun CloudText.asText(): String = when (this) {
    is CloudText.Phrase -> stringResource(resource, *arguments.toTypedArray())
    is CloudText.Counted -> pluralStringResource(resource, count, count)
    is CloudText.Plain -> text
}

internal fun cloudPlanText(subscription: CloudSubscription, now: Instant, timeZone: TimeZone): CloudText? {
    if (!subscription.isTrial) return null
    val expiresAt = instantFromMicroseconds(subscription.expiresAtMicroseconds)
    if (now >= expiresAt) {
        val endDate = cloudDateText(expiresAt, timeZone)
        val readOnlyEndDate = cloudDateText(expiresAt + READ_ONLY_TIME_AFTER_TRIAL_ENDS, timeZone)
        return CloudText.Phrase(Res.string.cloud_trial_ended, listOf(endDate, readOnlyEndDate))
    }
    val today = now.toLocalDateTime(timeZone).date
    val lastDay = expiresAt.toLocalDateTime(timeZone).date
    return when (val daysLeft = today.daysUntil(lastDay)) {
        0 -> CloudText.Phrase(Res.string.cloud_trial_ends_today)
        else -> CloudText.Counted(Res.plurals.cloud_trial_days_left, daysLeft)
    }
}

internal fun cloudStorageUsedText(accountDetails: CloudAccountDetails): CloudText =
    CloudText.Phrase(Res.string.cloud_storage_used, storageUsedArguments(accountDetails))

internal fun cloudStorageUsedFraction(accountDetails: CloudAccountDetails): Float =
    (accountDetails.storageUsedBytes.toDouble() / accountDetails.subscription.storageLimitBytes).toFloat().coerceIn(0f, 1f)

internal fun uploadsRefusedMessage(reason: CloudUploadsRefusedReason, accountDetails: CloudAccountDetails?): CloudText =
    when (reason) {
        CloudUploadsRefusedReason.STORAGE_FULL ->
            accountDetails?.let { CloudText.Phrase(Res.string.cloud_full_with_usage, storageUsedArguments(it)) }
                ?: CloudText.Phrase(Res.string.cloud_full)
        CloudUploadsRefusedReason.TRIAL_ENDED -> CloudText.Phrase(Res.string.cloud_trial_ended_read_only)
    }

internal fun filesTooLargeMessage(fileCount: Int): CloudText? =
    if (fileCount == 0) null else CloudText.Counted(Res.plurals.cloud_files_too_large, fileCount)

internal fun cloudCopyRemovedMessage(removedAtMicroseconds: Long, timeZone: TimeZone): CloudText =
    CloudText.Phrase(
        Res.string.cloud_copy_removed,
        listOf(cloudDateText(instantFromMicroseconds(removedAtMicroseconds), timeZone))
    )

private fun storageUsedArguments(accountDetails: CloudAccountDetails): List<Any> =
    listOf(formatByteSize(accountDetails.storageUsedBytes), formatByteSize(accountDetails.subscription.storageLimitBytes))

private fun cloudDateText(instant: Instant, timeZone: TimeZone): String {
    val date = instant.toLocalDateTime(timeZone).date
    val monthName = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "${date.day} $monthName ${date.year}"
}

private fun instantFromMicroseconds(microseconds: Long): Instant = Instant.fromEpochMilliseconds(microseconds / 1_000)
