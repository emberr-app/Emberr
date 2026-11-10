package com.emberr.presentation.shared.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.emberr.data.local.room.entity.DEFAULT_SPACE_NAME
import com.emberr.domain.people.BIRTHDAYS_CATEGORY_DEFAULT_NAME
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.calendar_birthdays
import emberr.shared.generated.resources.common_untitled
import emberr.shared.generated.resources.editor_loading
import emberr.shared.generated.resources.saved_name_canvas_group
import emberr.shared.generated.resources.saved_name_inbox
import emberr.shared.generated.resources.saved_name_loading_preview
import emberr.shared.generated.resources.saved_name_new_chat
import emberr.shared.generated.resources.saved_name_numbered_view
import emberr.shared.generated.resources.saved_name_others_folder
import emberr.shared.generated.resources.saved_name_personal_space
import emberr.shared.generated.resources.saved_name_unknown_link
import emberr.shared.generated.resources.saved_name_view_copy
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private const val UNTITLED_NOTE_TITLE = "Untitled"
private const val INBOX_NOTE_TITLE = "Inbox"
const val OTHERS_FOLDER_NAME = "Others"
const val NEW_CHAT_TITLE = "New Chat"
const val CANVAS_GROUP_DEFAULT_TITLE = "Group"
private const val BOOKMARK_LOADING_TITLE = "Loading..."
private const val BOOKMARK_LOADING_PREVIEW_TITLE = "Loading preview..."
private const val UNKNOWN_LINK_TITLE = "Unknown Link"
const val VIEW_COPY_SUFFIX = " copy"

class NoteTitleNames(private val untitled: String, private val inbox: String) {
    fun shown(storedTitle: String): String = when (storedTitle.trim()) {
        "", UNTITLED_NOTE_TITLE -> untitled
        INBOX_NOTE_TITLE -> inbox
        else -> storedTitle
    }
}

@Composable
fun rememberNoteTitleNames(): NoteTitleNames {
    val untitled = stringResource(Res.string.common_untitled)
    val inbox = stringResource(Res.string.saved_name_inbox)
    return remember(untitled, inbox) { NoteTitleNames(untitled, inbox) }
}

suspend fun loadNoteTitleNames(): NoteTitleNames =
    NoteTitleNames(getString(Res.string.common_untitled), getString(Res.string.saved_name_inbox))

@Composable
fun shownNoteTitle(storedTitle: String): String = rememberNoteTitleNames().shown(storedTitle)

@Composable
fun shownFolderName(storedName: String): String =
    shownSavedName(storedName, OTHERS_FOLDER_NAME, Res.string.saved_name_others_folder)

@Composable
fun shownSpaceName(storedName: String): String =
    shownSavedName(storedName, DEFAULT_SPACE_NAME, Res.string.saved_name_personal_space)

suspend fun loadShownSpaceName(storedName: String): String =
    if (storedName == DEFAULT_SPACE_NAME) getString(Res.string.saved_name_personal_space) else storedName

@Composable
fun shownCategoryName(storedName: String): String =
    shownSavedName(storedName, BIRTHDAYS_CATEGORY_DEFAULT_NAME, Res.string.calendar_birthdays)

@Composable
fun shownChatTitle(storedTitle: String): String =
    shownSavedName(storedTitle, NEW_CHAT_TITLE, Res.string.saved_name_new_chat)

@Composable
fun shownCanvasGroupTitle(storedTitle: String): String =
    shownSavedName(storedTitle, CANVAS_GROUP_DEFAULT_TITLE, Res.string.saved_name_canvas_group)

@Composable
fun shownBookmarkTitle(storedTitle: String): String =
    bookmarkTitlePhrase(storedTitle)?.let { stringResource(it) } ?: storedTitle

suspend fun loadShownBookmarkTitle(storedTitle: String): String =
    bookmarkTitlePhrase(storedTitle)?.let { getString(it) } ?: storedTitle

private fun bookmarkTitlePhrase(storedTitle: String): StringResource? = when (storedTitle) {
    BOOKMARK_LOADING_TITLE -> Res.string.editor_loading
    BOOKMARK_LOADING_PREVIEW_TITLE -> Res.string.saved_name_loading_preview
    UNKNOWN_LINK_TITLE -> Res.string.saved_name_unknown_link
    else -> null
}

@Composable
fun shownViewName(storedName: String, shownLabelsByEnglishLabel: Map<String, String>): String {
    if (storedName.endsWith(VIEW_COPY_SUFFIX)) {
        val copiedName = shownViewName(storedName.removeSuffix(VIEW_COPY_SUFFIX), shownLabelsByEnglishLabel)
        return stringResource(Res.string.saved_name_view_copy, copiedName)
    }
    shownLabelsByEnglishLabel[storedName]?.let { return it }
    val shownLabel = shownLabelsByEnglishLabel[storedName.substringBeforeLast(' ')]
    val number = storedName.substringAfterLast(' ').toIntOrNull()
    return if (shownLabel != null && number != null) stringResource(Res.string.saved_name_numbered_view, shownLabel, number) else storedName
}

@Composable
private fun shownSavedName(storedName: String, englishDefault: String, phrase: StringResource): String =
    if (storedName == englishDefault) stringResource(phrase) else storedName
