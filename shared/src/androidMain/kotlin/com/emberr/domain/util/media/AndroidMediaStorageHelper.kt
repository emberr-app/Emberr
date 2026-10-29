package com.emberr.domain.util.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Source
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

private const val CONVERTED_HEIC_JPEG_QUALITY = 95

class AndroidMediaStorageHelper(private val context: Context) : MediaStorageHelper {

    // Define the new clean media directory
    private val mediaDir = File(context.filesDir, "media").apply { mkdirs() }

    override suspend fun copyUriToInternalStorage(uriString: String): MediaInfo? = withContext(Dispatchers.IO) {
        return@withContext try {
            val uri = Uri.parse(uriString)
            val contentResolver = context.contentResolver
            var displayName = "Unknown_File"
            var size = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }

            if (displayName == "Unknown_File" && uriString.contains("camera_")) {
                displayName = "Camera_Photo.jpg"
            }

            val mimeType = contentResolver.getType(uri) ?: if (displayName.lowercase().endsWith(".jpg")) "image/jpeg" else "application/octet-stream"
            val extension = displayName.substringAfterLast('.', "")
            val uprightHeicBitmap = if (mimeType == "image/heic" || mimeType == "image/heif") decodeUprightBitmapOrNull(uri) else null
            val savedExtension = if (uprightHeicBitmap != null) "jpg" else extension.ifEmpty { "jpg" }
            val localFileName = "media_${UUID.randomUUID()}.$savedExtension"

            // Save the file into the dedicated media folder
            val file = File(mediaDir, localFileName)

            if (uprightHeicBitmap != null) {
                FileOutputStream(file).use { out -> uprightHeicBitmap.compress(Bitmap.CompressFormat.JPEG, CONVERTED_HEIC_JPEG_QUALITY, out) }
                uprightHeicBitmap.recycle()
            } else {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream -> inputStream.copyTo(outputStream) }
                }
            }

            size = file.length()

            MediaInfo(
                localFileName = localFileName,
                originalName = displayName,
                mimeType = if (uprightHeicBitmap != null) "image/jpeg" else mimeType,
                sizeBytes = size
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeUprightBitmapOrNull(uri: Uri): Bitmap? =
        try {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (_: IOException) {
            null
        }

    override suspend fun saveBytesToInternalStorage(bytes: ByteArray, extension: String, mimeType: String): MediaInfo? =
        withContext(Dispatchers.IO) {
            try {
                val localFileName = "media_${UUID.randomUUID()}${if (extension.isNotEmpty()) ".$extension" else ""}"
                val file = File(mediaDir, localFileName)
                FileOutputStream(file).use { it.write(bytes) }

                MediaInfo(
                    localFileName = localFileName,
                    originalName = localFileName,
                    mimeType = mimeType,
                    sizeBytes = file.length()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    override suspend fun saveStreamToInternalStorage(
        openSource: () -> Source,
        extension: String,
        mimeType: String,
        onBytesCopied: (copiedBytes: Long) -> Unit
    ): MediaInfo? =
        withContext(Dispatchers.IO) {
            val localFileName = "media_${UUID.randomUUID()}${if (extension.isNotEmpty()) ".$extension" else ""}"
            val file = File(mediaDir, localFileName)
            try {
                copySourceIntoFile(openSource, file, onBytesCopied)

                MediaInfo(
                    localFileName = localFileName,
                    originalName = localFileName,
                    mimeType = mimeType,
                    sizeBytes = file.length()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                file.delete()
                null
            }
        }

    override fun getAbsoluteMediaPath(fileName: String): String {
        val cleanName = mediaFileNameOnly(fileName)
        return File(mediaDir, cleanName).absolutePath
    }

    override fun listAllMediaFileNames(): List<String> {
        return mediaDir.listFiles()?.mapNotNull { if (it.isFile) it.name else null } ?: emptyList()
    }

    override suspend fun deleteMediaFile(fileName: String) {
        withContext(Dispatchers.IO) { File(getAbsoluteMediaPath(fileName)).delete() }
    }
}