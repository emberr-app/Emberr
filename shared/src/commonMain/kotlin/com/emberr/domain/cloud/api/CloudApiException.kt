package com.emberr.domain.cloud.api

enum class CloudApiErrorCode {
    BAD_REQUEST,
    REQUEST_TOO_LARGE,
    TOO_MANY_REQUESTS,
    INTERNAL_ERROR,
    INVALID_PASSWORD,
    INCORRECT_CODE,
    EXPIRED_CODE,
    INVALID_TOKEN,
    UPGRADE_REQUIRED,
    NOT_FOUND,
    EMAIL_ALREADY_USED,
    CONFLICT,
    UNKNOWN
}

class CloudApiException(val code: CloudApiErrorCode, val httpStatus: Int) :
    Exception("Emberr Cloud refused the request: $code (HTTP $httpStatus)")

class CloudServerUnreachableException :
    Exception("Could not reach the Emberr Cloud server")
