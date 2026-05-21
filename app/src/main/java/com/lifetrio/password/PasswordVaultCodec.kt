package com.lifetrio.password

import java.time.Instant
import java.util.Base64

object PasswordVaultCodec {
    fun encode(records: List<PasswordRecord>): ByteArray {
        val payload = buildString {
            append("life-trio-password-vault-v1\n")
            records.forEach { record ->
                append(
                    listOf(
                        record.id,
                        record.name,
                        record.account,
                        record.secret,
                        record.target,
                        record.note,
                        record.createdAt.toEpochMilli().toString(),
                        record.updatedAt.toEpochMilli().toString()
                    ).joinToString("\t") { it.b64() }
                )
                append('\n')
            }
        }
        return payload.toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): List<PasswordRecord> {
        val payload = bytes.toString(Charsets.UTF_8)
        val lines = payload.lineSequence().toList()
        require(lines.firstOrNull() == "life-trio-password-vault-v1") { "Unsupported password vault format" }
        return lines.drop(1)
            .filter { it.isNotBlank() }
            .map { line ->
                val parts = line.split('\t')
                require(parts.size == 8) { "Corrupted password vault record" }
                PasswordRecord(
                    id = parts[0].unb64(),
                    name = parts[1].unb64(),
                    account = parts[2].unb64(),
                    secret = parts[3].unb64(),
                    target = parts[4].unb64(),
                    note = parts[5].unb64(),
                    createdAt = Instant.ofEpochMilli(parts[6].unb64().toLong()),
                    updatedAt = Instant.ofEpochMilli(parts[7].unb64().toLong())
                )
            }
    }

    private fun String.b64(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(Charsets.UTF_8))

    private fun String.unb64(): String =
        String(Base64.getUrlDecoder().decode(this), Charsets.UTF_8)
}
