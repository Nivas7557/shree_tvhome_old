package shree.tvhome.source

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar

class GreetingShineCard(
    ctx: Context,
    private var userName: String,
    private var customPhrase: String,
    private var accentColor: Int
) : LinearLayout(ctx) {

    private data class GreetingPeriodSpec(val hours: IntRange, val emoji: String, val label: String)

    private val greetingPeriods = listOf(
        GreetingPeriodSpec(5..11, "☀️", "Good Morning"),
        GreetingPeriodSpec(12..16, "🌤️", "Good Afternoon"),
        GreetingPeriodSpec(17..20, "🌆", "Good Evening")
    )

    private val iconTv = TextView(ctx)
    private val titleTv = TextView(ctx)
    private var overlayBubble: TextView? = null
    private var shimmerAnim: ValueAnimator? = null
    private var shimmerX = -200f
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaderMatrix = Matrix()
    private val shapePath = Path()
    private val avatarCutoutPath = Path()
    private val clipRect = RectF()

    private var cutoutCenterX = -1f
    private var cutoutCenterY = -1f
    private var cutoutRadius = 0f

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        outlineProvider = null
        setWillNotDraw(false)
        setPadding(dp(14), dp(7), dp(16), dp(7))

        iconTv.apply {
            textSize = 15f
            setPadding(0, 0, dp(8), 0)
        }
        titleTv.apply {
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
        }
        addView(iconTv)
        addView(titleTv)
        updateGreeting(userName, customPhrase)
        setAccentColor(accentColor)
    }

    fun attachOverlayBubble(bubble: TextView) {
        overlayBubble = bubble
    }

    // Dynamically subtract the avatar circle geometry so the card starts cleanly from the circle's outer edge
    fun setAvatarCutout(centerXInCard: Float, centerYInCard: Float, radiusWithGap: Float) {
        cutoutCenterX = centerXInCard
        cutoutCenterY = centerYInCard
        cutoutRadius = radiusWithGap
        rebuildShapePath(width, height)
        invalidate()
    }

    fun setAccentColor(accent: Int) {
        accentColor = accent
        if (width > 0 && height > 0) {
            bgPaint.shader = LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                intArrayOf(LauncherUtils.withAlpha(accentColor, 115), Color.argb(85, 22, 34, 72)),
                null, Shader.TileMode.CLAMP
            )
        }
        invalidate()
    }

    fun updateGreeting(name: String, phrase: String) {
        userName = name
        customPhrase = phrase

        val hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val matched = greetingPeriods.find { hr in it.hours }
        val emoji = matched?.emoji ?: "🌙"
        val autoGreet = matched?.label ?: "Good Night"

        iconTv.text = emoji
        val cleanName = userName.trim()
        titleTv.text = when {
            customPhrase.isNotBlank() && cleanName.isNotEmpty() -> "$customPhrase, $cleanName"
            customPhrase.isNotBlank() -> customPhrase
            cleanName.isNotEmpty() -> "$autoGreet, $cleanName"
            else -> autoGreet
        }
    }

    private fun rebuildShapePath(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val r = h / 2f
        clipRect.set(0f, 0f, w.toFloat(), h.toFloat())
        shapePath.reset()
        shapePath.addRoundRect(clipRect, r, r, Path.Direction.CW)

        // Carve out the exact circular arc where the avatar meets this card
        if (cutoutRadius > 0f) {
            avatarCutoutPath.reset()
            avatarCutoutPath.addCircle(cutoutCenterX, cutoutCenterY, cutoutRadius, Path.Direction.CW)
            shapePath.op(avatarCutoutPath, Path.Op.DIFFERENCE)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        rebuildShapePath(w, h)
        bgPaint.shader = LinearGradient(
            0f, 0f, w.toFloat(), 0f,
            intArrayOf(LauncherUtils.withAlpha(accentColor, 115), Color.argb(85, 22, 34, 72)),
            null, Shader.TileMode.CLAMP
        )

        shimmerPaint.shader = LinearGradient(
            0f, 0f, w * 0.35f, h * 0.4f,
            intArrayOf(Color.TRANSPARENT, Color.argb(55, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        shimmerAnim?.cancel()
        shimmerAnim = ValueAnimator.ofFloat(-w * 0.5f, w * 1.5f).apply {
            duration = 3200L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { shimmerX = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (width > 0 && height > 0) {
            canvas.drawPath(shapePath, bgPaint)
        }
        super.onDraw(canvas)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (width > 0 && shimmerPaint.shader != null) {
            canvas.save()
            canvas.clipPath(shapePath)
            shaderMatrix.setTranslate(shimmerX, 0f)
            shimmerPaint.shader.setLocalMatrix(shaderMatrix)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shimmerPaint)
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        shimmerAnim?.cancel()
    }

    private fun dp(v: Int) = LauncherUtils.dp(context, v)
}
