package com.emberr.presentation.settings.cloud

import com.emberr.domain.cloud.api.CloudSubscription
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
            "Free trial · 12 days left",
            cloudPlanText(trialEndingAt("2027-01-13T09:00:00Z"), now, TimeZone.UTC)
        )
        assertEquals(
            "Free trial · ends today",
            cloudPlanText(trialEndingAt("2027-01-01T23:30:00Z"), now, TimeZone.UTC)
        )
    }

    @Test
    fun anEndedTrialSaysWhenItEndedAndThatItIsReadOnlyForThirtyDays() {
        val now = Instant.parse("2027-01-05T10:00:00Z")

        assertEquals(
            "Free trial ended on 2 January 2027 · read-only until 1 February 2027",
            cloudPlanText(trialEndingAt("2027-01-02T12:00:00Z"), now, TimeZone.UTC)
        )
    }
}
