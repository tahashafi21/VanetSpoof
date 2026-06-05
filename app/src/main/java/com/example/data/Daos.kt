package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SniProfileDao {
    @Query("SELECT * FROM sni_profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<SniProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SniProfile)

    @Delete
    suspend fun deleteProfile(profile: SniProfile)

    @Update
    suspend fun updateProfile(profile: SniProfile)
}

@Dao
interface HandshakeHistoryDao {
    @Query("SELECT * FROM handshake_histories ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HandshakeHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HandshakeHistory)

    @Query("DELETE FROM handshake_histories")
    suspend fun clearHistory()
}
