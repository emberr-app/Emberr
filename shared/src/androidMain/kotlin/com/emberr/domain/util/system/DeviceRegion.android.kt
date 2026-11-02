package com.emberr.domain.util.system

import android.content.Context
import android.telephony.TelephonyManager
import org.koin.mp.KoinPlatform
import java.util.Locale

actual fun deviceRegionCode(): String? {
    val telephonyManager = KoinPlatform.getKoin().get<Context>().getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val networkCountry = telephonyManager?.networkCountryIso?.takeIf { it.isNotBlank() }
    val simCountry = telephonyManager?.simCountryIso?.takeIf { it.isNotBlank() }
    return networkCountry ?: simCountry ?: Locale.getDefault().country.takeIf { it.isNotBlank() }
}
