package shree.tvhome.design

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import shree.tvhome.source.LauncherUtils
import shree.tvhome.source.TileThemePalette

class GlowTileLayout(context: Context) : FrameLayout(context) {
    private val outerCardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outerSheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spotlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val radiantAuraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val sweepBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val innerRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    private val rectF = RectF()
    private val sheenRect = RectF()
    private val outerPath = Path()
    private val innerIconPath = Path()
    private val sweepMatrix = Matrix()

    private var tileFocused = false
    private var glowIntensity = 0f
    private var exitAnimator: ValueAnimator? = null
    private var accentColor = TileThemePalette.ACCENTS.first().color
    private var appDominantColor = TileThemePalette.ACCENTS.first().color
    private var iconShape = TileThemePalette.TILE_SHAPES.first().key
    private var tileKey = TileThemePalette.THEMES.first().key

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
        outlineProvider = null
        // Required for smooth BlurMaskFilter ambient glow rendering
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setStyle(color: Int, shape: String?, themeKey: String?, dominantColor: Int = color) {
        accentColor = color
        appDominantColor = dominantColor
        iconShape = shape ?: TileThemePalette.TILE_SHAPES.first().key
        tileKey = themeKey ?: TileThemePalette.THEMES.first().key
        if (width > 0 && height > 0) {
            buildDoublePaths(width.toFloat(), height.toFloat())
            updateShaders(width.toFloat(), height.toFloat())
        }
        invalidate()
    }

    fun setFocusState(focused: Boolean) {
        if (tileFocused == focused) return
        tileFocused = focused
        exitAnimator?.cancel()

        if (focused) {
            if (width > 0 && height > 0) updateShaders(width.toFloat(), height.toFloat())
            invalidate()
        } else {
            // Smoothly fade out the radiant aura when focus moves away instead of snapping off abruptly
            val startGlow = glowIntensity.coerceAtLeast(0.45f)
            exitAnimator = ValueAnimator.ofFloat(startGlow, 0f).apply {
                duration = 320L
                interpolator = DecelerateInterpolator(1.6f)
                addUpdateListener { anim ->
                    glowIntensity = anim.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }

    fun updateGlowPulse(intensity: Float) {
        glowIntensity = intensity.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val wf = w.toFloat()
        val hf = h.toFloat()
        buildDoublePaths(wf, hf)
        updateShaders(wf, hf)
    }

    private fun updateShaders(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        val minDim = minOf(w, h)

        val colors = TileThemePalette.getTopBottomColors(tileKey, tileFocused)
        outerCardPaint.shader = LinearGradient(0f, 0f, 0f, h, colors, null, Shader.TileMode.CLAMP)

        sheenRect.set(0f, 0f, w, h * 0.50f)
        outerSheenPaint.shader = LinearGradient(
            0f, 0f, 0f, h * 0.50f,
            Color.argb(if (tileFocused) 130 else 72, 255, 255, 255),
            Color.argb(6, 255, 255, 255),
            Shader.TileMode.CLAMP
        )

        // Dynamic soft Gaussian blur radius proportional to tile size
        val blurRadius = (minDim * 0.18f).coerceAtLeast(6f)
        radiantAuraPaint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        radiantAuraPaint.color = appDominantColor

        spotlightPaint.shader = RadialGradient(
            w / 2f, h * 0.45f, maxOf(w, h) * 0.72f,
            LauncherUtils.withAlpha(appDominantColor, 155),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )

        // Specular rotating rim shader for focused border
        sweepBorderPaint.shader = SweepGradient(
            w / 2f, h / 2f,
            intArrayOf(
                LauncherUtils.withAlpha(appDominantColor, 90),
                Color.argb(235, 255, 255, 255),
                LauncherUtils.withAlpha(appDominantColor, 220),
                LauncherUtils.withAlpha(appDominantColor, 90)
            ),
            floatArrayOf(0f, 0.25f, 0.65f, 1f)
        )
    }

    private fun buildDoublePaths(w: Float, h: Float) {
        val outerInset = dp(1.5f)
        val visibleGap = minOf(w, h) * 0.095f
        buildShapeIntoPath(outerPath, w, h, outerInset)
        buildShapeIntoPath(innerIconPath, w, h, outerInset + visibleGap)
    }

    private fun buildShapeIntoPath(target: Path, w: Float, h: Float, inset: Float) {
        target.reset()
        val sw = w - inset * 2f
        val sh = h - inset * 2f
        if (sw <= 0f || sh <= 0f) return

        val s = minOf(sw, sh)
        val left = (w - s) / 2f
        val top = (h - s) / 2f
        val right = left + s
        val bottom = top + s
        val cx = w / 2f
        val cy = h / 2f

        when (iconShape) {
            "heart" -> {
                val topDipY = top + s * 0.26f
                val bottomTipY = bottom - s * 0.04f
                target.moveTo(cx, bottomTipY)
                target.cubicTo(
                    left - s * 0.06f, top + s * 0.62f,
                    left + s * 0.02f, top + s * 0.04f,
                    cx, topDipY
                )
                target.cubicTo(
                    right - s * 0.02f, top + s * 0.04f,
                    right + s * 0.06f, top + s * 0.62f,
                    cx, bottomTipY
                )
                target.close()
            }
            "circle" -> {
                target.addCircle(cx, cy, s / 2f, Path.Direction.CW)
            }
            else -> {
                val radius = if (iconShape == "classic") dp(6f) else s * 0.22f
                rectF.set(left, top, right, bottom)
                target.addRoundRect(rectF, radius, radius, Path.Direction.CW)
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // 1. Natural soft radiant ambient glow behind the tile (active during focus & smooth exit)
        if (tileFocused || glowIntensity > 0.01f) {
            val activeStrength = if (tileFocused) (0.55f + 0.45f * glowIntensity) else glowIntensity
            radiantAuraPaint.alpha = (185 * activeStrength).toInt().coerceIn(0, 220)
            canvas.drawPath(outerPath, radiantAuraPaint)
        }

        // 2. Glass card base & top sheen
        canvas.drawPath(outerPath, outerCardPaint)
        canvas.save()
        canvas.clipPath(outerPath)
        canvas.drawRect(sheenRect, outerSheenPaint)
        if (tileFocused || glowIntensity > 0.01f) {
            spotlightPaint.alpha = ((120 + 95 * glowIntensity) * if (tileFocused) 1f else glowIntensity).toInt()
            canvas.drawRect(0f, 0f, w, h, spotlightPaint)
        }
        canvas.restore()

        // 3. Rotating specular rim border on focus, subtle frosted rim when idle
        if (tileFocused || glowIntensity > 0.01f) {
            sweepMatrix.setRotate(glowIntensity * 180f, w / 2f, h / 2f)
            sweepBorderPaint.shader?.setLocalMatrix(sweepMatrix)
            sweepBorderPaint.strokeWidth = dp(1.8f)
            sweepBorderPaint.alpha = (255 * if (tileFocused) 1f else glowIntensity).toInt()
            canvas.drawPath(outerPath, sweepBorderPaint)
        } else {
            innerRimPaint.strokeWidth = dp(1f)
            innerRimPaint.color = Color.argb(75, 255, 255, 255)
            canvas.drawPath(outerPath, innerRimPaint)
        }

        // 4. App icon clipped cleanly inside the inner shape
        canvas.save()
        canvas.clipPath(innerIconPath)
        super.dispatchDraw(canvas)
        canvas.restore()

        innerRimPaint.strokeWidth = dp(1f)
        innerRimPaint.color = if (tileFocused) Color.argb(190, 255, 255, 255) else Color.argb(95, 255, 255, 255)
        canvas.drawPath(innerIconPath, innerRimPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        exitAnimator?.cancel()
    }

    private fun dp(value: Float) = LauncherUtils.dpF(context, value)
}
