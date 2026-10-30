package com.jarvis.assistant.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

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

        /**
         * Dispatches touch gestures on screen for games & app automation
         */
        fun performSwipe(direction: String): Boolean {
            val service = instance ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

            val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getMetrics(metrics)

            val width = metrics.widthPixels.toFloat()
            val height = metrics.heightPixels.toFloat()

            val path = Path()
            when (direction.lowercase()) {
                "up", "forward", "aage" -> {
                    path.moveTo(width / 2f, height * 0.75f)
                    path.lineTo(width / 2f, height * 0.25f)
                }
                "down", "back", "peeche" -> {
                    path.moveTo(width / 2f, height * 0.25f)
                    path.lineTo(width / 2f, height * 0.75f)
                }
                "left", "baye" -> {
                    path.moveTo(width * 0.8f, height / 2f)
                    path.lineTo(width * 0.2f, height / 2f)
                }
                "right", "daye" -> {
                    path.moveTo(width * 0.2f, height / 2f)
                    path.lineTo(width * 0.8f, height / 2f)
                }
                "jump", "tap", "click" -> {
                    path.moveTo(width / 2f, height / 2f)
                    path.lineTo(width / 2f, height / 2f)
                }
                else -> {
                    path.moveTo(width / 2f, height * 0.7f)
                    path.lineTo(width / 2f, height * 0.3f)
                }
            }

            val stroke = GestureDescription.StrokeDescription(path, 0, 300)
            val builder = GestureDescription.Builder().addStroke(stroke)
            return service.dispatchGesture(builder.build(), null, null)
        }

        /**
         * Finds and clicks any button or element on screen by its text
         */
        fun clickNodeByText(targetText: String): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            val nodes = root.findAccessibilityNodeInfosByText(targetText)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    if (node.isClickable) {
                        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    var parent = node.parent
                    while (parent != null) {
                        if (parent.isClickable) {
                            return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        }
                        parent = parent.parent
                    }
                }
            }
            return false
        }
    }
}
