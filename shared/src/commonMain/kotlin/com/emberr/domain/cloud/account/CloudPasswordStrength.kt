package com.emberr.domain.cloud.account

enum class CloudPasswordStrength { WEAK, MODERATE, STRONG }

expect fun estimateCloudPasswordStrength(password: String): CloudPasswordStrength
