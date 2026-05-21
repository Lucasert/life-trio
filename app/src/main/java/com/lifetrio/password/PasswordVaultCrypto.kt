package com.lifetrio.password

interface PasswordVaultCrypto {
    fun encrypt(plainText: ByteArray): ByteArray
    fun decrypt(encrypted: ByteArray): ByteArray
}
