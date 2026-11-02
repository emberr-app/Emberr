package com.emberr.presentation.people

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersonDetailValuesTest {

    @Test
    fun quickActionsBuildTheRightLinks() {
        assertEquals("tel:+919845012345", phoneCallLink("+91 98450 12345"))
        assertEquals("sms:+919845012345", phoneMessageLink("+91 98450-12345"))
        assertEquals("mailto:maya@example.com", emailLink(" maya@example.com "))
        assertEquals("https://maya.design", websiteLink("maya.design"))
        assertEquals("http://maya.design", websiteLink("http://maya.design"))
    }

    @Test
    fun socialLinksFollowTheLabelAndAnUnknownLabelGivesNoLink() {
        assertEquals("https://instagram.com/maya.draws", socialLink("Instagram", "@maya.draws"))
        assertEquals("https://github.com/maya", socialLink("GitHub", "maya"))
        assertEquals("https://mastodon.social/@maya", socialLink("Mastodon", "@maya@mastodon.social"))
        assertNull(socialLink("Bluesky", "@maya"))
        assertNull(socialLink(null, "@maya"))
    }

    @Test
    fun phonePlaceholderShowsTheRegionsCallingCodeOrPlainWords() {
        assertEquals("+91", phonePlaceholder("IN"))
        assertEquals("+1", phonePlaceholder("us"))
        assertEquals("Phone number", phonePlaceholder(null))
        assertEquals("Phone number", phonePlaceholder("ZZ"))
    }

    @Test
    fun birthdayChoicesBecomeIsoTextAndComeBack() {
        val thisYear = 2026
        val withYear = BirthdayChoice(month = 3, day = 14, yearText = "1996")
        assertEquals("1996-03-14", withYear.toIsoText(thisYear))
        assertEquals(withYear, birthdayChoiceFrom("1996-03-14"))

        val withoutYear = BirthdayChoice(month = 3, day = 14)
        assertEquals("--03-14", withoutYear.toIsoText(thisYear))
        assertEquals(withoutYear, birthdayChoiceFrom("--03-14"))

        assertEquals(29, daysInBirthdayMonth(2))
        assertEquals("--02-29", BirthdayChoice(month = 2, day = 29).toIsoText(thisYear))
        assertEquals("2000-02-29", BirthdayChoice(month = 2, day = 29, yearText = "2000").toIsoText(thisYear))
        assertNull(BirthdayChoice(month = 2, day = 29, yearText = "1997").toIsoText(thisYear))
        assertNull(BirthdayChoice(month = 3, day = null).toIsoText(thisYear))
    }
}
