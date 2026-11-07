// The background gradients a gradient widget can use, and the saved-state key that remembers each widget's choice.
package com.emberr.presentation.widget.gradient

import androidx.annotation.DrawableRes
import androidx.datastore.preferences.core.stringPreferencesKey
import com.emberr.R

val widgetGradientKey = stringPreferencesKey("daily_jot_gradient")

enum class WidgetGradient(
    val storageName: String,
    val displayName: String,
    @DrawableRes val drawableResource: Int
) {
    DUSK("dusk", "Dusk", R.drawable.widget_daily_jot_gradient_dusk),
    EMBER("ember", "Ember", R.drawable.widget_daily_jot_gradient_ember),
    FOREST("forest", "Forest", R.drawable.widget_daily_jot_gradient_forest),
    LAGOON("lagoon", "Lagoon", R.drawable.widget_daily_jot_gradient_lagoon),
    MIDNIGHT("midnight", "Midnight", R.drawable.widget_daily_jot_gradient_midnight),
    BLOSSOM("blossom", "Blossom", R.drawable.widget_daily_jot_gradient_blossom),
    GRAPHITE("graphite", "Graphite", R.drawable.widget_daily_jot_gradient_graphite),
    AURORA("aurora", "Aurora", R.drawable.widget_daily_jot_gradient_aurora),
    SUNSET("sunset", "Sunset", R.drawable.widget_daily_jot_gradient_sunset),
    SLATE("slate", "Slate", R.drawable.widget_daily_jot_gradient_slate),
    COCOA("cocoa", "Cocoa", R.drawable.widget_daily_jot_gradient_cocoa),
    LAVENDER("lavender", "Lavender", R.drawable.widget_daily_jot_gradient_lavender);

    companion object {
        val defaultGradient = DUSK

        fun named(storageName: String?): WidgetGradient =
            entries.firstOrNull { gradient -> gradient.storageName == storageName } ?: defaultGradient
    }
}
