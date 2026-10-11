package com.emberr.domain.importer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okio.Source
import okio.source
import java.io.IOException

private const val FALLBACK_FOLDER_NAME = "Folder"

class AndroidImportFolderReader(private val context: Context) : ImportFolderReader {

    override suspend fun readPickedFolder(folderLocation: String): PickedImportFiles = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(folderLocation)
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val folderName = displayNameOf(DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId))
        val files = mutableListOf<ImportFile>()
        val skippedNames = mutableListOf<String>()

        collectFiles(treeUri, rootDocumentId, folderName, files, skippedNames)
        PickedImportFiles(pickedNames = listOf(folderName), files = files, skippedNames = skippedNames) {}
    }

    private suspend fun collectFiles(
        treeUri: Uri,
        folderDocumentId: String,
        folderPath: String,
        files: MutableList<ImportFile>,
        skippedNames: MutableList<String>
    ) {
        val children = try {
            childrenOf(treeUri, folderDocumentId)
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        if (children == null) {
            skippedNames += folderPath
            return
        }

        for (child in children) {
            currentCoroutineContext().ensureActive()
            val childPath = "$folderPath/${child.name}"
            when {
                ImportPaths.isIgnoredEntry(childPath) -> Unit
                child.isFolder -> collectFiles(treeUri, child.documentId, childPath, files, skippedNames)
                ImportPaths.isUnsupportedObsidianFile(childPath) -> skippedNames += childPath
                else -> files += DocumentImportFile(
                    relativePath = childPath,
                    sizeBytes = child.sizeBytes,
                    lastModifiedAt = child.lastModifiedAt,
                    documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, child.documentId),
                    context = context
                )
            }
        }
    }

    private fun childrenOf(treeUri: Uri, folderDocumentId: String): List<FolderChild> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, folderDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        val children = mutableListOf<FolderChild>()
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val documentId = cursor.getString(0)
                val name = cursor.getString(1)
                if (documentId != null && !name.isNullOrBlank()) {
                    children += FolderChild(
                        documentId = documentId,
                        name = name,
                        isFolder = cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR,
                        sizeBytes = if (cursor.isNull(3)) 0L else cursor.getLong(3),
                        lastModifiedAt = if (cursor.isNull(4)) null else cursor.getLong(4).takeIf { it > 0 }
                    )
                }
            }
        }
        return children
    }

    private fun displayNameOf(documentUri: Uri): String {
        val name = try {
            context.contentResolver.query(documentUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        return name?.takeIf { it.isNotBlank() } ?: FALLBACK_FOLDER_NAME
    }
}

private class FolderChild(
    val documentId: String,
    val name: String,
    val isFolder: Boolean,
    val sizeBytes: Long,
    val lastModifiedAt: Long?
)

private class DocumentImportFile(
    override val relativePath: String,
    override val sizeBytes: Long,
    override val lastModifiedAt: Long?,
    private val documentUri: Uri,
    private val context: Context
) : ImportFile {
    override suspend fun readBytes(): ByteArray = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(documentUri)?.use { stream -> stream.readBytes() }
            ?: throw IOException("Cannot open $relativePath")
    }

    override fun openSource(): Source =
        context.contentResolver.openInputStream(documentUri)?.source() ?: throw IOException("Cannot open $relativePath")
}
