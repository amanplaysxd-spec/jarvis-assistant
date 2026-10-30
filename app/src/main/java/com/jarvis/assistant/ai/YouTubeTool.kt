package com.jarvis.assistant.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

class YouTubeTool(private val context: Context) {

    fun playOrSearch(query: String): String {
        val encodedQuery = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            query
        }

        // Try launching official YouTube app
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://results?search_query=$encodedQuery")).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(appIntent)
            "Playing '$query' on YouTube for you, Boss."
        } catch (e: Exception) {
            // Fallback to web browser
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
                "Opening YouTube in browser for '$query'."
            } catch (err: Exception) {
                "Unable to open YouTube at this moment."
            }
        }
    }
}
