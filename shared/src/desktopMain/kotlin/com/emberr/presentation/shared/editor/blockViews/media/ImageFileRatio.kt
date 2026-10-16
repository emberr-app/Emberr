package com.emberr.presentation.shared.editor.blockViews.media

import java.io.File
import javax.imageio.ImageIO

private const val JPEG_BYTES_TO_SCAN = 64 * 1024
private const val EXIF_ORIENTATION_TAG = 0x0112
private val QuarterTurnOrientations = 5..8

actual fun readImageWidthToHeightRatio(absolutePath: String): Float? = runCatching {
    val file = File(absolutePath)
    val (width, height) = ImageIO.createImageInputStream(file)?.use { stream ->
        val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull() ?: return@use null
        try {
            reader.setInput(stream, true, true)
            reader.getWidth(0) to reader.getHeight(0)
        } finally {
            reader.dispose()
        }
    } ?: return null
    if (width <= 0 || height <= 0) return null
    if (isJpegTurnedQuarterTurn(file)) height.toFloat() / width else width.toFloat() / height
}.getOrNull()

private fun isJpegTurnedQuarterTurn(file: File): Boolean {
    val bytes = file.inputStream().use { it.readNBytes(JPEG_BYTES_TO_SCAN) }
    if (bytes.size < 4 || bytes.unsignedByteAt(0) != 0xFF || bytes.unsignedByteAt(1) != 0xD8) return false

    var markerStart = 2
    while (markerStart + 4 <= bytes.size && bytes.unsignedByteAt(markerStart) == 0xFF) {
        val marker = bytes.unsignedByteAt(markerStart + 1)
        if (marker == 0xDA) return false
        val segmentLength = bytes.readBigEndianShort(markerStart + 2)
        if (marker == 0xE1 && bytes.startsWithExifHeader(markerStart + 4)) {
            return exifOrientation(bytes, tiffStart = markerStart + 10) in QuarterTurnOrientations
        }
        markerStart += 2 + segmentLength
    }
    return false
}

private fun exifOrientation(bytes: ByteArray, tiffStart: Int): Int? {
    val isLittleEndian = bytes.unsignedByteAt(tiffStart) == 'I'.code
    fun readShort(position: Int) =
        if (isLittleEndian) bytes.unsignedByteAt(position) or (bytes.unsignedByteAt(position + 1) shl 8)
        else bytes.readBigEndianShort(position)
    fun readInt(position: Int) =
        if (isLittleEndian) readShort(position) or (readShort(position + 2) shl 16)
        else (readShort(position) shl 16) or readShort(position + 2)

    val firstDirectoryStart = tiffStart + readInt(tiffStart + 4)
    val entryCount = readShort(firstDirectoryStart)
    for (entryIndex in 0 until entryCount) {
        val entryStart = firstDirectoryStart + 2 + entryIndex * 12
        if (readShort(entryStart) == EXIF_ORIENTATION_TAG) return readShort(entryStart + 8)
    }
    return null
}

private fun ByteArray.startsWithExifHeader(position: Int): Boolean =
    position + 6 <= size && decodeToString(position, position + 6) == "Exif\u0000\u0000"

private fun ByteArray.unsignedByteAt(position: Int): Int = this[position].toInt() and 0xFF

private fun ByteArray.readBigEndianShort(position: Int): Int =
    (unsignedByteAt(position) shl 8) or unsignedByteAt(position + 1)
