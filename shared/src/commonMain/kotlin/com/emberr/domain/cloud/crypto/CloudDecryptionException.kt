package com.emberr.domain.cloud.crypto

class CloudDecryptionException(cause: Throwable? = null) :
    Exception("Could not decrypt: wrong key, wrong associated data or damaged bytes", cause)
