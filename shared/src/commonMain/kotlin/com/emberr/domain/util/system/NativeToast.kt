package com.emberr.domain.util.system

expect fun showNativeToast(message: String)

const val COULD_NOT_MOVE_TO_TRASH_MESSAGE = "Couldn't move to Trash"

fun movedToTrashMessage(title: String): String = "Moved ${title.trim().ifEmpty { "Untitled" }} to Trash"

const val COULD_NOT_RESTORE_MESSAGE = "Couldn't restore"

fun restoredMessage(title: String): String = "Restored ${title.trim().ifEmpty { "Untitled" }}"
