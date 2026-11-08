package com.emberr.presentation.shared

import com.emberr.domain.util.system.isWindowsDesktop

enum class TopBarFadeStyle(private val shortName: String, private val hasProgressiveVariant: Boolean) {
    BLUR("Blur", hasProgressiveVariant = true),
    COLOR("Color", hasProgressiveVariant = true),
    NONE("None", hasProgressiveVariant = false),
    FALLBACK("Fallback", hasProgressiveVariant = false);

    val isAlwaysFlatOnThisPlatform: Boolean
        get() = this == BLUR && isWindowsDesktop

    fun displayName(isProgressive: Boolean): String =
        if (isProgressive && hasProgressiveVariant && !isAlwaysFlatOnThisPlatform) "Progressive $shortName" else shortName
}
