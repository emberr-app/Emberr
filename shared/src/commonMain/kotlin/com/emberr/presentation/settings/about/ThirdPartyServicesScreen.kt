package com.emberr.presentation.settings.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.presentation.settings.SettingsActionRow
import com.emberr.presentation.settings.SettingsDivider
import com.emberr.presentation.settings.SettingsGroup
import com.emberr.presentation.settings.desktopSettingsSidePadding
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.topHeaderBarPadding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.common_back
import emberr.shared.generated.resources.settings_about_third_party_services
import emberr.shared.generated.resources.settings_third_party_group_movies_shows
import emberr.shared.generated.resources.settings_third_party_justwatch_notice
import emberr.shared.generated.resources.settings_third_party_tmdb_notice
import emberr.shared.generated.resources.settings_third_party_tmdb_proxy
import emberr.shared.generated.resources.settings_third_party_tmdb_usage
import emberr.shared.generated.resources.settings_third_party_visit_tmdb
import emberr.shared.generated.resources.logo_tmdb
import emberr.shared.generated.resources.square_arrow_out_up_right
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val TMDB_URL = "https://www.themoviedb.org"

@Composable
fun ThirdPartyServicesScreen(onNavigateBack: () -> Unit, showTopBar: Boolean = true) {
    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }
    val uriHandler = LocalUriHandler.current

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
            item(key = "tmdb") {
                SettingsGroup(title = stringResource(Res.string.settings_third_party_group_movies_shows)) {
                    TmdbNotice()
                    SettingsDivider()
                    SettingsActionRow(
                        icon = painterResource(Res.drawable.square_arrow_out_up_right),
                        title = stringResource(Res.string.settings_third_party_visit_tmdb),
                        onClick = { uriHandler.openUri(TMDB_URL) }
                    )
                }
            }
        }

        if (showTopBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(10f)
                    .onGloballyPositioned { coordinates -> topBarHeightPx = coordinates.size.height.toFloat() }
            ) {
                EmberrTopHeaderBar(
                    title = stringResource(Res.string.settings_about_third_party_services),
                    hazeState = hazeState,
                    contentPadding = topHeaderBarPadding(bottom = 16.dp),
                    backTooltip = stringResource(Res.string.common_back),
                    onBackClick = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun TmdbNotice() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(Res.string.settings_third_party_tmdb_usage),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.settings_third_party_tmdb_proxy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
        Icon(
            painter = painterResource(Res.drawable.logo_tmdb),
            contentDescription = "TMDB",
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(top = 16.dp)
                .height(12.dp)
        )
        Text(
            text = stringResource(Res.string.settings_third_party_tmdb_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = stringResource(Res.string.settings_third_party_justwatch_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
