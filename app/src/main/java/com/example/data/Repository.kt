package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {
    val allProfiles: Flow<List<SniProfile>> = db.sniProfileDao().getAllProfiles()
    val allHistory: Flow<List<HandshakeHistory>> = db.handshakeHistoryDao().getAllHistory()

    suspend fun insertProfile(profile: SniProfile) {
        db.sniProfileDao().insertProfile(profile)
    }

    suspend fun deleteProfile(profile: SniProfile) {
        db.sniProfileDao().deleteProfile(profile)
    }

    suspend fun updateProfile(profile: SniProfile) {
        db.sniProfileDao().updateProfile(profile)
    }

    suspend fun insertHistory(history: HandshakeHistory) {
        db.handshakeHistoryDao().insertHistory(history)
    }

    suspend fun clearHistory() {
        db.handshakeHistoryDao().clearHistory()
    }
}
