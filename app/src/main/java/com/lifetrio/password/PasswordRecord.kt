package com.lifetrio.password

import java.time.Instant
import java.util.UUID

data class PasswordRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val account: String,
    val secret: String,
    val target: String,
    val note: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun matches(query: String): Boolean {
        val normalized = query.trim()
        if (normalized.isBlank()) return true
        return name.contains(normalized, ignoreCase = true) ||
            account.contains(normalized, ignoreCase = true) ||
            target.contains(normalized, ignoreCase = true)
    }
}
