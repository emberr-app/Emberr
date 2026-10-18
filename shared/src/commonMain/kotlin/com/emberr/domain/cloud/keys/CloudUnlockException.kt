package com.emberr.domain.cloud.keys

class IncorrectCloudPasswordException(cause: Throwable) :
    Exception("Incorrect password, could not unlock the Emberr Cloud master key", cause)

class IncorrectRecoveryKeyException(cause: Throwable) :
    Exception("Incorrect recovery key, could not unlock the Emberr Cloud master key", cause)

class InvalidRecoveryWordsException :
    Exception("The recovery words are not 24 valid recovery words")
