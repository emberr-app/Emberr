package com.emberr.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.IconTooltipPosition
import com.emberr.presentation.shared.components.NoRippleIndicationNodeFactory
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.logo_github_outline
import emberr.shared.generated.resources.logo_reddit_outline
import emberr.shared.generated.resources.logo_x_outline
import emberr.shared.generated.resources.settings_footer_support_us
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private class EmberrSocialLink(val name: String, val address: String, val icon: DrawableResource, val iconSize: Dp)

private val EmberrSocialLinks = listOf(
    EmberrSocialLink(name = "X", address = "https://x.com/emberr_app", icon = Res.drawable.logo_x_outline, iconSize = 22.dp),
    EmberrSocialLink(name = "GitHub", address = "https://github.com/emberr-app/Emberr", icon = Res.drawable.logo_github_outline, iconSize = 24.dp),
    EmberrSocialLink(name = "Reddit", address = "https://www.reddit.com/r/Emberr/s/HLrywiUCkd", icon = Res.drawable.logo_reddit_outline, iconSize = 24.dp)
)

@Composable
internal fun SettingsFooter() {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.settings_footer_support_us),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmberrSocialLinks.forEach { link ->
                SocialLinkIcon(link = link, onClick = { uriHandler.openUri(link.address) })
            }
        }
    }
}

@Composable
private fun SocialLinkIcon(link: EmberrSocialLink, onClick: () -> Unit) {
    IconTooltip(text = link.name, position = IconTooltipPosition.Above) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = NoRippleIndicationNodeFactory,
                    onClick = onClick
                )
        ) {
            Icon(
                painter = painterResource(link.icon),
                contentDescription = link.name,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(link.iconSize)
            )
        }
    }
}
