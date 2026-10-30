package com.jarvis.assistant.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class VisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }

    private var rmsLevel: Float = 0f
    private var isSpeaking: Boolean = false
    private var isListening: Boolean = false
    private var phase: Float = 0f
    private val barCount = 28

    fun setRms(rms: Float) {
        rmsLevel = (rms.coerceIn(0f, 15f)) / 15f
        invalidate()
    }

    fun setState(listening: Boolean, speaking: Boolean) {
        isListening = listening
        isSpeaking = speaking
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0 || height <= 0) return

        val centerY = height / 2f
        val step = width / (barCount + 1)
        val barWidth = step * 0.5f
        paint.strokeWidth = barWidth

        phase += 0.15f

        for (i in 0 until barCount) {
            val x = (i + 1) * step
            val normalizedX = (i.toFloat() / barCount) * Math.PI.toFloat()
            val baseWave = sin(normalizedX + phase)

            val barHeight = when {
                isListening -> {
                    paint.color = Color.parseColor("#00E5FF") // Neon Cyan
                    (20f + (rmsLevel * height * 0.75f) * (0.4f + 0.6f * Math.abs(baseWave))).coerceIn(8f, height * 0.9f)
                }
                isSpeaking -> {
                    paint.color = Color.parseColor("#FF1744") // Neon Red
                    val wave = Math.abs(sin(normalizedX * 2 + phase))
                    (15f + wave * height * 0.65f).coerceIn(10f, height * 0.85f)
                }
                else -> {
                    paint.color = Color.parseColor("#3300E5FF") // Idle soft glow
                    (8f + 6f * sin(normalizedX + phase)).coerceAtLeast(6f)
                }
            }

            canvas.drawLine(x, centerY - barHeight / 2f, x, centerY + barHeight / 2f, paint)
        }

        if (isSpeaking || isListening) {
            postInvalidateDelayed(35)
        }
    }
}
