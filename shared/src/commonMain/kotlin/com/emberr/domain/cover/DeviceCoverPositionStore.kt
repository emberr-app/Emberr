package com.emberr.domain.cover

import com.emberr.data.local.prefs.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

const val CENTERED_COVER_POSITION = 0.5f

class DeviceCoverPositionStore(private val settingsManager: SettingsManager) {

    private val positionsByCover = MutableStateFlow<Map<Pair<String, String>, Float>>(emptyMap())

    fun currentPositionOf(noteId: String, coverImagePath: String): Float {
        val cover = noteId to coverImagePath
        positionsByCover.value[cover]?.let { return it }
        val savedPosition = settingsManager.getCoverImagePosition(noteId, coverImagePath) ?: CENTERED_COVER_POSITION
        positionsByCover.update { positions -> if (cover in positions) positions else positions + (cover to savedPosition) }
        return savedPosition
    }

    fun positionOf(noteId: String, coverImagePath: String): Flow<Float> {
        val firstPosition = currentPositionOf(noteId, coverImagePath)
        return positionsByCover.map { it[noteId to coverImagePath] ?: firstPosition }.distinctUntilChanged()
    }

    fun savePosition(noteId: String, coverImagePath: String, position: Float) {
        positionsByCover.update { positions -> positions + ((noteId to coverImagePath) to position) }
        settingsManager.saveCoverImagePosition(noteId, coverImagePath, position)
    }
}
