package com.emberr.presentation.shared.components

import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_could_not_move_to_trash
import emberr.shared.generated.resources.message_could_not_restore
import emberr.shared.generated.resources.message_moved_to_trash
import emberr.shared.generated.resources.message_restored
import org.jetbrains.compose.resources.getString

suspend fun couldNotMoveToTrashMessage(): String = getString(Res.string.message_could_not_move_to_trash)

suspend fun movedToTrashMessage(title: String): String =
    getString(Res.string.message_moved_to_trash, loadNoteTitleNames().shown(title))

suspend fun couldNotRestoreMessage(): String = getString(Res.string.message_could_not_restore)

suspend fun restoredMessage(title: String): String =
    getString(Res.string.message_restored, loadNoteTitleNames().shown(title))
