package com.emberr.presentation.home

import androidx.compose.runtime.Composable
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.common_delete
import emberr.shared.generated.resources.home_add_to_favorites
import emberr.shared.generated.resources.home_delete_items
import emberr.shared.generated.resources.home_remove_from_favorites
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TreeSelectionMenu.favoriteLabel(): String =
    if (makeFavorite) stringResource(Res.string.home_add_to_favorites)
    else stringResource(Res.string.home_remove_from_favorites)

@Composable
fun TreeSelectionMenu.deleteLabel(): String =
    if (selectedCount > 1) pluralStringResource(Res.plurals.home_delete_items, selectedCount, selectedCount)
    else stringResource(Res.string.common_delete)
