package com.jarvis.assistant.ai

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import java.util.Locale

class AppLauncherTool(private val context: Context) {

    // Common package name shortcuts
    private val knownPackages = mapOf(
        "minecraft" to listOf("com.mojang.minecraftpe", "com.mojang.minecraft"),
        "chatgpt" to listOf("com.openai.chatgpt"),
        "youtube" to listOf("com.google.android.youtube"),
        "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "chrome" to listOf("com.android.chrome"),
        "camera" to listOf("com.android.camera", "com.google.android.GoogleCamera"),
        "settings" to listOf("com.android.settings"),
        "clock" to listOf("com.google.android.deskclock", "com.android.deskclock"),
        "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
        "instagram" to listOf("com.instagram.android"),
        "spotify" to listOf("com.spotify.music"),
        "free fire" to listOf("com.dts.freefireth", "com.dts.freefiremax")
    )

    fun openApp(appName: String): AppLaunchResult {
        val pm = context.packageManager
        val query = appName.lowercase(Locale.ROOT).trim()

        // 1. Check known aliases
        for ((key, packages) in knownPackages) {
            if (query.contains(key)) {
                for (pkg in packages) {
                    val intent = pm.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return AppLaunchResult(true, "Opening $key, Boss.")
                    }
                }
            }
        }

        // 2. Search all installed launchable apps
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = pm.queryIntentActivities(mainIntent, 0)
        for (info in resolveInfoList) {
            val label = info.loadLabel(pm).toString().lowercase(Locale.ROOT)
            val pkg = info.activityInfo.packageName.lowercase(Locale.ROOT)
            if (label.contains(query) || pkg.contains(query)) {
                val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return AppLaunchResult(true, "Launching ${info.loadLabel(pm)} now.")
                }
            }
        }

        // 3. Fallback: Search Play Store
        try {
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$query")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
            return AppLaunchResult(false, "$appName is not installed. Opening Google Play Store to install it.")
        } catch (e: Exception) {
            return AppLaunchResult(false, "Could not find or launch $appName.")
        }
    }
}

data class AppLaunchResult(val success: Boolean, val message: String)
