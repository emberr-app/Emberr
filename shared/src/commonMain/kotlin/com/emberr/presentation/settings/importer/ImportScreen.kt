package com.emberr.presentation.settings.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.presentation.settings.SettingsActionRow
import com.emberr.presentation.settings.SettingsDivider
import com.emberr.presentation.settings.SettingsFootnote
import com.emberr.presentation.settings.SettingsGroup
import com.emberr.presentation.settings.desktopSettingsSidePadding
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.topHeaderBarPadding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.database
import emberr.shared.generated.resources.doc_text
import emberr.shared.generated.resources.file_text
import emberr.shared.generated.resources.notes2
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

private const val COMING_SOON_LABEL = "Coming Soon"

@Composable
fun ImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportViewModel = koinViewModel()
) {
    val spaces by viewModel.spaces.collectAsState()
    val selectedSpaceId by viewModel.selectedSpaceId.collectAsState()
    val pickedFilesState by viewModel.pickedFilesState.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    var showMarkdownImportSheet by remember { mutableStateOf(false) }

    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sidePadding = desktopSettingsSidePadding(maxWidth)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = sidePadding,
                top = topBarHeightDp + 8.dp,
                end = sidePadding,
                bottom = 48.dp
            )
        ) {
            item(key = "import_sources") {
                SettingsGroup(title = "Import From") {
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.file_text),
                        title = "Text & Markdown",
                        onClick = {
                            viewModel.prepareMarkdownImport()
                            showMarkdownImportSheet = true
                        }
                    )
                    SettingsFootnote(text = "Also works for Obsidian and Bear notes.")
                    SettingsDivider()
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.database),
                        title = "Notion",
                        trailingLabel = COMING_SOON_LABEL,
                        isEnabled = false,
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.notes2),
                        title = "Joplin",
                        trailingLabel = COMING_SOON_LABEL,
                        isEnabled = false,
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.doc_text),
                        title = "Evernote",
                        trailingLabel = COMING_SOON_LABEL,
                        isEnabled = false,
                        onClick = {}
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
                .onGloballyPositioned { coordinates -> topBarHeightPx = coordinates.size.height.toFloat() }
        ) {
            EmberrTopHeaderBar(
                title = "Import Notes",
                hazeState = hazeState,
                contentPadding = topHeaderBarPadding(bottom = 16.dp),
                onBackClick = onNavigateBack
            )
        }
    }

    if (showMarkdownImportSheet) {
        MarkdownImportSheet(
            spaces = spaces,
            selectedSpaceId = selectedSpaceId,
            pickedFilesState = pickedFilesState,
            importProgress = importProgress,
            onFilesPicked = viewModel::readPickedFiles,
            onFolderPicked = viewModel::readPickedFolder,
            onSpaceSelected = viewModel::selectSpace,
            onImportClick = viewModel::startImport,
            onDismiss = {
                showMarkdownImportSheet = false
                viewModel.closeMarkdownImport()
            }
        )
    }
}
