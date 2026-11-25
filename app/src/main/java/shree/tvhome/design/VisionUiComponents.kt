package shree.tvhome.design

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import shree.tvhome.source.LauncherUtils

object VisionUiComponents {

    // Frameless Apple tvOS style card with edge-to-edge art and soft brand-color underglow
    class VisionTileLayout(context: Context) : FrameLayout(context) {
        private val ambientGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val specularPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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
                    val r = if (shapeKey == "classic") LauncherUtils.dpF(context, 8f) else s * 0.24f
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

            // Soft ambient aura behind the card when focused
            if (isFocusedTile || glowAlpha > 0.02f) {
                val radius = maxOf(w, h) * 0.78f
                ambientGlowPaint.shader = RadialGradient(
                    cx, cy + h * 0.08f, radius,
                    intArrayOf(
                        LauncherUtils.withAlpha(brandColor, (175 * glowAlpha).toInt()),
                        LauncherUtils.withAlpha(accentColor, (75 * glowAlpha).toInt()),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0.2f, 0.62f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(cx, cy + h * 0.08f, radius, ambientGlowPaint)
            }

            // Full-bleed edge-to-edge icon art
            canvas.save()
            canvas.clipPath(clipPath)
            surfacePaint.color = Color.argb(215, 22, 28, 48)
            canvas.drawRect(0f, 0f, w, h, surfacePaint)
            super.dispatchDraw(canvas)

            // Subtle top glass sheen
            specularPaint.shader = LinearGradient(
                0f, 0f, 0f, h * 0.42f,
                intArrayOf(Color.argb(if (isFocusedTile) 95 else 45, 255, 255, 255), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h * 0.42f, specularPaint)
            canvas.restore()

            // Hairline precision rim
            rimPaint.strokeWidth = LauncherUtils.dpF(context, if (isFocusedTile) 2.2f else 0.8f)
            rimPaint.color = if (isFocusedTile) Color.WHITE else Color.argb(65, 255, 255, 255)
            canvas.drawPath(clipPath, rimPaint)
        }
    }

    // VisionOS translucent glass card background for drawer controls
    fun visionCardBg(ctx: Context, accent: Int, focused: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (focused) {
            intArrayOf(Color.argb(235, 255, 255, 255), Color.argb(215, 238, 244, 255))
        } else {
            intArrayOf(Color.argb(42, 255, 255, 255), Color.argb(22, 255, 255, 255))
        }
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 16f)
        setStroke(
            LauncherUtils.dp(ctx, if (focused) 2 else 1),
            if (focused) accent else Color.argb(48, 255, 255, 255)
        )
    }

    // VisionOS pill track background
    fun visionTrackBg(ctx: Context) = GradientDrawable().apply {
        setColor(Color.argb(38, 0, 0, 0))
        cornerRadius = LauncherUtils.dpF(ctx, 18f)
        setStroke(LauncherUtils.dp(ctx, 1), Color.argb(42, 255, 255, 255))
    }

    // Clean floating typography without any boxy pill container
    fun styleFloatingLabel(ctx: Context, tv: TextView, focused: Boolean) {
        tv.setTextColor(if (focused) Color.WHITE else Color.argb(225, 235, 242, 255))
        tv.typeface = if (focused) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL)
        tv.setShadowLayer(if (focused) 8f else 4f, 0f, 2f, Color.argb(190, 0, 0, 0))
    }
}
