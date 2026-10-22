package com.emberr.presentation.shared

enum class TopBarFadeStyle(private val shortName: String, private val hasProgressiveVariant: Boolean) {
    BLUR("Blur", hasProgressiveVariant = true),
    COLOR("Color", hasProgressiveVariant = true),
    NONE("None", hasProgressiveVariant = false),
    FALLBACK("Fallback", hasProgressiveVariant = false);

    fun displayName(isProgressive: Boolean): String =
        if (isProgressive && hasProgressiveVariant) "Progressive $shortName" else shortName
}
