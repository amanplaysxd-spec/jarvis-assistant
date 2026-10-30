package com.jarvis.assistant.ai

import com.google.gson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class GeminiClient(
    private var apiKey: String,
    private var model: String = "gemini-1.5-flash"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    fun updateApiKey(newKey: String) {
        apiKey = newKey.trim().replace("\"", "").replace("'", "")
    }

    fun updateModel(newModel: String) {
        model = newModel
    }

    suspend fun sendUserMessage(
        userMessage: String,
        systemInstruction: String,
        chatHistory: List<Pair<String, String>> = emptyList()
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim().replace("\"", "").replace("'", "")
        if (cleanKey.isBlank()) {
            return@withContext GeminiResponse(
                text = "Boss, please set your Google Gemini API Key in Settings (gear icon) first.",
                functionCall = null
            )
        }

        // Resolving model name: 1.5-flash is universal and stable across all Google AI Studio accounts
        val targetModel = when (model) {
            "gemini-3.8-flash", "gemini-2.0-flash" -> "gemini-1.5-flash"
            else -> model
        }

        val requestBody = buildRequestBody(userMessage, systemInstruction, chatHistory)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$cleanKey"

        // Execute with automatic retry and 404/timeout recovery
        try {
            executeRequest(url, requestBody, cleanKey, targetModel)
        } catch (e: SocketTimeoutException) {
            // Automatic retry once on timeout (helps on 5G network latency spikes)
            try {
                executeRequest(url, requestBody, cleanKey, targetModel)
            } catch (retryErr: Exception) {
                GeminiResponse(
                    text = "Network connection is slow, Boss. Please try again or check your mobile data/Wi-Fi.",
                    functionCall = null
                )
            }
        } catch (e: Exception) {
            GeminiResponse(
                text = "Connection error: ${e.localizedMessage ?: "Unknown error"}",
                functionCall = null
            )
        }
    }

    private fun executeRequest(
        url: String,
        requestBody: String,
        cleanKey: String,
        targetModel: String
    ): GeminiResponse {
        val request = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody(mediaType))
            .build()

        var response = client.newCall(request).execute()
        var responseBody = response.body?.string()

        // 404 Fallback: If target model returned 404, fallback to gemini-1.5-flash immediately
        if (response.code == 404 && targetModel != "gemini-1.5-flash") {
            val fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$cleanKey"
            val fallbackReq = Request.Builder()
                .url(fallbackUrl)
                .post(requestBody.toRequestBody(mediaType))
                .build()
            response = client.newCall(fallbackReq).execute()
            responseBody = response.body?.string()
        }

        if (!response.isSuccessful || responseBody == null) {
            val errorMsg = when (response.code) {
                400 -> "API Key is invalid or restricted. Please verify your key in Settings."
                403 -> "API Key lacks permissions. Please generate a new key on Google AI Studio."
                404 -> "Model unavailable. Jarvis will auto-switch models on next query."
                429 -> "Rate limit reached. Please wait a few moments, Boss."
                else -> "API response error (${response.code})."
            }
            return GeminiResponse(text = errorMsg, functionCall = null)
        }

        return parseGeminiResponse(responseBody)
    }

    private fun buildRequestBody(
        userMessage: String,
        systemInstruction: String,
        chatHistory: List<Pair<String, String>>
    ): String {
        val rootJson = JsonObject()

        // System Instruction
        val sysObj = JsonObject()
        val sysParts = JsonArray()
        val sysTextPart = JsonObject()
        sysTextPart.addProperty("text", systemInstruction)
        sysParts.add(sysTextPart)
        sysObj.add("parts", sysParts)
        rootJson.add("system_instruction", sysObj)

        // Conversation history
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

        // Tools / Function declarations
        rootJson.add("tools", createToolsDeclaration())

        return rootJson.toString()
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
            addProperty("description", "Opens an installed app or game on the phone, such as Minecraft, Free Fire, ChatGPT, YouTube, Camera, WhatsApp, etc.")
            add("parameters", JsonObject().apply {
                addProperty("type", "OBJECT")
                add("properties", JsonObject().apply {
                    add("app_name", JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "Name of the app/game to open (e.g. minecraft, free fire, chatgpt, settings)")
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

        // 3. upload_youtube_video
        funcArray.add(JsonObject().apply {
            addProperty("name", "upload_youtube_video")
            addProperty("description", "Opens YouTube upload screen to publish a video with title and description.")
            add("parameters", JsonObject().apply {
                addProperty("type", "OBJECT")
                add("properties", JsonObject().apply {
                    add("title", JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "Video title")
                    })
                    add("description", JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "Video description")
                    })
                })
                add("required", JsonArray().apply { add("title") })
            })
        })

        // 4. perform_gesture
        funcArray.add(JsonObject().apply {
            addProperty("name", "perform_gesture")
            addProperty("description", "Performs directional movement, swipe, or tap gesture on screen to control games or scroll.")
            add("parameters", JsonObject().apply {
                addProperty("type", "OBJECT")
                add("properties", JsonObject().apply {
                    add("direction", JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "Direction: up, down, left, right, jump, tap, click")
                    })
                })
                add("required", JsonArray().apply { add("direction") })
            })
        })

        // 5. click_screen_text
        funcArray.add(JsonObject().apply {
            addProperty("name", "click_screen_text")
            addProperty("description", "Finds and clicks any button or element on the current screen by its visible text.")
            add("parameters", JsonObject().apply {
                addProperty("type", "OBJECT")
                add("properties", JsonObject().apply {
                    add("text", JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "The button or label text to click on")
                    })
                })
                add("required", JsonArray().apply { add("text") })
            })
        })

        // 6. generate_image
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

        // 7. remember_fact
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

        // 8. control_phone
        funcArray.add(JsonObject().apply {
            addProperty("name", "control_phone")
            addProperty("description", "Performs navigation and system actions on the phone: HOME, BACK, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, LOCK.")
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

        // 9. open_floating_bubble
        funcArray.add(JsonObject().apply {
            addProperty("name", "open_floating_bubble")
            addProperty("description", "Opens or closes the draggable floating bubble companion on screen.")
            add("parameters", JsonObject().apply {
                addProperty("type", "OBJECT")
                add("properties", JsonObject().apply {
                    add("state", JsonObject().apply {
                        addProperty("type", "STRING")
                        addProperty("description", "start or stop")
                    })
                })
                add("required", JsonArray().apply { add("state") })
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
