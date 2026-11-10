package com.emberr.presentation.shared

import com.emberr.domain.util.system.isWindowsDesktop

enum class TopBarFadeStyle {
    BLUR,
    COLOR,
    NONE,
    FALLBACK;

    val isAlwaysFlatOnThisPlatform: Boolean
        get() = this == BLUR && isWindowsDesktop
}
