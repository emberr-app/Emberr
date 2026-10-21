package com.emberr.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emberr.data.local.room.entity.CalendarTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<CalendarTaskEntity>)

    @Query("SELECT * FROM calendar_tasks WHERE targetDate LIKE :yearMonth || '%' AND noteId NOT IN (SELECT noteId FROM notes_metadata WHERE trashedAt IS NOT NULL)")
    suspend fun getTasksInMonth(yearMonth: String): List<CalendarTaskEntity>

    @Query("SELECT * FROM calendar_tasks WHERE targetDate = :dateString AND noteId NOT IN (SELECT noteId FROM notes_metadata WHERE trashedAt IS NOT NULL)")
    suspend fun getTasksOnDate(dateString: String): List<CalendarTaskEntity>

    @Query("SELECT * FROM calendar_tasks WHERE blockId = :blockId LIMIT 1")
    suspend fun getTaskById(blockId: String): CalendarTaskEntity?

    @Query("SELECT DISTINCT categoryId FROM calendar_tasks WHERE categoryId IN (:categoryIds)")
    suspend fun getCategoryIdsUsedByTasks(categoryIds: List<String>): List<String>

    // Used by the NoteEditorViewModel/DailyEditorViewModel to clear old tasks before saving new ones.
    @Query("SELECT * FROM calendar_tasks WHERE spaceId = :spaceId AND noteId = :noteId")
    suspend fun getTasksForNote(spaceId: String, noteId: String): List<CalendarTaskEntity>

    @Query("DELETE FROM calendar_tasks WHERE spaceId = :spaceId AND noteId = :noteId")
    suspend fun deleteTasksByNoteId(spaceId: String, noteId: String)

    @Query("SELECT * FROM calendar_tasks WHERE targetDate >= :fromDate AND isChecked = 0 AND noteId NOT IN (SELECT noteId FROM notes_metadata WHERE trashedAt IS NOT NULL) ORDER BY targetDate ASC")
    suspend fun getUpcomingTasks(fromDate: String): List<CalendarTaskEntity>

    @Query("SELECT * FROM calendar_tasks WHERE noteId NOT IN (SELECT noteId FROM notes_metadata WHERE trashedAt IS NOT NULL)")
    suspend fun getTasksOutsideTrashAcrossSpaces(): List<CalendarTaskEntity>

    @Query("SELECT * FROM calendar_tasks")
    fun getAllTasksAcrossSpacesFlow(): Flow<List<CalendarTaskEntity>>

    @Query("SELECT * FROM calendar_tasks WHERE noteId NOT IN (SELECT noteId FROM notes_metadata WHERE trashedAt IS NOT NULL)")
    fun getTasksOutsideTrashAcrossSpacesFlow(): Flow<List<CalendarTaskEntity>>

    @Query("SELECT * FROM calendar_tasks")
    suspend fun getAllTasksAcrossSpaces(): List<CalendarTaskEntity>
}
