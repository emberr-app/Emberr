package com.emberr.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emberr.domain.model.WatchState
import com.emberr.domain.model.WatchStatus

@Entity(
    tableName = "movie_blocks",
    indices = [
        Index("noteId"),
        Index("titleKey")
    ]
)
data class MovieBlockEntity(
    @PrimaryKey val blockId: String,
    val noteId: String,
    val titleKey: String,
    val watchStatus: WatchStatus,
    val ratingHalfStars: Int?,
    val watchStateChangedAt: Long
) {
    fun watchState(): WatchState = WatchState(watchStatus, ratingHalfStars, watchStateChangedAt)
}
