package com.emberr.presentation.shared.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.emberr.presentation.BOTTOM_BAR_BOTTOM_PADDING
import com.emberr.presentation.BottomBarSearchField
import com.emberr.presentation.EXPANDED_BOTTOM_BAR_PILL_HEIGHT
import dev.chrisbanes.haze.HazeState
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.search
import org.jetbrains.compose.resources.painterResource

private val SEARCH_BUTTON_SIZE = 44.dp
private val CLOSED_SEARCH_ICON_SIZE = 22.dp
private val OPEN_SEARCH_ICON_SIZE = 18.dp
private val OPEN_SEARCH_ICON_START_OFFSET = 20.dp

@Composable
fun EmberrExpandingSearchBar(
    isOpen: Boolean,
    placeholder: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenClick: () -> Unit,
    onCloseClick: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val keyboardInsets = WindowInsets.ime
    val navigationBarInsets = WindowInsets.navigationBars
    val headerEndPadding = topHeaderBarPadding().calculateEndPadding(LocalLayoutDirection.current)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .offset {
                val keyboardLift = keyboardInsets.getBottom(this) - navigationBarInsets.getBottom(this)
                IntOffset(0, -keyboardLift.coerceAtLeast(0))
            }
            .navigationBarsPadding()
            .padding(start = 16.dp, end = headerEndPadding, bottom = BOTTOM_BAR_BOTTOM_PADDING),
        contentAlignment = Alignment.BottomEnd
    ) {
        val openWidth = maxWidth
        val morphAnimationSpec = tween<Dp>(durationMillis = 350, easing = FastOutSlowInEasing)
        val barWidth by animateDpAsState(
            targetValue = if (isOpen) openWidth else SEARCH_BUTTON_SIZE,
            animationSpec = morphAnimationSpec
        )
        val barHeight by animateDpAsState(
            targetValue = if (isOpen) EXPANDED_BOTTOM_BAR_PILL_HEIGHT else SEARCH_BUTTON_SIZE,
            animationSpec = morphAnimationSpec
        )

        val searchIconStartOffset by animateDpAsState(
            targetValue = if (isOpen) OPEN_SEARCH_ICON_START_OFFSET else (SEARCH_BUTTON_SIZE - CLOSED_SEARCH_ICON_SIZE) / 2,
            animationSpec = morphAnimationSpec
        )
        val searchIconSize by animateDpAsState(
            targetValue = if (isOpen) OPEN_SEARCH_ICON_SIZE else CLOSED_SEARCH_ICON_SIZE,
            animationSpec = morphAnimationSpec
        )
        val searchIconAlpha by animateFloatAsState(
            targetValue = if (isOpen) 0.6f else 1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )

        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier
                .padding(bottom = (EXPANDED_BOTTOM_BAR_PILL_HEIGHT - barHeight) / 2)
                .size(width = barWidth, height = barHeight)
                .emberrBorder(CircleShape)
                .customEmberrShadow(CircleShape, elevation = EmberrShadowElevation.Standard)
                .clip(CircleShape)
                .emberrBlur(hazeState, EmberrBlur.Regular)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = isOpen,
                    enter = fadeIn(tween(durationMillis = 200, delayMillis = 100)),
                    exit = fadeOut(tween(durationMillis = 150))
                ) {
                    Box(
                        modifier = Modifier
                            .wrapContentSize(Alignment.CenterStart, unbounded = true)
                            .size(width = openWidth, height = EXPANDED_BOTTOM_BAR_PILL_HEIGHT)
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        BottomBarSearchField(
                            query = query,
                            onQueryChange = onQueryChange,
                            onCloseClick = onCloseClick,
                            placeholder = placeholder,
                            isSearchIconVisible = false
                        )
                    }
                }

                Icon(
                    painter = painterResource(Res.drawable.search),
                    contentDescription = if (isOpen) null else placeholder,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = searchIconStartOffset)
                        .size(searchIconSize)
                        .alpha(searchIconAlpha)
                )

                if (!isOpen) {
                    Box(modifier = Modifier.matchParentSize()) {
                        IconTooltip(text = placeholder, position = IconTooltipPosition.Above) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = NoRippleIndicationNodeFactory,
                                        onClick = onOpenClick
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
