package com.jarvis.assistant.ai

import com.google.gson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiClient(
    private var apiKey: String,
    private var model: String = "gemini-3.8-flash"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    fun updateApiKey(newKey: String) {
        apiKey = newKey
    }

    fun updateModel(newModel: String) {
        model = newModel
    }

    suspend fun sendUserMessage(
        userMessage: String,
        systemInstruction: String,
        chatHistory: List<Pair<String, String>> = emptyList()
    ): GeminiResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResponse(
                text = "Boss, please set your Google Gemini API Key in Settings first.",
                functionCall = null
            )
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val rootJson = JsonObject()

        // System Instruction
        val sysObj = JsonObject()
        val sysParts = JsonArray()
        val sysTextPart = JsonObject()
        sysTextPart.addProperty("text", systemInstruction)
        sysParts.add(sysTextPart)
        sysObj.add("parts", sysParts)
        rootJson.add("system_instruction", sysObj)

        // Contents (History + Latest user message)
        val contentsArray = JsonArray()

        for ((role, text) in chatHistory.takeLast(6)) {
            val historyTurn = JsonObject()
            historyTurn.addProperty("role", if (role == "user") "user" else "model")
            val parts = JsonArray()
            val textPart = JsonObject()
            textPart.addProperty("text", text)
            parts.add(textPart)
            historyTurn.add("parts", parts)
            contentsArray.add(historyTurn)
        }

        val currentTurn = JsonObject()
        currentTurn.addProperty("role", "user")
        val curParts = JsonArray()
        val curTextPart = JsonObject()
        curTextPart.addProperty("text", userMessage)
        curParts.add(curTextPart)
        currentTurn.add("parts", curParts)
        contentsArray.add(currentTurn)

        rootJson.add("contents", contentsArray)

        // Tools Declaration
        rootJson.add("tools", createToolsDeclaration())

        val requestBody = rootJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext GeminiResponse(
                    text = "API error (${response.code}). Please verify your Gemini API key.",
                    functionCall = null
                )
            }

            parseGeminiResponse(responseBody)
        } catch (e: Exception) {
            GeminiResponse(
                text = "Connection error: ${e.localizedMessage ?: "Unknown error"}",
                functionCall = null
            )
        }
    }

    private fun parseGeminiResponse(jsonString: String): GeminiResponse {
        return try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            val candidates = root.getAsJsonArray("candidates")
            if (candidates == null || candidates.size() == 0) {
                return GeminiResponse("No response received from Gemini.", null)
            }

            val firstCandidate = candidates[0].asJsonObject
            val content = firstCandidate.getAsJsonObject("content")
            val parts = content?.getAsJsonArray("parts")

            var replyText: String? = null
            var functionCall: FunctionCall? = null

            if (parts != null) {
                for (partElem in parts) {
                    val partObj = partElem.asJsonObject
                    if (partObj.has("text")) {
                        val t = partObj.get("text").asString
                        replyText = if (replyText == null) t else "$replyText\n$t"
                    }
                    if (partObj.has("functionCall")) {
                        val fnObj = partObj.getAsJsonObject("functionCall")
                        val fnName = fnObj.get("name").asString
                        val argsObj = fnObj.getAsJsonObject("args")
                        val argsMap = mutableMapOf<String, String>()
                        if (argsObj != null) {
                            for (entry in argsObj.entrySet()) {
                                argsMap[entry.key] = entry.value.asString
                            }
                        }
                        functionCall = FunctionCall(name = fnName, args = argsMap)
                    }
                }
            }

            GeminiResponse(text = replyText, functionCall = functionCall)
        } catch (e: Exception) {
            GeminiResponse(text = "Error parsing response: ${e.message}", functionCall = null)
        }
    }

    private fun createToolsDeclaration(): JsonArray {
        val toolsArray = JsonArray()
        val toolObj = JsonObject()
        val funcArray = JsonArray()

        // 1. open_app
        funcArray.add(JsonObject().apply {
            addProperty("name", "open_app")
            addProperty("description", "Opens an installed app or game on the phone, such as Minecraft, ChatGPT, WhatsApp, Camera, etc.")
            add("parameters", JsonObject().apply {
                addProperty("type", "OBJECT")
                add("properties", JsonObject().apply {
                    add("app_name", JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "Name of the app/game to open (e.g. minecraft, chatgpt, settings)")
                    })
                })
                add("required", JsonArray().apply { add("app_name") })
            })
        })

        // 2. play_youtube
        funcArray.add(JsonObject().apply {
            addProperty("name", "play_youtube")
            addProperty("description", "Plays a song, video, or searches for content on YouTube.")
            add("parameters", JsonObject().apply {
                addProperty("type", "OBJECT")
                add("properties", JsonObject().apply {
                    add("query", JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "Search query or title of the song/video")
                    })
                })
                add("required", JsonArray().apply { add("query") })
            })
        })

        // 3. generate_image
        funcArray.add(JsonObject().apply {
            addProperty("name", "generate_image")
            addProperty("description", "Opens ChatGPT or AI image generator with the user prompt to make an image.")
            add("parameters", JsonObject().apply {
                addProperty("type", "OBJECT")
                add("properties", JsonObject().apply {
                    add("prompt", JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "The image generation description")
                    })
                })
                add("required", JsonArray().apply { add("prompt") })
            })
        })

        // 4. remember_fact
        funcArray.add(JsonObject().apply {
            addProperty("name", "remember_fact")
            addProperty("description", "Stores a fact, preference, or habit into persistent long-term memory.")
            add("parameters", JsonObject().apply {
                addProperty("type", "OBJECT")
                add("properties", JsonObject().apply {
                    add("category", JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "Category: PREFERENCE, HABIT, FACT, or PROFILE")
                    })
                    add("key", JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "Key identifier, e.g. favorite_game, music_taste")
                    })
                    add("value", JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "Information to remember")
                    })
                })
                add("required", JsonArray().apply {
                    add("category")
                    add("key")
                    add("value")
                })
            })
        })

        // 5. control_phone
        funcArray.add(JsonObject().apply {
            addProperty("name", "control_phone")
            addProperty("description", "Performs navigation and system actions on the phone.")
            add("parameters", JsonObject().apply {
                addProperty("type", "OBJECT")
                add("properties", JsonObject().apply {
                    add("action", JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "Action to perform: HOME, BACK, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, LOCK")
                    })
                })
                add("required", JsonArray().apply { add("action") })
            })
        })

        toolObj.add("function_declarations", funcArray)
        toolsArray.add(toolObj)
        return toolsArray
    }
}

data class GeminiResponse(
    val text: String?,
    val functionCall: FunctionCall?
)

data class FunctionCall(
    val name: String,
    val args: Map<String, String>
)
