package com.sixblock.app.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

class AnimatedSkylineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var progress = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 6200L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    private val buildings = listOf(
        Building(0.05f, 0.50f, 0.10f, 0.78f, 4, 5),
        Building(0.13f, 0.58f, 0.18f, 0.78f, 3, 4),
        Building(0.21f, 0.48f, 0.28f, 0.78f, 5, 5),
        Building(0.31f, 0.55f, 0.37f, 0.78f, 4, 4),
        Building(0.63f, 0.61f, 0.72f, 0.78f, 4, 3),
        Building(0.76f, 0.52f, 0.82f, 0.78f, 3, 5),
        Building(0.86f, 0.49f, 0.93f, 0.78f, 4, 5)
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        drawSky(canvas, w, h)
        drawMoonAndStars(canvas, w, h)
        drawDistantGlow(canvas, w, h)
        drawBuildings(canvas, w, h)
        drawCnTower(canvas, w, h)
        drawLake(canvas, w, h)
    }

    private fun drawSky(canvas: Canvas, w: Float, h: Float) {
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(Color.rgb(53, 84, 111), Color.rgb(19, 27, 34), Color.rgb(10, 12, 13)),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(RectF(0f, 0f, w, h), h * 0.08f, h * 0.08f, paint)
        paint.shader = null
    }

    private fun drawMoonAndStars(canvas: Canvas, w: Float, h: Float) {
        paint.color = Color.argb(38, 255, 255, 210)
        canvas.drawCircle(w * 0.78f, h * 0.14f, h * 0.055f, paint)
        paint.color = Color.rgb(17, 21, 24)
        canvas.drawCircle(w * 0.81f, h * 0.12f, h * 0.052f, paint)

        val stars = arrayOf(
            0.12f to 0.10f,
            0.26f to 0.08f,
            0.38f to 0.13f,
            0.64f to 0.09f,
            0.91f to 0.17f
        )
        stars.forEachIndexed { index, star ->
            val pulse = 0.45f + 0.35f * sin((progress + index * 0.17f) * Math.PI * 2).toFloat()
            paint.color = Color.argb((80 + pulse * 120).toInt(), 214, 230, 238)
            canvas.drawCircle(star.first * w, star.second * h, h * 0.006f, paint)
        }
    }

    private fun drawDistantGlow(canvas: Canvas, w: Float, h: Float) {
        paint.shader = RadialGradient(
            w * 0.53f,
            h * 0.74f,
            w * 0.35f,
            Color.argb(62, 198, 215, 222),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(RectF(w * 0.12f, h * 0.50f, w * 0.88f, h * 0.92f), paint)
        paint.shader = null
    }

    private fun drawBuildings(canvas: Canvas, w: Float, h: Float) {
        paint.color = Color.rgb(18, 21, 22)
        buildings.forEachIndexed { index, building ->
            val left = building.left * w
            val top = building.top * h
            val right = building.right * w
            val bottom = building.bottom * h
            canvas.drawRect(left, top, right, bottom, paint)

            paint.color = Color.rgb(24, 28, 29)
            canvas.drawRect(left + 2f, top + 2f, right - 2f, bottom, paint)
            drawWindows(canvas, left, top, right, bottom, building.columns, building.rows, index)
            paint.color = Color.rgb(18, 21, 22)
        }

        paint.color = Color.rgb(16, 19, 20)
        canvas.drawOval(RectF(w * 0.48f, h * 0.66f, w * 0.62f, h * 0.77f), paint)
    }

    private fun drawWindows(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        columns: Int,
        rows: Int,
        seed: Int
    ) {
        val cellW = (right - left) / (columns + 1)
        val cellH = (bottom - top) / (rows + 1)
        for (row in 1..rows) {
            for (column in 1..columns) {
                if ((row + column + seed) % 3 == 0) continue
                val flicker = 0.65f + 0.25f * sin((progress + row * 0.08f + column * 0.11f + seed) * Math.PI * 2).toFloat()
                paint.color = Color.argb((110 + flicker * 95).toInt(), 230, 176, 74)
                val cx = left + column * cellW
                val cy = top + row * cellH
                canvas.drawRect(cx - cellW * 0.15f, cy - cellH * 0.10f, cx + cellW * 0.14f, cy + cellH * 0.12f, paint)
            }
        }
    }

    private fun drawCnTower(canvas: Canvas, w: Float, h: Float) {
        val x = w * 0.52f
        val top = h * 0.15f
        val base = h * 0.80f

        paint.color = Color.rgb(25, 28, 29)
        paint.strokeWidth = w * 0.012f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(x, top, x, base, paint)

        paint.color = Color.rgb(55, 61, 62)
        paint.strokeWidth = w * 0.004f
        canvas.drawLine(x - w * 0.018f, h * 0.40f, x - w * 0.035f, base, paint)
        canvas.drawLine(x + w * 0.018f, h * 0.40f, x + w * 0.035f, base, paint)

        paint.color = Color.rgb(198, 215, 222)
        canvas.drawRoundRect(RectF(x - w * 0.045f, h * 0.36f, x + w * 0.045f, h * 0.405f), w * 0.02f, w * 0.02f, paint)
        paint.color = Color.rgb(35, 39, 39)
        canvas.drawRoundRect(RectF(x - w * 0.034f, h * 0.385f, x + w * 0.034f, h * 0.430f), w * 0.015f, w * 0.015f, paint)

        paint.color = Color.rgb(220, 77, 70)
        paint.strokeWidth = h * 0.010f
        canvas.drawLine(x - w * 0.038f, h * 0.375f, x + w * 0.038f, h * 0.375f, paint)
        paint.color = Color.rgb(229, 230, 214)
        paint.strokeWidth = h * 0.008f
        canvas.drawLine(x - w * 0.030f, h * 0.405f, x + w * 0.030f, h * 0.405f, paint)

        paint.color = Color.rgb(198, 215, 222)
        path.reset()
        path.moveTo(x, top - h * 0.04f)
        path.lineTo(x - w * 0.011f, h * 0.36f)
        path.lineTo(x + w * 0.011f, h * 0.36f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawLake(canvas: Canvas, w: Float, h: Float) {
        val lakeTop = h * 0.79f
        paint.shader = LinearGradient(
            0f,
            lakeTop,
            0f,
            h,
            Color.rgb(10, 16, 18),
            Color.rgb(32, 51, 58),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, lakeTop, w, h, paint)
        paint.shader = null

        val shimmer = sin(progress * Math.PI * 2).toFloat() * w * 0.025f
        val reflectionColors = intArrayOf(
            Color.argb(120, 198, 215, 222),
            Color.argb(100, 230, 77, 70),
            Color.argb(90, 230, 230, 210)
        )
        val xs = floatArrayOf(w * 0.28f, w * 0.52f, w * 0.72f)
        reflectionColors.forEachIndexed { index, color ->
            paint.color = color
            paint.strokeWidth = h * 0.010f
            paint.strokeCap = Paint.Cap.ROUND
            val y = lakeTop + h * (0.045f + index * 0.045f)
            canvas.drawLine(xs[index] - w * 0.055f + shimmer, y, xs[index] + w * 0.052f + shimmer, y, paint)
            paint.color = color.withAlpha(62)
            canvas.drawLine(xs[index] - w * 0.030f - shimmer, y + h * 0.045f, xs[index] + w * 0.034f - shimmer, y + h * 0.045f, paint)
        }

        paint.color = Color.argb(58, 255, 255, 255)
        paint.strokeWidth = h * 0.006f
        canvas.drawLine(w * 0.06f, lakeTop + h * 0.025f, w * 0.42f, lakeTop + h * 0.025f, paint)
        canvas.drawLine(w * 0.68f, lakeTop + h * 0.030f, w * 0.95f, lakeTop + h * 0.030f, paint)
    }

    private fun Int.withAlpha(alpha: Int): Int = Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))

    private data class Building(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val columns: Int,
        val rows: Int
    )
}
