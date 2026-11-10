package com.emberr.presentation.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.presentation.shared.components.orHighContrastLine
import com.emberr.presentation.shared.components.shownSpaceName
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.widgets_choose_a_space
import emberr.shared.generated.resources.widgets_follow_the_app
import emberr.shared.generated.resources.widgets_follow_the_app_detail
import emberr.shared.generated.resources.widgets_current_space
import emberr.shared.generated.resources.widgets_untitled_space
import org.jetbrains.compose.resources.stringResource

@Composable
fun WidgetSpaceChooser(
    spaces: List<SpaceEntity>,
    activeSpaceId: String,
    onSpaceChosen: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(Res.string.widgets_choose_a_space),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                SpaceChooserRow(
                    title = stringResource(Res.string.widgets_follow_the_app),
                    subtitle = stringResource(Res.string.widgets_follow_the_app_detail),
                    onClick = { onSpaceChosen(null) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f).orHighContrastLine())
            }

            items(items = spaces, key = { space -> space.spaceId }) { space ->
                SpaceChooserRow(
                    title = shownSpaceName(space.displayName),
                    subtitle = if (space.spaceId == activeSpaceId) stringResource(Res.string.widgets_current_space) else null,
                    onClick = { onSpaceChosen(space.spaceId) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f).orHighContrastLine())
            }
        }
    }
}

@Composable
private fun SpaceChooserRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val untitledSpaceTitle = stringResource(Res.string.widgets_untitled_space)
            Text(
                text = title.trim().ifBlank { untitledSpaceTitle },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
