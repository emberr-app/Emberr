package com.emberr.domain.util.system

import java.util.Locale

actual fun deviceRegionCode(): String? = Locale.getDefault().country.takeIf { it.isNotBlank() }
