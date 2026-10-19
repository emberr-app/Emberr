package com.emberr.domain.cloud.account

interface CloudAccountStorage {
    suspend fun saveSessionToken(sessionToken: String)
    suspend fun getSessionToken(): String?
    suspend fun saveMasterKey(masterKey: ByteArray)
    suspend fun getMasterKey(): ByteArray?
    suspend fun savePrivateKey(privateKey: ByteArray)
    suspend fun getPrivateKey(): ByteArray?
    suspend fun saveEmail(email: String)
    suspend fun getEmail(): String?
    suspend fun clearAll()
}
