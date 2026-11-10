package com.emberr.presentation.onboarding

import androidx.compose.runtime.Composable
import com.emberr.domain.util.system.isDesktopPlatform
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.arrow_up_down
import emberr.shared.generated.resources.astroid
import emberr.shared.generated.resources.daily
import emberr.shared.generated.resources.pen_square
import emberr.shared.generated.resources.shield_alert
import emberr.shared.generated.resources.sidebar
import emberr.shared.generated.resources.sliders_horizontal
import emberr.shared.generated.resources.square_check
import emberr.shared.generated.resources.text_tool_2
import emberr.shared.generated.resources.text_type
import emberr.shared.generated.resources.transfer_h
import emberr.shared.generated.resources.widget
import org.jetbrains.compose.resources.DrawableResource
import emberr.shared.generated.resources.onboarding_ai_headline
import emberr.shared.generated.resources.onboarding_ai_lead
import emberr.shared.generated.resources.onboarding_blocks_detail
import emberr.shared.generated.resources.onboarding_blocks_headline
import emberr.shared.generated.resources.onboarding_blocks_lead
import emberr.shared.generated.resources.onboarding_daily_detail
import emberr.shared.generated.resources.onboarding_daily_headline
import emberr.shared.generated.resources.onboarding_daily_lead
import emberr.shared.generated.resources.onboarding_done_button
import emberr.shared.generated.resources.onboarding_done_detail
import emberr.shared.generated.resources.onboarding_done_headline
import emberr.shared.generated.resources.onboarding_done_lead
import emberr.shared.generated.resources.onboarding_layout_headline
import emberr.shared.generated.resources.onboarding_one_more_thing
import emberr.shared.generated.resources.onboarding_permissions_headline
import emberr.shared.generated.resources.onboarding_privacy_detail_desktop
import emberr.shared.generated.resources.onboarding_privacy_detail_mobile
import emberr.shared.generated.resources.onboarding_privacy_headline
import emberr.shared.generated.resources.onboarding_privacy_lead
import emberr.shared.generated.resources.onboarding_scrollbars_headline
import emberr.shared.generated.resources.onboarding_scrollbars_lead
import emberr.shared.generated.resources.onboarding_sync_detail
import emberr.shared.generated.resources.onboarding_sync_headline
import emberr.shared.generated.resources.onboarding_sync_lead
import emberr.shared.generated.resources.onboarding_text_size_headline
import emberr.shared.generated.resources.onboarding_text_size_lead
import emberr.shared.generated.resources.onboarding_typeface_headline
import emberr.shared.generated.resources.onboarding_typeface_lead
import emberr.shared.generated.resources.onboarding_welcome_button
import emberr.shared.generated.resources.onboarding_welcome_headline
import emberr.shared.generated.resources.onboarding_welcome_lead
import org.jetbrains.compose.resources.stringResource

enum class OnboardingChoice {
    NONE,
    TYPEFACE,
    TEXT_SIZE,
    AI_ASSISTANT,
    DESKTOP_LAYOUT,
    SCROLLBARS,
    PERMISSIONS
}

data class OnboardingStep(
    val icon: DrawableResource,
    val lead: String,
    val headline: String,
    val detail: String? = null,
    val choice: OnboardingChoice = OnboardingChoice.NONE,
    val buttonLabel: String? = null,
    val hasRainbowHeadline: Boolean = false
)

@Composable
fun rememberOnboardingSteps(): List<OnboardingStep> =
    buildList {
        add(
            OnboardingStep(
                icon = Res.drawable.pen_square,
                lead = stringResource(Res.string.onboarding_welcome_lead),
                headline = stringResource(Res.string.onboarding_welcome_headline),
                buttonLabel = stringResource(Res.string.onboarding_welcome_button)
            )
        )
        add(
            OnboardingStep(
                icon = Res.drawable.shield_alert,
                lead = stringResource(Res.string.onboarding_privacy_lead),
                headline = stringResource(Res.string.onboarding_privacy_headline),
                detail = if (isDesktopPlatform) {
                    stringResource(Res.string.onboarding_privacy_detail_desktop)
                } else {
                    stringResource(Res.string.onboarding_privacy_detail_mobile)
                }
            )
        )
        add(
            OnboardingStep(
                icon = Res.drawable.widget,
                lead = stringResource(Res.string.onboarding_blocks_lead),
                headline = stringResource(Res.string.onboarding_blocks_headline),
                detail = stringResource(Res.string.onboarding_blocks_detail)
            )
        )
        add(
            OnboardingStep(
                icon = Res.drawable.daily,
                lead = stringResource(Res.string.onboarding_daily_lead),
                headline = stringResource(Res.string.onboarding_daily_headline),
                detail = stringResource(Res.string.onboarding_daily_detail)
            )
        )
        add(
            OnboardingStep(
                icon = Res.drawable.transfer_h,
                lead = stringResource(Res.string.onboarding_sync_lead),
                headline = stringResource(Res.string.onboarding_sync_headline),
                detail = stringResource(Res.string.onboarding_sync_detail)
            )
        )
        add(
            OnboardingStep(
                icon = Res.drawable.text_type,
                lead = stringResource(Res.string.onboarding_typeface_lead),
                headline = stringResource(Res.string.onboarding_typeface_headline),
                choice = OnboardingChoice.TYPEFACE
            )
        )
        add(
            OnboardingStep(
                icon = Res.drawable.text_tool_2,
                lead = stringResource(Res.string.onboarding_text_size_lead),
                headline = stringResource(Res.string.onboarding_text_size_headline),
                choice = OnboardingChoice.TEXT_SIZE
            )
        )
        add(
            OnboardingStep(
                icon = Res.drawable.astroid,
                lead = stringResource(Res.string.onboarding_ai_lead),
                headline = stringResource(Res.string.onboarding_ai_headline),
                choice = OnboardingChoice.AI_ASSISTANT
            )
        )
        if (isDesktopPlatform) {
            add(
                OnboardingStep(
                    icon = Res.drawable.sidebar,
                    lead = stringResource(Res.string.onboarding_one_more_thing),
                    headline = stringResource(Res.string.onboarding_layout_headline),
                    choice = OnboardingChoice.DESKTOP_LAYOUT
                )
            )
            add(
                OnboardingStep(
                    icon = Res.drawable.arrow_up_down,
                    lead = stringResource(Res.string.onboarding_scrollbars_lead),
                    headline = stringResource(Res.string.onboarding_scrollbars_headline),
                    choice = OnboardingChoice.SCROLLBARS
                )
            )
        } else {
            add(
                OnboardingStep(
                    icon = Res.drawable.sliders_horizontal,
                    lead = stringResource(Res.string.onboarding_one_more_thing),
                    headline = stringResource(Res.string.onboarding_permissions_headline),
                    choice = OnboardingChoice.PERMISSIONS
                )
            )
        }
        add(
            OnboardingStep(
                icon = Res.drawable.square_check,
                lead = stringResource(Res.string.onboarding_done_lead),
                headline = stringResource(Res.string.onboarding_done_headline),
                detail = stringResource(Res.string.onboarding_done_detail),
                buttonLabel = stringResource(Res.string.onboarding_done_button),
                hasRainbowHeadline = true
            )
        )
    }
