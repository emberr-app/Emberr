// The background gradients a gradient widget can use, and the saved-state key that remembers each widget's choice.
package com.emberr.presentation.widget.gradient

import androidx.annotation.DrawableRes
import androidx.datastore.preferences.core.stringPreferencesKey
import com.emberr.R
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.widgets_gradient_dusk
import emberr.shared.generated.resources.widgets_gradient_copper
import emberr.shared.generated.resources.widgets_gradient_forest
import emberr.shared.generated.resources.widgets_gradient_lagoon
import emberr.shared.generated.resources.widgets_gradient_midnight
import emberr.shared.generated.resources.widgets_gradient_blossom
import emberr.shared.generated.resources.widgets_gradient_graphite
import emberr.shared.generated.resources.widgets_gradient_aurora
import emberr.shared.generated.resources.widgets_gradient_sunset
import emberr.shared.generated.resources.widgets_gradient_slate
import emberr.shared.generated.resources.widgets_gradient_cocoa
import emberr.shared.generated.resources.widgets_gradient_lavender
import org.jetbrains.compose.resources.StringResource

val widgetGradientKey = stringPreferencesKey("daily_jot_gradient")

enum class WidgetGradient(
    val storageName: String,
    val nameResource: StringResource,
    @DrawableRes val drawableResource: Int
) {
    DUSK("dusk", Res.string.widgets_gradient_dusk, R.drawable.widget_daily_jot_gradient_dusk),
    COPPER("ember", Res.string.widgets_gradient_copper, R.drawable.widget_daily_jot_gradient_copper),
    FOREST("forest", Res.string.widgets_gradient_forest, R.drawable.widget_daily_jot_gradient_forest),
    LAGOON("lagoon", Res.string.widgets_gradient_lagoon, R.drawable.widget_daily_jot_gradient_lagoon),
    MIDNIGHT("midnight", Res.string.widgets_gradient_midnight, R.drawable.widget_daily_jot_gradient_midnight),
    BLOSSOM("blossom", Res.string.widgets_gradient_blossom, R.drawable.widget_daily_jot_gradient_blossom),
    GRAPHITE("graphite", Res.string.widgets_gradient_graphite, R.drawable.widget_daily_jot_gradient_graphite),
    AURORA("aurora", Res.string.widgets_gradient_aurora, R.drawable.widget_daily_jot_gradient_aurora),
    SUNSET("sunset", Res.string.widgets_gradient_sunset, R.drawable.widget_daily_jot_gradient_sunset),
    SLATE("slate", Res.string.widgets_gradient_slate, R.drawable.widget_daily_jot_gradient_slate),
    COCOA("cocoa", Res.string.widgets_gradient_cocoa, R.drawable.widget_daily_jot_gradient_cocoa),
    LAVENDER("lavender", Res.string.widgets_gradient_lavender, R.drawable.widget_daily_jot_gradient_lavender);

    companion object {
        val defaultGradient = DUSK

        fun named(storageName: String?): WidgetGradient =
            entries.firstOrNull { gradient -> gradient.storageName == storageName } ?: defaultGradient
    }
}
