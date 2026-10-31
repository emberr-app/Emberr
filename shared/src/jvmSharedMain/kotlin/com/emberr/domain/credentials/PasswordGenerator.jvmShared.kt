package com.emberr.domain.credentials

import java.security.SecureRandom

private val secureRandom by lazy { SecureRandom() }

actual fun secureRandomIndex(bound: Int): Int = secureRandom.nextInt(bound)
