package shree.tvhome.design

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
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import shree.tvhome.source.LauncherUtils
import shree.tvhome.source.TileThemePalette

class GlowTileLayout(context: Context) : FrameLayout(context) {
    private val outerCardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outerSheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunrayGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val coreBloomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val innerRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    private val rectF = RectF()
    private val sheenRect = RectF()
    private val outerPath = Path()
    private val innerIconPath = Path()

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
            // Smoothly dissolve the sunray aura when focus leaves
            val startGlow = glowIntensity.coerceAtLeast(0.45f)
            exitAnimator = ValueAnimator.ofFloat(startGlow, 0f).apply {
                duration = 340L
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

        val colors = TileThemePalette.getTopBottomColors(tileKey, tileFocused)
        outerCardPaint.shader = LinearGradient(0f, 0f, 0f, h, colors, null, Shader.TileMode.CLAMP)

        sheenRect.set(0f, 0f, w, h * 0.48f)
        outerSheenPaint.shader = LinearGradient(
            0f, 0f, 0f, h * 0.48f,
            Color.argb(if (tileFocused) 125 else 68, 255, 255, 255),
            Color.argb(0, 255, 255, 255),
            Shader.TileMode.CLAMP
        )

        // Inner core bloom inside the glass card
        coreBloomPaint.shader = RadialGradient(
            w / 2f, h * 0.45f, maxOf(w, h) * 0.65f,
            intArrayOf(
                LauncherUtils.withAlpha(appDominantColor, 160),
                LauncherUtils.withAlpha(appDominantColor, 55),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
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

        val cx = w / 2f
        val cy = h / 2f
        val baseRadius = maxOf(w, h) * 0.56f

        // 1. Borderless Sunray Radial Aura: bright behind icon and dissolves to 0% transparent before adjacent tiles
        if (tileFocused || glowIntensity > 0.01f) {
            val strength = if (tileFocused) (0.60f + 0.40f * glowIntensity) else glowIntensity
            val sunrayRadius = baseRadius * (1.08f + 0.14f * glowIntensity)

            sunrayGlowPaint.shader = RadialGradient(
                cx, cy, sunrayRadius,
                intArrayOf(
                    LauncherUtils.withAlpha(appDominantColor, (205 * strength).toInt()),
                    LauncherUtils.withAlpha(appDominantColor, (95 * strength).toInt()),
                    LauncherUtils.withAlpha(appDominantColor, (22 * strength).toInt()),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0.28f, 0.58f, 0.82f, 1.0f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, sunrayRadius, sunrayGlowPaint)
        }

        // 2. Borderless frosted glass tile base + glossy top sheen + core bloom
        canvas.drawPath(outerPath, outerCardPaint)
        canvas.save()
        canvas.clipPath(outerPath)
        canvas.drawRect(sheenRect, outerSheenPaint)
        if (tileFocused || glowIntensity > 0.01f) {
            coreBloomPaint.alpha = (255 * if (tileFocused) (0.65f + 0.35f * glowIntensity) else glowIntensity).toInt()
            canvas.drawRect(0f, 0f, w, h, coreBloomPaint)
        }
        canvas.restore()

        // 3. App icon clipped cleanly inside inner shape (no harsh outer border lines)
        canvas.save()
        canvas.clipPath(innerIconPath)
        super.dispatchDraw(canvas)
        canvas.restore()

        // Ultra-subtle inner glass edge highlight only around the app icon
        innerRimPaint.strokeWidth = dp(0.9f)
        innerRimPaint.color = if (tileFocused) Color.argb(145, 255, 255, 255) else Color.argb(55, 255, 255, 255)
        canvas.drawPath(innerIconPath, innerRimPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        exitAnimator?.cancel()
    }

    private fun dp(value: Float) = LauncherUtils.dpF(context, value)
}
