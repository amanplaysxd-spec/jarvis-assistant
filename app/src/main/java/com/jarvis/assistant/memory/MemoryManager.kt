package com.jarvis.assistant.memory

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class MemoryManager(context: Context) {
    private val dao = MemoryDatabase.getDatabase(context).memoryDao()

    suspend fun saveFact(category: String, key: String, value: String) = withContext(Dispatchers.IO) {
        val existing = dao.getMemoryByKey(key)
        val entity = if (existing != null) {
            existing.copy(category = category, value = value, timestamp = System.currentTimeMillis())
        } else {
            MemoryEntity(category = category, key = key, value = value)
        }
        dao.insertOrUpdate(entity)
    }

    suspend fun getAllMemories(): List<MemoryEntity> = withContext(Dispatchers.IO) {
        dao.getAllMemories()
    }

    suspend fun deleteMemory(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    suspend fun buildMemoryContext(): String = withContext(Dispatchers.IO) {
        val memories = dao.getAllMemories()
        if (memories.isEmpty()) {
            return@withContext "No stored personal facts yet. You are learning about your Boss as you converse."
        }

        val sb = StringBuilder()
        sb.append("Stored Facts & Learned Preferences about Boss:\n")
        memories.take(25).forEach { mem ->
            sb.append("- [${mem.category}] ${mem.key}: ${mem.value}\n")
        }
        sb.toString()
    }

    /**
     * Inspects user utterances in Hindi, Hinglish, and English to auto-extract facts
     * e.g., "Mera favorite game Minecraft hai", "Yaad rakhna mujhe Arijit ke gaane pasand hain"
     */
    suspend fun autoLearnFromText(userText: String) = withContext(Dispatchers.IO) {
        val lower = userText.lowercase(Locale.ROOT).trim()

        when {
            lower.contains("yaad rakhna") || lower.contains("remember that") -> {
                val cleaned = lower.replace("yaad rakhna", "").replace("remember that", "").replace("ki", "").trim()
                if (cleaned.isNotBlank()) {
                    saveFact("LEARNED", "fact_${System.currentTimeMillis() % 10000}", cleaned)
                }
            }
            lower.contains("mera naam") || lower.contains("my name is") -> {
                val words = userText.split(" ")
                val idx = words.indexOfFirst { it.equals("naam", true) || it.equals("is", true) }
                if (idx != -1 && idx + 1 < words.size) {
                    val name = words.subList(idx + 1, words.size).joinToString(" ").replace("hai", "").trim()
                    saveFact("PROFILE", "user_name", name)
                }
            }
            lower.contains("favorite game") || lower.contains("pasandeeda game") -> {
                if (lower.contains("minecraft")) {
                    saveFact("PREFERENCE", "favorite_game", "Minecraft")
                }
            }
            lower.contains("favorite singer") || lower.contains("gaana pasand hai") -> {
                saveFact("PREFERENCE", "music_taste", userText)
            }
        }
    }
}
