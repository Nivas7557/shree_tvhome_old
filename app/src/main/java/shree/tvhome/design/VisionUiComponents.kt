package shree.tvhome.design

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.LauncherUtils
import shree.tvhome.source.MotionEngine
import shree.tvhome.source.TileThemePalette

object VisionUiComponents {

    // Dynamically builds multi-color sweep gradient from registered TileThemePalette accents
    class AnimatedGradientBorderDrawable(
        private val ctx: Context,
        accentColor: Int
    ) : Drawable() {

        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val coreBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val matrix = Matrix()
        private val rectF = RectF()
        private var angle = 0f

        private val gradientColors: IntArray = buildList {
            add(accentColor)
            TileThemePalette.ACCENTS.forEach { spec ->
                if (spec.color != accentColor) add(spec.color)
            }
            add(accentColor)
        }.toIntArray()

        private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
            val style = LauncherPreferences(ctx).motionStyle
            duration = MotionEngine.getSpec(style).previewMs * 7L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                angle = it.animatedValue as Float
                invalidateSelf()
            }
        }

        fun start() {
            if (!animator.isStarted) animator.start()
        }

        fun stop() {
            animator.cancel()
        }

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)
            val w = bounds.width().toFloat()
            val h = bounds.height().toFloat()
            if (w <= 0f || h <= 0f) return
            val sweep = SweepGradient(w / 2f, h / 2f, gradientColors, null)
            coreBorderPaint.shader = sweep
            glowPaint.shader = sweep
            start()
        }

        override fun draw(canvas: Canvas) {
            val w = bounds.width().toFloat()
            val h = bounds.height().toFloat()
            if (w <= 0f || h <= 0f) return

            val cx = w / 2f
            val cy = h / 2f
            matrix.setRotate(angle, cx, cy)
            (coreBorderPaint.shader as? SweepGradient)?.setLocalMatrix(matrix)
            (glowPaint.shader as? SweepGradient)?.setLocalMatrix(matrix)

            val minSide = minOf(w, h)
            val cornerR = (minSide * 0.045f).coerceAtLeast(LauncherUtils.dpF(ctx, 14f))
            val strokeUnit = (minSide * 0.0032f).coerceAtLeast(1.5f)
            val inset = strokeUnit * 2.2f
            rectF.set(inset, inset, w - inset, h - inset)

            glowPaint.strokeWidth = strokeUnit * 3.2f
            glowPaint.alpha = 85
            canvas.drawRoundRect(rectF, cornerR, cornerR, glowPaint)

            coreBorderPaint.strokeWidth = strokeUnit
            coreBorderPaint.alpha = 235
            canvas.drawRoundRect(rectF, cornerR, cornerR, coreBorderPaint)
        }

        override fun setAlpha(alpha: Int) {
            coreBorderPaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            coreBorderPaint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    fun applySectionDeckStyle(ctx: Context, container: ViewGroup, enabled: Boolean, accentColor: Int) {
        (container.background as? AnimatedGradientBorderDrawable)?.stop()
        if (!enabled) {
            container.background = null
            return
        }
        val dm = ctx.resources.displayMetrics
        val padH = (dm.widthPixels * 0.014f).toInt()
        val padV = (dm.heightPixels * 0.015f).toInt()
        container.setPadding(padH, padV, padH, padV)
        container.clipToPadding = true
        container.clipChildren = true

        val borderDrawable = AnimatedGradientBorderDrawable(ctx, accentColor)
        container.background = borderDrawable

        container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = borderDrawable.start()
            override fun onViewDetachedFromWindow(v: View) = borderDrawable.stop()
        })
    }

    class VisionTileLayout(context: Context) : FrameLayout(context) {
        private val ambientGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        private val rectF = RectF()
        private val clipPath = Path()

        private var isFocusedTile = false
        private var glowAlpha = 0f
        private var brandColor = TileThemePalette.ACCENTS.first().color
        private var accentColor = TileThemePalette.ACCENTS.first().color
        private var shapeKey = TileThemePalette.TILE_SHAPES.first().key

        init {
            setWillNotDraw(false)
            clipChildren = false
            clipToPadding = false
            outlineProvider = null
        }

        fun setVisionStyle(accent: Int, shape: String?, dominant: Int) {
            accentColor = accent
            brandColor = dominant
            shapeKey = shape ?: TileThemePalette.TILE_SHAPES.first().key
            if (width > 0 && height > 0) rebuildPath(width.toFloat(), height.toFloat())
            invalidate()
        }

        fun setVisionFocus(focused: Boolean) {
            isFocusedTile = focused
            glowAlpha = if (focused) 1f else 0f
            invalidate()
        }

        fun setVisionPulse(pulse: Float) {
            glowAlpha = 0.65f + 0.35f * pulse.coerceIn(0f, 1f)
            invalidate()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            rebuildPath(w.toFloat(), h.toFloat())
        }

        private fun rebuildPath(w: Float, h: Float) {
            clipPath.reset()
            if (w <= 0f || h <= 0f) return
            val s = minOf(w, h)
            val left = (w - s) / 2f
            val top = (h - s) / 2f
            val right = left + s
            val bottom = top + s
            val cx = w / 2f
            val cy = h / 2f

            when (shapeKey) {
                "circle" -> clipPath.addCircle(cx, cy, s / 2f, Path.Direction.CW)
                "heart" -> {
                    val topDipY = top + s * 0.26f
                    val bottomTipY = bottom - s * 0.04f
                    clipPath.moveTo(cx, bottomTipY)
                    clipPath.cubicTo(left - s * 0.06f, top + s * 0.62f, left + s * 0.02f, top + s * 0.04f, cx, topDipY)
                    clipPath.cubicTo(right - s * 0.02f, top + s * 0.04f, right + s * 0.06f, top + s * 0.62f, cx, bottomTipY)
                    clipPath.close()
                }
                else -> {
                    val r = if (shapeKey == "classic") s * 0.11f else s * 0.25f
                    rectF.set(left, top, right, bottom)
                    clipPath.addRoundRect(rectF, r, r, Path.Direction.CW)
                }
            }
        }

        override fun dispatchDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            val cx = w / 2f
            val cy = h / 2f

            if (isFocusedTile || glowAlpha > 0.02f) {
                val radius = maxOf(w, h) * 0.82f
                ambientGlowPaint.shader = RadialGradient(
                    cx, cy + h * 0.08f, radius,
                    intArrayOf(
                        LauncherUtils.withAlpha(brandColor, (195 * glowAlpha).toInt()),
                        LauncherUtils.withAlpha(accentColor, (85 * glowAlpha).toInt()),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0.18f, 0.58f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(cx, cy + h * 0.08f, radius, ambientGlowPaint)
            }

            canvas.save()
            canvas.clipPath(clipPath)
            super.dispatchDraw(canvas)
            canvas.restore()

            if (isFocusedTile) {
                rimPaint.strokeWidth = (minOf(w, h) * 0.028f).coerceAtLeast(2f)
                rimPaint.color = Color.WHITE
                canvas.drawPath(clipPath, rimPaint)
            }
        }
    }

    fun visionCardBg(ctx: Context, accent: Int, focused: Boolean): GradientDrawable {
        val baseSurface = TileThemePalette.getTheme(LauncherPreferences(ctx).tileColor).normalColors
        val topBase = baseSurface.firstOrNull() ?: accent
        val botBase = baseSurface.lastOrNull() ?: accent
        val cornerR = ctx.resources.displayMetrics.heightPixels * 0.022f

        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            if (focused) {
                intArrayOf(
                    LauncherUtils.blendWithAccent(Color.WHITE, accent, 0.08f, 240),
                    LauncherUtils.blendWithAccent(Color.WHITE, accent, 0.16f, 220)
                )
            } else {
* 0.025f
return GradientDrawable().apply {
setColor(LauncherUtils.withAlpha(Color.BLACK, 44))
cornerRadius = cornerR
setStroke(1, LauncherUtils.withAlpha(Color.WHITE, 45))
}
}
fun styleFloatingLabel(ctx: Context, tv: TextView, focused: Boolean) {
tv.setTextColor(if (focused) Color.WHITE else LauncherUtils.withAlpha(Color.WHITE, 230))
tv.typeface = if (focused) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL)
val shadowR = tv.textSize * if (focused) 0.75f else 0.4f
tv.setShadowLayer(shadowR, 0f, shadowR * 0.25f, LauncherUtils.withAlpha(Color.BLACK, 215))
}
}
