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
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
                "Opening YouTube for '$query'."
            } catch (err: Exception) {
                "Unable to open YouTube at this moment."
            }
        }
    }

    /**
     * Opens YouTube Video Upload screen pre-populating title and description
     */
    fun openUploadScreen(title: String, description: String): String {
        val pm = context.packageManager
        // 1. Try launching YouTube Creator Studio or YouTube App upload intent
        val uploadIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            setPackage("com.google.android.youtube")
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, description)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(uploadIntent)
            "Opening YouTube upload for your video with title '$title', Boss."
        } catch (e: Exception) {
            // Fallback: Open YouTube Studio web upload
            val studioIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://studio.youtube.com/channel/upload")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(studioIntent)
                "Opening YouTube Studio web upload for '$title'."
            } catch (err: Exception) {
                "Opening YouTube app now, Boss."
            }
        }
    }
}
