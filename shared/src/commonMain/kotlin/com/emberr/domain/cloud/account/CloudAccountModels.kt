package com.emberr.domain.cloud.account

sealed interface CloudSignInState {
    data object SignedOut : CloudSignInState
    data class SignedIn(val email: String) : CloudSignInState
}

class CloudPendingLogin(
    val email: String,
    val passwordKey: ByteArray,
    val loginKey: ByteArray
)

class CloudPendingRecovery(
    val email: String,
    val sessionToken: String,
    val masterKey: ByteArray,
    val privateKey: ByteArray
)
