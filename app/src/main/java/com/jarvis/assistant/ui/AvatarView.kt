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
        strokeWidth = 14f
    }

    private var rotationAngle = 0f
    private var reverseRotationAngle = 360f
    private var currentState = State.IDLE
    private var isUsingChannelLogo = false

    enum class State {
        IDLE, LISTENING, THINKING, SPEAKING
    }

    init {
        setWillNotDraw(false)
        cameraDistance = 8000f

        imageView = ImageView(context).apply {
            setImageResource(R.drawable.avatar_anime)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                val pad = 36
                setMargins(pad, pad, pad, pad)
            }
        }
        addView(imageView)

        // Tap avatar to toggle between Anime Boy and Channel Logo
        setOnClickListener {
            toggleAvatarImage()
        }

        startAnimations()
    }

    private fun toggleAvatarImage() {
        isUsingChannelLogo = !isUsingChannelLogo
        val targetRes = if (isUsingChannelLogo) R.drawable.channel_logo else R.drawable.avatar_anime
        imageView.animate().alpha(0f).scaleX(0.7f).scaleY(0.7f).setDuration(200).withEndAction {
            imageView.setImageResource(targetRes)
            imageView.animate().alpha(1f).scaleX(1.0f).scaleY(1.0f).setDuration(250).start()
        }.start()
    }

    private fun startAnimations() {
        // Continuous HUD outer ring rotation
        ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 8000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                rotationAngle = it.animatedValue as Float
                reverseRotationAngle = 360f - rotationAngle
                invalidate()
            }
            start()
        }

        // 3D Parallax Depth Tilt & Breathing animation
        val tiltY = PropertyValuesHolder.ofFloat("rotationY", -8f, 8f)
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 0.96f, 1.04f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 0.96f, 1.04f)
        ObjectAnimator.ofPropertyValuesHolder(imageView, tiltY, scaleX, scaleY).apply {
            duration = 2600
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
                imageView.animate().scaleX(1.0f).scaleY(1.0f).translationY(0f).setDuration(300).start()
            }
            State.LISTENING -> {
                imageView.animate().scaleX(1.08f).scaleY(1.08f).translationY(-8f).setDuration(200).start()
            }
            State.THINKING -> {
                imageView.animate().scaleX(0.96f).scaleY(0.96f).translationY(4f).setDuration(200).start()
            }
            State.SPEAKING -> {
                // 3D Talking bounce animation
                imageView.animate().scaleX(1.12f).scaleY(1.12f).translationY(-12f).setDuration(160).start()
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

        // Outer glow
        glowPaint.color = Color.argb(45, Color.red(mainColor), Color.green(mainColor), Color.blue(mainColor))
        canvas.drawCircle(cx, cy, radius, glowPaint)

        // Outer rotating HUD segments
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

        // Inner counter-rotating ring
        val innerRadius = radius - 16f
        if (innerRadius > 0) {
            canvas.save()
            canvas.rotate(reverseRotationAngle, cx, cy)
            val innerRect = RectF(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius)
            val innerSegments = 4
            val innerSweep = 360f / innerSegments
            ringPaint.color = Color.argb(120, Color.red(mainColor), Color.green(mainColor), Color.blue(mainColor))
            for (i in 0 until innerSegments) {
                val start = i * innerSweep + 12f
                canvas.drawArc(innerRect, start, innerSweep - 24f, false, ringPaint)
            }
            canvas.restore()
        }
    }
}
