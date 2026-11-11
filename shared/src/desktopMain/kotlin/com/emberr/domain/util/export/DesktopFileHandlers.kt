package com.emberr.domain.util.export

import com.emberr.domain.backup.manual.BackupContentChoice
import com.emberr.domain.backup.manual.DesktopManualBackupExporter
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.util.media.MediaStorageHelper
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import java.awt.FileDialog
import java.awt.Frame
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_backup_saved_successfully
import emberr.shared.generated.resources.message_dialog_error_title
import emberr.shared.generated.resources.message_dialog_success_title
import emberr.shared.generated.resources.message_export_backup_title
import emberr.shared.generated.resources.message_export_failed_with_reason
import emberr.shared.generated.resources.message_export_markdown_title
import emberr.shared.generated.resources.message_export_pdf_title
import emberr.shared.generated.resources.message_markdown_save_error
import emberr.shared.generated.resources.message_markdown_saved_as
import emberr.shared.generated.resources.message_pdf_save_error
import emberr.shared.generated.resources.message_pdf_saved_as
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

fun handleExportMarkdown(window: Frame, export: MarkdownExport) {
    try {
        val dialog = FileDialog(window, phrase(Res.string.message_export_markdown_title), FileDialog.SAVE)
        dialog.file = export.fileName
        dialog.isVisible = true

        val chosenFileStr = dialog.file ?: return
        val chosenDirStr = dialog.directory

        var saveFile = if (chosenDirStr != null) java.io.File(chosenDirStr, chosenFileStr)
        else java.io.File(chosenFileStr)

        if (saveFile.name.endsWith(".txt", ignoreCase = true)) {
            saveFile = java.io.File(saveFile.absolutePath.removeSuffix(".txt").removeSuffix(".TXT"))
        }
        val extension = if (export.isZip) ".zip" else ".md"
        if (!saveFile.name.endsWith(extension, ignoreCase = true)) {
            saveFile = java.io.File(saveFile.absolutePath + extension)
        }

        val missingAttachmentCount = if (export.isZip) {
            val mediaStorageHelper = GlobalContext.get().get<MediaStorageHelper>()
            saveFile.outputStream().use { stream ->
                MarkdownZipWriter.write(export, stream) { storedFileName ->
                    java.io.File(mediaStorageHelper.getAbsoluteMediaPath(storedFileName))
                }
            }
        } else {
            saveFile.writeText(export.markdown)
            0
        }

        val missingNotice = runBlocking { MarkdownZipWriter.missingAttachmentsNotice(missingAttachmentCount) }?.let { "\n\n$it" }.orEmpty()
        val savedMessage = phrase(Res.string.message_markdown_saved_as, saveFile.name) + missingNotice
        val successTitle = phrase(Res.string.message_dialog_success_title)
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(window, savedMessage, successTitle, JOptionPane.INFORMATION_MESSAGE)
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        val errorMessage = phrase(Res.string.message_markdown_save_error, e.message.toString())
        val errorTitle = phrase(Res.string.message_dialog_error_title)
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(window, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE)
        }
    }
}

fun handleExportPdf(window: Frame, fileName: String, title: String, blocks: List<NoteBlock>) {
    try {
        val dialog = FileDialog(window, phrase(Res.string.message_export_pdf_title), FileDialog.SAVE)
        dialog.file = fileName
        dialog.isVisible = true

        val chosenFileStr = dialog.file ?: return
        val chosenDirStr = dialog.directory

        var saveFile = if (chosenDirStr != null) java.io.File(chosenDirStr, chosenFileStr)
        else java.io.File(chosenFileStr)

        if (saveFile.name.endsWith(".txt", ignoreCase = true)) {
            saveFile = java.io.File(saveFile.absolutePath.removeSuffix(".txt").removeSuffix(".TXT"))
        }
        if (!saveFile.name.endsWith(".pdf", ignoreCase = true)) {
            saveFile = java.io.File(saveFile.absolutePath + ".pdf")
        }

        generateDesktopPdf(saveFile, title, blocks)

        val savedMessage = phrase(Res.string.message_pdf_saved_as, saveFile.name)
        val successTitle = phrase(Res.string.message_dialog_success_title)
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(window, savedMessage, successTitle, JOptionPane.INFORMATION_MESSAGE)
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        val errorMessage = phrase(Res.string.message_pdf_save_error, e.message.toString())
        val errorTitle = phrase(Res.string.message_dialog_error_title)
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(window, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE)
        }
    }
}

fun handleExportBackup(window: Frame, choice: BackupContentChoice) {
    try {
        val fileName = "EmberrBackup_${System.currentTimeMillis()}.emberr"
        val dialog = FileDialog(window, phrase(Res.string.message_export_backup_title), FileDialog.SAVE)
        dialog.file = fileName
        dialog.isVisible = true

        val chosenFileStr = dialog.file ?: return
        val chosenDirStr = dialog.directory
        val saveFile = if (chosenDirStr != null) java.io.File(chosenDirStr, chosenFileStr)
        else java.io.File(chosenFileStr)

        val exporter = GlobalContext.get().get<DesktopManualBackupExporter>()
        runBlocking { exporter.exportToZip(saveFile, choice) }

        val savedMessage = phrase(Res.string.message_backup_saved_successfully)
        val successTitle = phrase(Res.string.message_dialog_success_title)
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(window, savedMessage, successTitle, JOptionPane.INFORMATION_MESSAGE)
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        val errorMessage = phrase(Res.string.message_export_failed_with_reason, e.message.toString())
        val errorTitle = phrase(Res.string.message_dialog_error_title)
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(window, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE)
        }
    }
}

private fun phrase(resource: StringResource, vararg arguments: Any): String =
    runBlocking { getString(resource, *arguments) }
