// The background gradients a jot widget can use, and the saved-state key that remembers each widget's choice.
package com.emberr.presentation.widget.dailyjot

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.stringPreferencesKey
import com.emberr.R

val dailyJotGradientKey = stringPreferencesKey("daily_jot_gradient")

enum class DailyJotGradient(
    val storageName: String,
    val displayName: String,
    @DrawableRes val drawableResource: Int,
    val topColor: Color,
    val bottomColor: Color
) {
    DUSK("dusk", "Dusk", R.drawable.widget_daily_jot_gradient_dusk, Color(0xFF4F5B8A), Color(0xFF4F7F8A)),
    EMBER("ember", "Ember", R.drawable.widget_daily_jot_gradient_ember, Color(0xFF7A2E2A), Color(0xFFB8642F)),
    FOREST("forest", "Forest", R.drawable.widget_daily_jot_gradient_forest, Color(0xFF2F5D4A), Color(0xFF6B8550)),
    LAGOON("lagoon", "Lagoon", R.drawable.widget_daily_jot_gradient_lagoon, Color(0xFF1F5F82), Color(0xFF3E9A9E)),
    MIDNIGHT("midnight", "Midnight", R.drawable.widget_daily_jot_gradient_midnight, Color(0xFF1C2033), Color(0xFF3A4060)),
    BLOSSOM("blossom", "Blossom", R.drawable.widget_daily_jot_gradient_blossom, Color(0xFF7E4A66), Color(0xFFB97A7A)),
    GRAPHITE("graphite", "Graphite", R.drawable.widget_daily_jot_gradient_graphite, Color(0xFF262626), Color(0xFF4E4E4E)),
    AURORA("aurora", "Aurora", R.drawable.widget_daily_jot_gradient_aurora, Color(0xFF2E4A6B), Color(0xFF4F8A7A)),
    SUNSET("sunset", "Sunset", R.drawable.widget_daily_jot_gradient_sunset, Color(0xFF6B3A5A), Color(0xFFC46A4A)),
    SLATE("slate", "Slate", R.drawable.widget_daily_jot_gradient_slate, Color(0xFF3A4A5C), Color(0xFF6A7A8C)),
    COCOA("cocoa", "Cocoa", R.drawable.widget_daily_jot_gradient_cocoa, Color(0xFF3A2A22), Color(0xFF7A5A44)),
    LAVENDER("lavender", "Lavender", R.drawable.widget_daily_jot_gradient_lavender, Color(0xFF4E4A7A), Color(0xFF8A7AB0));

    companion object {
        val defaultGradient = DUSK

        fun named(storageName: String?): DailyJotGradient =
            entries.firstOrNull { gradient -> gradient.storageName == storageName } ?: defaultGradient
    }
}
