package com.emberr.domain.util.media

private val imageMimeTypesByExtension = mapOf(
    "png" to "image/png",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "gif" to "image/gif",
    "webp" to "image/webp",
    "bmp" to "image/bmp",
    "heic" to "image/heic",
    "heif" to "image/heif"
)
private val otherMimeTypesByExtension = mapOf(
    "svg" to "image/svg+xml",
    "pdf" to "application/pdf",
    "txt" to "text/plain",
    "csv" to "text/csv",
    "doc" to "application/msword",
    "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "xls" to "application/vnd.ms-excel",
    "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "ppt" to "application/vnd.ms-powerpoint",
    "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "zip" to "application/zip",
    "mp3" to "audio/mpeg",
    "m4a" to "audio/mp4",
    "wav" to "audio/wav",
    "webm" to "audio/webm",
    "ogg" to "audio/ogg",
    "opus" to "audio/opus",
    "flac" to "audio/flac",
    "mp4" to "video/mp4"
)
private val mimeTypesByExtension = imageMimeTypesByExtension + otherMimeTypesByExtension

fun isImageExtension(extension: String): Boolean = extension.lowercase() in imageMimeTypesByExtension

fun mimeTypeForExtension(extension: String): String? = mimeTypesByExtension[extension.lowercase()]

fun imageExtensionForMimeType(mimeType: String): String? =
    imageMimeTypesByExtension.entries.firstOrNull { (_, imageMimeType) -> imageMimeType == mimeType }?.key
