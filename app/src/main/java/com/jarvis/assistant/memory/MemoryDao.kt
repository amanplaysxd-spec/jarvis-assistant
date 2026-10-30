package com.jarvis.assistant.memory

import androidx.room.*

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    suspend fun getAllMemories(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE `key` = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE `value` LIKE '%' || :query || '%' OR `key` LIKE '%' || :query || '%'")
    suspend fun searchMemories(query: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(memory: MemoryEntity): Long

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}
