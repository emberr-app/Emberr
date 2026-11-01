package com.emberr.domain.people

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.DEFAULT_SPACE_ID
import com.emberr.data.local.room.entity.NoteKind
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PersonNoteRecognitionTest {

    private val liveDetail = Json.parseToJsonElement("""{"type":"person_detail","id":"d1","kind":"PHONE","value":"1"}""")
    private val deletedDetail = Json.parseToJsonElement("""{"type":"person_detail","id":"d2","kind":"PHONE","value":"1","isDeleted":true}""")
    private val textBlock = Json.parseToJsonElement("""{"type":"text","id":"t1","text":"hi"}""")

    @Test
    fun onlyAnAccountNoteWithALivePersonDetailBecomesAPerson() {
        assertEquals(NoteKind.PERSON, kindRecognizingPersonNote(NoteKind.NOTE, ACCOUNT_SPACE_ID, listOf(textBlock, liveDetail)))
        assertEquals(NoteKind.NOTE, kindRecognizingPersonNote(NoteKind.NOTE, ACCOUNT_SPACE_ID, listOf(textBlock, deletedDetail)))
        assertEquals(NoteKind.NOTE, kindRecognizingPersonNote(NoteKind.NOTE, DEFAULT_SPACE_ID, listOf(liveDetail)))
        assertEquals(NoteKind.CANVAS, kindRecognizingPersonNote(NoteKind.CANVAS, ACCOUNT_SPACE_ID, listOf(liveDetail)))
        assertEquals(NoteKind.PERSON, kindRecognizingPersonNote(NoteKind.PERSON, ACCOUNT_SPACE_ID, emptyList()))
    }
}
