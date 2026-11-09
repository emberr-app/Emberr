package com.emberr.presentation.settings.cloud

import com.emberr.domain.cloud.api.CloudAccountDetails
import com.emberr.domain.cloud.api.CloudSubscription
import com.emberr.domain.cloud.sync.CloudUploadsRefusedReason
import com.emberr.domain.util.format.formatByteSize
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private val READ_ONLY_TIME_AFTER_TRIAL_ENDS = 30.days

internal fun cloudPlanText(subscription: CloudSubscription, now: Instant, timeZone: TimeZone): String? {
    if (!subscription.isTrial) return null
    val expiresAt = instantFromMicroseconds(subscription.expiresAtMicroseconds)
    if (now >= expiresAt) {
        val endDate = cloudDateText(expiresAt, timeZone)
        val readOnlyEndDate = cloudDateText(expiresAt + READ_ONLY_TIME_AFTER_TRIAL_ENDS, timeZone)
        return "Free trial ended on $endDate · read-only until $readOnlyEndDate"
    }
    val today = now.toLocalDateTime(timeZone).date
    val lastDay = expiresAt.toLocalDateTime(timeZone).date
    return when (val daysLeft = today.daysUntil(lastDay)) {
        0 -> "Free trial · ends today"
        1 -> "Free trial · 1 day left"
        else -> "Free trial · $daysLeft days left"
    }
}

internal fun cloudStorageUsedText(accountDetails: CloudAccountDetails): String =
    "${formatByteSize(accountDetails.storageUsedBytes)} of ${formatByteSize(accountDetails.subscription.storageLimitBytes)} used"

internal fun cloudStorageUsedFraction(accountDetails: CloudAccountDetails): Float =
    (accountDetails.storageUsedBytes.toDouble() / accountDetails.subscription.storageLimitBytes).toFloat().coerceIn(0f, 1f)

internal fun uploadsRefusedMessage(reason: CloudUploadsRefusedReason, accountDetails: CloudAccountDetails?): String =
    when (reason) {
        CloudUploadsRefusedReason.STORAGE_FULL -> {
            val usage = accountDetails?.let { ": ${cloudStorageUsedText(it)}" } ?: ""
            "Emberr Cloud is full$usage. New notes and files stay on this device and upload when there's room."
        }
        CloudUploadsRefusedReason.TRIAL_ENDED ->
            "Your free trial has ended, so Emberr Cloud is read-only. This device still gets changes from your " +
                "other devices, but nothing new uploads. Your notes stay on your devices."
    }

internal fun filesTooLargeMessage(fileCount: Int): String? = when (fileCount) {
    0 -> null
    1 -> "1 file is larger than 10 GB, so it stays on this device only."
    else -> "$fileCount files are larger than 10 GB, so they stay on this device only."
}

internal fun cloudCopyRemovedMessage(removedAtMicroseconds: Long, timeZone: TimeZone): String =
    "Your free trial ended and the cloud copy was removed on " +
        "${cloudDateText(instantFromMicroseconds(removedAtMicroseconds), timeZone)}. " +
        "Every note is still on this device. Syncing starts again when you choose a plan."

private fun cloudDateText(instant: Instant, timeZone: TimeZone): String {
    val date = instant.toLocalDateTime(timeZone).date
    val monthName = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "${date.day} $monthName ${date.year}"
}

private fun instantFromMicroseconds(microseconds: Long): Instant = Instant.fromEpochMilliseconds(microseconds / 1_000)
