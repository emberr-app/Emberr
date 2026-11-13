package com.emberr.domain.template

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.sample.STARTER_CONTENT_UPDATED_AT
import com.emberr.domain.space.ActiveSpaceStore
import com.emberr.domain.util.system.isDebugBuild

// Registry of every template that should exist out of the box. Add a new object (in its own
// file, implementing PredefinedTemplate) and list it here to ship another default template.
val PREDEFINED_TEMPLATES: List<PredefinedTemplate> = listOf(ProjectsTemplate, ResearchTemplate)

private val DEBUG_ONLY_TEMPLATES: List<PredefinedTemplate> = listOf(AllBlocksTemplate)

fun templatesNeverSeededInSpace(
    spaceId: String,
    seededTemplateNoteIds: Set<String>,
    templates: List<PredefinedTemplate> = PREDEFINED_TEMPLATES
): List<PredefinedTemplate> =
    templates.filter { template -> template.noteIdInSpace(spaceId) !in seededTemplateNoteIds }

class DefaultTemplateSeeder(
    private val repository: NoteRepository,
    private val activeSpaceStore: ActiveSpaceStore,
    private val settingsManager: SettingsManager
) {

    suspend fun seedIfMissing() {
        val now = System.currentTimeMillis()
        val spaceId = activeSpaceStore.currentActiveSpaceId()
        val templates = if (isDebugBuild) PREDEFINED_TEMPLATES + DEBUG_ONLY_TEMPLATES else PREDEFINED_TEMPLATES
        for (template in templatesNeverSeededInSpace(spaceId, settingsManager.getSeededTemplateNoteIds(), templates)) {
            val noteId = template.noteIdInSpace(spaceId)
            if (repository.getNoteById(noteId) == null) {
                template.seedSupportingNotes(repository, spaceId)
                repository.saveNote(
                    metadata = NoteMetadataEntity(
                        noteId = noteId,
                        title = template.title,
                        icon = template.icon,
                        folderId = null,
                        isDaily = false,
                        dateString = null,
                        createdAt = now,
                        updatedAt = STARTER_CONTENT_UPDATED_AT,
                        filePath = "",
                        isTemplate = true
                    ),
                    content = template.buildContent(spaceId),
                    stampUpdatedAt = false
                )
            }
            settingsManager.saveSeededTemplateNoteIds(settingsManager.getSeededTemplateNoteIds() + noteId)
        }
    }
}
