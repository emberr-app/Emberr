package com.emberr.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emberr.data.local.room.entity.MovieBlockEntity

@Dao
interface MovieBlockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMovieBlocks(movieBlocks: List<MovieBlockEntity>)

    @Query("DELETE FROM movie_blocks WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: String)

    @Query(
        "SELECT * FROM movie_blocks WHERE titleKey IN (:titleKeys) AND noteId != :exceptNoteId AND noteId IN " +
            "(SELECT noteId FROM notes_metadata WHERE spaceId = :spaceId AND trashedAt IS NULL AND isTemplate = 0)"
    )
    suspend fun getLinkedMovieBlocks(spaceId: String, titleKeys: List<String>, exceptNoteId: String): List<MovieBlockEntity>

    @Query(
        "SELECT * FROM movie_blocks WHERE titleKey = :titleKey AND noteId IN " +
            "(SELECT noteId FROM notes_metadata WHERE spaceId = :spaceId AND trashedAt IS NULL AND isTemplate = 0) " +
            "ORDER BY watchStateChangedAt DESC LIMIT 1"
    )
    suspend fun getNewestMovieBlockOfTitle(spaceId: String, titleKey: String): MovieBlockEntity?
}
