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
import shree.tvhome.source.LauncherUtils

object VisionUiComponents {

    // Google TV boot-style animated multi-color sweep-gradient border with transparent interior
    class AnimatedGradientBorderDrawable(
        private val ctx: Context,
        accentColor: Int
    ) : Drawable() {

        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val coreBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val matrix = Matrix()
        private val rectF = RectF()
        private var angle = 0f

        private val gradientColors = intArrayOf(
            Color.rgb(66, 133, 244),   // Google Blue
            Color.rgb(234, 67, 53),    // Crimson Coral
            Color.rgb(251, 188, 5),    // Warm Gold
            Color.rgb(52, 168, 83),    // Emerald Mint
            Color.rgb(175, 92, 255),   // Neon Orchid
            accentColor,
            Color.rgb(66, 133, 244)    // Seamless loop wrap
        )

        private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 5500L
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

            val cornerR = LauncherUtils.dpF(ctx, 26f)
            val inset = LauncherUtils.dpF(ctx, 3f)
            rectF.set(inset, inset, w - inset, h - inset)

            // Soft outer halo glow
            glowPaint.strokeWidth = LauncherUtils.dpF(ctx, 5.5f)
            glowPaint.alpha = 85
            canvas.drawRoundRect(rectF, cornerR, cornerR, glowPaint)

            // Crisp animated gradient border line (interior stays 100% transparent)
            coreBorderPaint.strokeWidth = LauncherUtils.dpF(ctx, 1.8f)
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

    // Apply only the animated rounding border (no interior glass fill)
    fun applySectionDeckStyle(ctx: Context, container: ViewGroup, enabled: Boolean, accentColor: Int) {
        (container.background as? AnimatedGradientBorderDrawable)?.stop()
        if (!enabled) {
            container.background = null
            return
        }
        val padH = LauncherUtils.dp(ctx, 16)
        val padV = LauncherUtils.dp(ctx, 12)
        container.setPadding(padH, padV, padH, padV)

        val borderDrawable = AnimatedGradientBorderDrawable(ctx, accentColor)
        container.background = borderDrawable

        container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = borderDrawable.start()
            override fun onViewDetachedFromWindow(v: View) = borderDrawable.stop()
        })
    }

    // Pure frameless icon card: zero interior background fill, only brand-color glow on focus
    class VisionTileLayout(context: Context) : FrameLayout(context) {
        private val ambientGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        private val rectF = RectF()
        private val clipPath = Path()

        private var isFocusedTile = false
        private var glowAlpha = 0f
        private var brandColor = Color.rgb(92, 184, 255)
        private var accentColor = Color.rgb(92, 184, 255)
        private var shapeKey = "squircle"

        init {
            setWillNotDraw(false)
            clipChildren = false
            clipToPadding = false
            outlineProvider = null
        }

        fun setVisionStyle(accent: Int, shape: String?, dominant: Int) {
            accentColor = accent
            brandColor = dominant
            shapeKey = shape ?: "squircle"
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
                    val r = if (shapeKey == "classic") LauncherUtils.dpF(context, 10f) else s * 0.25f
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

            // Soft brand-color ambient underglow only when focused
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

            // Pure full-bleed icon without any background fill or box
            canvas.save()
            canvas.clipPath(clipPath)
            super.dispatchDraw(canvas)
            canvas.restore()

            // Crisp white focus ring only when selected
            if (isFocusedTile) {
                rimPaint.strokeWidth = LauncherUtils.dpF(context, 2.2f)
                rimPaint.color = Color.WHITE
                canvas.drawPath(clipPath, rimPaint)
            }
        }
    }

    fun visionCardBg(ctx: Context, accent: Int, focused: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (focused) {
            intArrayOf(Color.argb(242, 255, 255, 255), Color.argb(222, 236, 244, 255))
        } else {
            intArrayOf(Color.argb(46, 255, 255, 255), Color.argb(22, 255, 255, 255))
        }
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 16f)
        setStroke(
            LauncherUtils.dp(ctx, if (focused) 2 else 1),
            if (focused) accent else Color.argb(52, 255, 255, 255)
        )
    }
    fun visionTrackBg(ctx: Context) = GradientDrawable().apply {
        setColor(Color.argb(42, 0, 0, 0))
        cornerRadius = LauncherUtils.dpF(ctx, 18f)
        setStroke(LauncherUtils.dp(ctx, 1), Color.argb(45, 255, 255, 255))
     }
     fun styleFloatingLabel(ctx: Context, tv: TextView, focused: Boolean) {
         tv.setTextColor(if (focused) Color.WHITE else Color.argb(230, 235, 242, 255))
         tv.typeface = if (focused) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL)
        tv.setShadowLayer(if (focused) 10f else 5f, 0f, 2f, Color.argb(210, 0, 0, 0))
     }
 }
