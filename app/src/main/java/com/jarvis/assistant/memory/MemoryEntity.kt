package com.jarvis.assistant.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // PREFERENCE, HABIT, FACT, PROFILE, APP
    val key: String,       // e.g., "favorite_game", "user_nickname"
    val value: String,     // e.g., "Minecraft", "Sir"
    val timestamp: Long = System.currentTimeMillis()
)
