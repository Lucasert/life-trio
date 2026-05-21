package com.lifetrio.password

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class PasswordVaultTest {
    @Test
    fun codecRoundTripsPasswordRecords() {
        val records = listOf(
            PasswordRecord(name = "邮箱", account = "me@example.com", secret = "S3cret!", target = "mail")
        )

        val decoded = PasswordVaultCodec.decode(PasswordVaultCodec.encode(records))

        assertEquals(records.first().name, decoded.first().name)
        assertEquals(records.first().account, decoded.first().account)
        assertEquals(records.first().secret, decoded.first().secret)
    }

    @Test
    fun encryptedVaultDoesNotContainPlainTextAndRejectsTampering() {
        val crypto = TestPasswordVaultCrypto()
        val encrypted = crypto.encrypt(PasswordVaultCodec.encode(listOf(
            PasswordRecord(name = "github", account = "lucas", secret = "plain-secret", target = "github.com")
        )))

        assertFalse(encrypted.toString(Charsets.UTF_8).contains("plain-secret"))
        val decrypted = PasswordVaultCodec.decode(crypto.decrypt(encrypted))
        assertEquals("plain-secret", decrypted.first().secret)

        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertTrue(runCatching { crypto.decrypt(encrypted) }.isFailure)
    }

    @Test
    fun repositorySavesUpdatesAndDeletesRecords() = runTest {
        val file = Files.createTempFile("life-trio-password-test", ".bin").toFile()
        file.delete()
        val repository = PasswordVaultRepository(file, TestPasswordVaultCrypto())
        repository.unlock()

        val record = PasswordRecord(name = "Github", account = "lucas", secret = "one", target = "github.com")
        repository.save(record)
        assertEquals(1, repository.records.value.size)
        assertFalse(file.readBytes().toString(Charsets.UTF_8).contains("Github"))

        repository.save(record.copy(secret = "two"))
        assertEquals("two", repository.records.value.first().secret)

        repository.delete(record.id)
        assertTrue(repository.records.value.isEmpty())
        file.delete()
    }

    private class TestPasswordVaultCrypto : PasswordVaultCrypto {
        private val key: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        private val random = SecureRandom()

        override fun encrypt(plainText: ByteArray): ByteArray {
            val iv = ByteArray(12).also(random::nextBytes)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            return iv + cipher.doFinal(plainText)
        }

        override fun decrypt(encrypted: ByteArray): ByteArray {
            val iv = encrypted.copyOfRange(0, 12)
            val cipherText = encrypted.copyOfRange(12, encrypted.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            return cipher.doFinal(cipherText)
        }
    }
}
