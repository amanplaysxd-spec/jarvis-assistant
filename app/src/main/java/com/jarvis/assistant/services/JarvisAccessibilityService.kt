package com.jarvis.assistant.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can inspect active window content or user interaction
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        @Volatile
        var instance: JarvisAccessibilityService? = null

        fun isServiceEnabled(context: Context): Boolean {
            val prefString = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return prefString.contains(context.packageName)
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        fun executeAction(action: String): Boolean {
            val service = instance ?: return false
            return when (action.uppercase()) {
                "HOME" -> service.performGlobalAction(GLOBAL_ACTION_HOME)
                "BACK" -> service.performGlobalAction(GLOBAL_ACTION_BACK)
                "RECENTS" -> service.performGlobalAction(GLOBAL_ACTION_RECENTS)
                "NOTIFICATIONS" -> service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                "QUICK_SETTINGS" -> service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                "LOCK" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    } else false
                }
                else -> false
            }
        }
    }
}
