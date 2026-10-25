package com.emberr.domain.selfhost.sync

import com.emberr.domain.selfhost.webdav.WebDavResourceInfo
import com.emberr.domain.selfhost.webdav.WebDavSyncPaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListFileSyncMemoryTest {

    private val spacesFile = WebDavSyncPaths.SPACES_FILE
    private val localSpaces = setOf("Work", "Home")
    private val spacesOnServer = "{\"Work\",\"Home\"}" to "\"etag-1\""

    @Test
    fun aListFileIsSkippedOnlyWhileTheServerAndThisDeviceBothStayTheSame() {
        val memory = ListFileSyncMemory()
        memory.startPass(mapOf(spacesFile to "\"etag-1\""))
        memory.rememberIfNothingChanged(spacesFile, spacesOnServer, localSpaces, changedNothing = true)

        assertTrue(memory.isAlreadyInSync(spacesFile, localSpaces))
        assertFalse(memory.isAlreadyInSync(spacesFile, localSpaces + "Travel"))

        memory.startPass(mapOf(spacesFile to "\"etag-2\""))
        assertFalse(memory.isAlreadyInSync(spacesFile, localSpaces))

        memory.startPass(emptyMap())
        assertFalse(memory.isAlreadyInSync(spacesFile, localSpaces))
    }

    @Test
    fun aPassThatChangedSomethingOrOnlyHadAWeakTagIsNotRemembered() {
        val memory = ListFileSyncMemory()
        memory.startPass(mapOf(spacesFile to "\"etag-1\"", WebDavSyncPaths.FOLDERS_FILE to "W/\"etag-1\""))

        memory.rememberIfNothingChanged(spacesFile, spacesOnServer, localSpaces, changedNothing = false)
        memory.rememberIfNothingChanged(
            WebDavSyncPaths.FOLDERS_FILE,
            "[]" to "W/\"etag-1\"",
            emptySet<String>(),
            changedNothing = true
        )

        assertFalse(memory.isAlreadyInSync(spacesFile, localSpaces))
        assertFalse(memory.isAlreadyInSync(WebDavSyncPaths.FOLDERS_FILE, emptySet<String>()))
    }

    @Test
    fun theFolderListingMarksMissingFilesAndLeavesOutFilesWithoutAStrongTag() {
        val listedFiles = listOf(
            WebDavResourceInfo(href = "/dav/emberr_sync/spaces.json", etag = "\"etag-1\"", isCollection = false, contentLength = 10),
            WebDavResourceInfo(href = "/dav/emberr_sync/folders.json", etag = "W/\"etag-1\"", isCollection = false, contentLength = 10),
            WebDavResourceInfo(href = "/dav/emberr_sync/notes/", etag = "\"etag-3\"", isCollection = true, contentLength = null)
        )

        val states = listFileStatesOnServer(
            listedFiles,
            listOf(spacesFile, WebDavSyncPaths.FOLDERS_FILE, WebDavSyncPaths.CATEGORIES_FILE)
        )

        assertEquals(
            mapOf(spacesFile to "\"etag-1\"", WebDavSyncPaths.CATEGORIES_FILE to LIST_FILE_NOT_ON_SERVER),
            states
        )
    }
}
