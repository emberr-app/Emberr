package com.emberr.presentation.home.overview

import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.bookmark
import emberr.shared.generated.resources.check_square
import emberr.shared.generated.resources.images
import emberr.shared.generated.resources.notes2
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import emberr.shared.generated.resources.overview_section_bookmarks
import emberr.shared.generated.resources.overview_section_documents
import emberr.shared.generated.resources.overview_section_images
import emberr.shared.generated.resources.overview_section_tasks
import org.jetbrains.compose.resources.stringResource

enum class OverviewSection(val storageKey: String, val titleResource: StringResource, val icon: DrawableResource) {
    TASKS("tasks", Res.string.overview_section_tasks, Res.drawable.check_square),
    BOOKMARKS("bookmarks", Res.string.overview_section_bookmarks, Res.drawable.bookmark),
    IMAGES("images", Res.string.overview_section_images, Res.drawable.images),
    DOCUMENTS("documents", Res.string.overview_section_documents, Res.drawable.notes2)
}

@Composable
fun OverviewSection.label(): String = stringResource(titleResource)

fun visibleOverviewSections(hiddenStorageKeys: Set<String>): List<OverviewSection> =
    OverviewSection.entries.filterNot { section -> section.storageKey in hiddenStorageKeys }
