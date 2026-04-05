package com.antigravity.pampastarshooter.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.antigravity.pampastarshooter.data.local.entity.RunHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunHistoryDao {
    @Query("SELECT * FROM run_history ORDER BY startedAtEpochMs DESC LIMIT 40")
    fun observeRecent(): Flow<List<RunHistoryEntity>>

    @Upsert
    suspend fun upsert(entry: RunHistoryEntity)
}
