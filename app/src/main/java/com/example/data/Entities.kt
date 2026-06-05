package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sni_profiles")
data class SniProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetHost: String,
    val targetPort: Int = 443,
    val spoofedSni: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "handshake_histories")
data class HandshakeHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val targetHost: String,
    val targetPort: Int,
    val spoofedSni: String,
    val isSuccess: Boolean,
    val latencyMs: Long,
    val errorMessage: String? = null,
    val certIssuer: String? = null,
    val certSubject: String? = null,
    val cipherSuite: String? = null,
    val protocol: String? = null
)
