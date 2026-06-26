package com.lifetrio.password

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Instant

class PasswordVaultRepository(
    private val vaultFile: File,
    private val crypto: PasswordVaultCrypto
) {
    private val mutex = Mutex()
    private val _records = MutableStateFlow<List<PasswordRecord>>(emptyList())
    private val _isUnlocked = MutableStateFlow(false)
    val records: StateFlow<List<PasswordRecord>> = _records.asStateFlow()
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    suspend fun unlock() {
        mutex.withLock {
            _records.value = readRecords()
            _isUnlocked.value = true
        }
    }

    fun lock() {
        _records.value = emptyList()
        _isUnlocked.value = false
    }

    suspend fun save(record: PasswordRecord) {
        mutex.withLock {
            check(_isUnlocked.value) { "Password vault is locked" }
            val now = Instant.now()
            val current = _records.value
            val next = if (current.any { it.id == record.id }) {
                current.map {
                    if (it.id == record.id) {
                        record.copy(createdAt = it.createdAt, updatedAt = now)
                    } else {
                        it
                    }
                }
            } else {
                current + record.copy(createdAt = now, updatedAt = now)
            }.sortedByDescending { it.updatedAt }
            writeRecords(next)
            _records.value = next
        }
    }

    suspend fun delete(id: String) {
        mutex.withLock {
            check(_isUnlocked.value) { "Password vault is locked" }
            val next = _records.value.filterNot { it.id == id }
            writeRecords(next)
            _records.value = next
        }
    }

    private fun readRecords(): List<PasswordRecord> {
        if (!vaultFile.exists()) return emptyList()
        return PasswordVaultCodec.decode(crypto.decrypt(vaultFile.readBytes()))
            .sortedByDescending { it.updatedAt }
    }

    private fun writeRecords(records: List<PasswordRecord>) {
        vaultFile.parentFile?.mkdirs()
        vaultFile.writeBytes(crypto.encrypt(PasswordVaultCodec.encode(records)))
    }

    // ── Import / Export helpers ──

    /** Returns the raw encrypted vault file bytes, or null if the vault file doesn't exist. */
    fun exportRawBytes(): ByteArray? {
        if (!vaultFile.exists()) return null
        return vaultFile.readBytes()
    }

    /** Writes raw encrypted vault file bytes (imported from another device).
     *  NOTE: Only works if the Android Keystore key on this device matches the one
     *  used to encrypt the vault (typically the same device). */
    fun importRawBytes(bytes: ByteArray) {
        vaultFile.parentFile?.mkdirs()
        vaultFile.writeBytes(bytes)
    }
}
