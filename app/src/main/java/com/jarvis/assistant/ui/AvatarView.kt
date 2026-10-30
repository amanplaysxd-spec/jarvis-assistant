package com.jarvis.assistant.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.jarvis.assistant.R

class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val imageView: ImageView
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
    }

    private var rotationAngle = 0f
    private var pulseScale = 1.0f
    private var currentState = State.IDLE

    enum class State {
        IDLE, LISTENING, THINKING, SPEAKING
    }

    init {
        setWillNotDraw(false)

        imageView = ImageView(context).apply {
            setImageResource(R.drawable.avatar_anime)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                val pad = 36
                setMargins(pad, pad, pad, pad)
            }
        }
        addView(imageView)

        startAnimations()
    }

    private fun startAnimations() {
        // Continuous HUD ring rotation
        ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 10000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                rotationAngle = it.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Gentle breathing animation for the character
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 0.97f, 1.03f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 0.97f, 1.03f)
        ObjectAnimator.ofPropertyValuesHolder(imageView, scaleX, scaleY).apply {
            duration = 2400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    fun setState(state: State) {
        currentState = state
        when (state) {
            State.IDLE -> {
                imageView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start()
            }
            State.LISTENING -> {
                imageView.animate().scaleX(1.06f).scaleY(1.06f).setDuration(250).start()
            }
            State.THINKING -> {
                imageView.animate().scaleX(0.98f).scaleY(0.98f).setDuration(250).start()
            }
            State.SPEAKING -> {
                imageView.animate().scaleX(1.08f).scaleY(1.08f).setDuration(200).start()
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (Math.min(width, height) / 2f) - 18f
        if (radius <= 0) return

        val colorHex = when (currentState) {
            State.IDLE -> "#00E5FF"
            State.LISTENING -> "#00E5FF"
            State.THINKING -> "#FFAB00"
            State.SPEAKING -> "#FF1744"
        }
        val mainColor = Color.parseColor(colorHex)

        // Outer glow ring
        glowPaint.color = Color.argb(45, Color.red(mainColor), Color.green(mainColor), Color.blue(mainColor))
        canvas.drawCircle(cx, cy, radius, glowPaint)

        // Segmented HUD arc ring
        ringPaint.color = mainColor
        canvas.save()
        canvas.rotate(rotationAngle, cx, cy)

        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val segmentCount = 6
        val sweepAngle = 360f / segmentCount
        for (i in 0 until segmentCount) {
            val startAngle = i * sweepAngle + 8f
            canvas.drawArc(rect, startAngle, sweepAngle - 16f, false, ringPaint)
        }
        canvas.restore()
    }
}
