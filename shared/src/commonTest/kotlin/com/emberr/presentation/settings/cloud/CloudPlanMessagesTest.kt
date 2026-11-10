package com.emberr.presentation.settings.cloud

import com.emberr.domain.cloud.api.CloudSubscription
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.cloud_trial_days_left
import emberr.shared.generated.resources.cloud_trial_ended
import emberr.shared.generated.resources.cloud_trial_ends_today
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class CloudPlanMessagesTest {

    private fun trialEndingAt(isoTime: String) = CloudSubscription(
        plan = CloudSubscription.TRIAL_PLAN,
        storageLimitBytes = 104_857_600L,
        expiresAtMicroseconds = Instant.parse(isoTime).toEpochMilliseconds() * 1_000,
        cloudCopyRemovedAtMicroseconds = null
    )

    @Test
    fun aRunningTrialCountsCalendarDaysLeftAndSaysWhenItEndsToday() {
        val now = Instant.parse("2027-01-01T22:00:00Z")

        assertEquals(
            CloudText.Counted(Res.plurals.cloud_trial_days_left, 12),
            cloudPlanText(trialEndingAt("2027-01-13T09:00:00Z"), now, TimeZone.UTC)
        )
        assertEquals(
            CloudText.Phrase(Res.string.cloud_trial_ends_today),
            cloudPlanText(trialEndingAt("2027-01-01T23:30:00Z"), now, TimeZone.UTC)
        )
    }

    @Test
    fun anEndedTrialSaysWhenItEndedAndThatItIsReadOnlyForThirtyDays() {
        val now = Instant.parse("2027-01-05T10:00:00Z")

        assertEquals(
            CloudText.Phrase(Res.string.cloud_trial_ended, listOf("2 January 2027", "1 February 2027")),
            cloudPlanText(trialEndingAt("2027-01-02T12:00:00Z"), now, TimeZone.UTC)
        )
    }
}
