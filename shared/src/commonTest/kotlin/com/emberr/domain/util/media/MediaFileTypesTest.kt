package com.emberr.domain.util.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaFileTypesTest {

    @Test
    fun commonPhotoTypesAreImages() {
        assertTrue(isImageExtension("png"))
        assertTrue(isImageExtension("JPEG"))
        assertTrue(isImageExtension("heic"))
        assertFalse(isImageExtension("pdf"))
    }

    @Test
    fun svgIsSavedAsADocumentBecauseTheAppCannotDrawIt() {
        assertFalse(isImageExtension("svg"))
        assertEquals("image/svg+xml", mimeTypeForExtension("svg"))
    }

    @Test
    fun everyAttachmentTypeHasItsMimeType() {
        assertEquals("image/jpeg", mimeTypeForExtension("JPG"))
        assertEquals("application/pdf", mimeTypeForExtension("pdf"))
        assertEquals("audio/mpeg", mimeTypeForExtension("mp3"))
        assertNull(mimeTypeForExtension("unknown"))
    }

    @Test
    fun aWebImageTypeGivesTheFileExtensionToSaveWith() {
        assertEquals("jpg", imageExtensionForMimeType("image/jpeg"))
        assertEquals("png", imageExtensionForMimeType("image/png"))
        assertNull(imageExtensionForMimeType("image/svg+xml"))
        assertNull(imageExtensionForMimeType("text/html"))
    }
}
