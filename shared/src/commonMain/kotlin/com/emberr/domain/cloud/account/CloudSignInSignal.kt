package com.emberr.domain.cloud.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CloudSignInSignal {
    private val mutableIsSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = mutableIsSignedIn.asStateFlow()

    internal fun update(isSignedIn: Boolean) {
        mutableIsSignedIn.value = isSignedIn
    }
}
