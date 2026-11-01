package com.emberr.domain.people

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.NoteKind
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

private const val PERSON_DETAIL_BLOCK_TYPE = "person_detail"

fun kindRecognizingPersonNote(kind: NoteKind, spaceId: String, blockContents: List<JsonElement>): NoteKind {
    if (kind != NoteKind.NOTE || spaceId != ACCOUNT_SPACE_ID) return kind
    return if (blockContents.any(::isLivePersonDetailBlock)) NoteKind.PERSON else kind
}

private fun isLivePersonDetailBlock(blockContent: JsonElement): Boolean {
    val block = blockContent as? JsonObject ?: return false
    val type = (block["type"] as? JsonPrimitive)?.contentOrNull
    val isDeleted = (block["isDeleted"] as? JsonPrimitive)?.booleanOrNull ?: false
    return type == PERSON_DETAIL_BLOCK_TYPE && !isDeleted
}
