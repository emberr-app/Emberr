package com.emberr.domain.cloud.account

import com.nulabinc.zxcvbn.Zxcvbn

private val passwordStrengthMeter by lazy { Zxcvbn() }

actual fun estimateCloudPasswordStrength(password: String): CloudPasswordStrength {
    if (password.isEmpty()) return CloudPasswordStrength.WEAK
    val score = synchronized(passwordStrengthMeter) { passwordStrengthMeter.measure(password).score }
    return when {
        score < 2 -> CloudPasswordStrength.WEAK
        score < 3 -> CloudPasswordStrength.MODERATE
        else -> CloudPasswordStrength.STRONG
    }
}
