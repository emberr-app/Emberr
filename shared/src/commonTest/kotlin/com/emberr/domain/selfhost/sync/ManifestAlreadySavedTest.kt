package com.emberr.domain.selfhost.sync

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManifestAlreadySavedTest {

    private fun note(entryId: String, updatedAt: Long) =
        SelfHostManifestEntry(entryId = entryId, entryType = SelfHostEntryType.NOTE, updatedAt = updatedAt)

    @Test
    fun theSameEntriesInADifferentOrderAreAlreadySaved() {
        val serverManifest = SelfHostManifest(entries = listOf(note("groceries", 1000), note("trip", 2000)))
        val newManifest = SelfHostManifest(entries = listOf(note("trip", 2000), note("groceries", 1000)))

        assertTrue(newManifest.isAlreadySavedAs(serverManifest))
    }

    @Test
    fun aServerWithNoManifestYetStillGetsOne() {
        assertFalse(SelfHostManifest().isAlreadySavedAs(SelfHostManifest()))
    }
}
