package com.jarvis.assistant.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

class ImageGenTool(private val context: Context) {

    fun generateImage(prompt: String): String {
        val pm = context.packageManager
        val encodedPrompt = try {
            URLEncoder.encode(prompt, "UTF-8")
        } catch (e: Exception) {
            prompt
        }

        // Try ChatGPT app
        val chatGptIntent = pm.getLaunchIntentForPackage("com.openai.chatgpt")
        if (chatGptIntent != null) {
            chatGptIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            chatGptIntent.putExtra(Intent.EXTRA_TEXT, "Create an image: $prompt")
            try {
                context.startActivity(chatGptIntent)
                return "Opening ChatGPT to generate your image: '$prompt', Boss."
            } catch (e: Exception) {
                // Ignore and fallback
            }
        }

        // Fallback: Web AI generator
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.bing.com/images/create?q=$encodedPrompt")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(webIntent)
            "Opening AI Image Creator for '$prompt', Boss."
        } catch (e: Exception) {
            "Unable to open image generator."
        }
    }
}
