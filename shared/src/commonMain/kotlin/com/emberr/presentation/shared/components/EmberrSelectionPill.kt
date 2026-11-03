package com.emberr.presentation.shared.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emberr.presentation.BOTTOM_BAR_BOTTOM_PADDING
import dev.chrisbanes.haze.HazeState
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.pen
import emberr.shared.generated.resources.star
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.x
import org.jetbrains.compose.resources.painterResource

private val DefaultCornerShape = RoundedCornerShape(12.dp)

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )

@Composable
fun EmberrSelectionPill(
    isVisible: Boolean,
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    showRename: Boolean = false,
    showFavorite: Boolean = false,
    favoriteLabel: String = "Add to Favorites",
    onRename: () -> Unit = {},
    onToggleFavorite: () -> Unit = {}
) {
    val tint = MaterialTheme.colorScheme.primary

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        Surface(
            shape = DefaultCornerShape,
            color = Color.Transparent,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = BOTTOM_BAR_BOTTOM_PADDING)
                .emberrBorder(DefaultCornerShape)
                .customEmberrShadow(DefaultCornerShape)
                .clip(DefaultCornerShape)
                .emberrBlur(hazeState, EmberrBlur.Regular)
        ) {
            val pillScroll = rememberScrollState()
            Row(
                modifier = Modifier.horizontalScroll(pillScroll)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(
                    painterResource(Res.drawable.x),
                    "Clear",
                    modifier = Modifier.size(18.dp).noRippleClickable { onClearSelection() },
                    tint = tint
                )
                Text(
                    "$selectedCount",
                    style = MaterialTheme.typography.bodyLarge,
                    color = tint
                )
                Box(Modifier.width(1.dp).height(18.dp).background(tint.copy(alpha = 0.2f)))
                if (showRename) {
                    Icon(
                        painterResource(Res.drawable.pen),
                        "Rename",
                        modifier = Modifier.size(18.dp).noRippleClickable { onRename() },
                        tint = tint
                    )
                }
                if (showFavorite) {
                    Icon(
                        painterResource(Res.drawable.star),
                        favoriteLabel,
                        modifier = Modifier.size(18.dp).noRippleClickable { onToggleFavorite() },
                        tint = tint
                    )
                }
                Icon(
                    painterResource(Res.drawable.trash),
                    "Move to Trash",
                    modifier = Modifier.size(18.dp).noRippleClickable { onDelete() },
                    tint = tint
                )
            }
        }
    }
}
