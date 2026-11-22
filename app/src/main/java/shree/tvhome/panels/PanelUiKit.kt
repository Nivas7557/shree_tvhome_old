package shree.tvhome.panels

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.LauncherUtils
import shree.tvhome.source.MotionEngine
import java.util.Locale

object PanelUiKit {

    fun addSectionHeader(ctx: Context, parent: LinearLayout, lp: LauncherPreferences, label: String) {
        parent.addView(
            TextView(ctx).apply {
                text = label.uppercase(Locale.ROOT)
                textSize = 8.8f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(lp.accentColor)
                letterSpacing = 0.09f
            },
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(ctx, 10)
                bottomMargin = dp(ctx, 5)
            }
        )
    }

    fun glassCardBg(ctx: Context, accent: Int, active: Boolean, themeKey: String? = null) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        if (active) {
            intArrayOf(
                LauncherUtils.withAlpha(accent, 185),
                LauncherUtils.withAlpha(accent, 95)
            )
        } else {
            val base = TileThemePalette.getTheme(themeKey).normalColors
            intArrayOf(
                LauncherUtils.blendWithAccent(base[0], accent, 0.14f, 145),
                LauncherUtils.withAlpha(base[1], 115)
            )
        }
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 14f)
        setStroke(dp(ctx, if (active) 2 else 1), if (active) Color.WHITE else LauncherUtils.withAlpha(accent, 95))
    }

    // Adaptive tvOS segmented track that automatically resizes thumb when panel width changes (20%-35%)
    fun addSegmentedTrack(
        ctx: Context,
        parent: LinearLayout,
        lp: LauncherPreferences,
        options: List<Pair<String, String>>,
        getSelected: () -> String,
        onSelect: (String) -> Unit
    ) {
        val track = FrameLayout(ctx).apply {
            setPadding(dp(ctx, 3), dp(ctx, 3), dp(ctx, 3), dp(ctx, 3))
            clipChildren = false
            clipToPadding = false
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(95, 10, 16, 38), Color.argb(65, 22, 34, 72))
            ).apply {
                cornerRadius = LauncherUtils.dpF(ctx, 14f)
                setStroke(dp(ctx, 1), LauncherUtils.withAlpha(lp.accentColor, 85))
            }
        }

        val activeThumb = View(ctx).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(LauncherUtils.withAlpha(lp.accentColor, 225), LauncherUtils.withAlpha(lp.accentColor, 145))
            ).apply {
                cornerRadius = LauncherUtils.dpF(ctx, 11f)
                setStroke(dp(ctx, 1), Color.WHITE)
            }
        }
        track.addView(activeThumb, FrameLayout.LayoutParams(0, dp(ctx, 30)))

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }

        val itemViews = ArrayList<Pair<String, TextView>>()
        options.forEach { (key, label) ->
            val tv = TextView(ctx).apply {
                text = label
                textSize = 9.2f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
                isFocusable = true
                isClickable = true
                setOnFocusChangeListener { v, f ->
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.04f, 4f)
                    if (f) {
                        v.background = GradientDrawable().apply {
                            cornerRadius = LauncherUtils.dpF(ctx, 11f)
                            setStroke(dp(ctx, 2), Color.WHITE)
                        }
                    } else {
                        v.background = null
                    }
                }
                setOnClickListener {
                    onSelect(key)
                    val targetView = itemViews.find { it.first == key }?.second ?: return@setOnClickListener
                    activeThumb.layoutParams.width = targetView.width
                    activeThumb.requestLayout()
                    activeThumb.animate()
                        .x(targetView.x)
                        .setDuration(240L)
                        .setInterpolator(LauncherUtils.FLUID_SPRING)
                        .start()
                }
            }
            itemViews.add(key to tv)
            row.addView(tv, LinearLayout.LayoutParams(0, dp(ctx, 30), 1f))
        }

        track.addView(row, FrameLayout.LayoutParams(-1, -2))
        // Dynamically recalculate thumb width & position whenever panel width changes
        track.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val target = itemViews.find { it.first == getSelected() }?.second ?: itemViews.firstOrNull()?.second ?: return@addOnLayoutChangeListener
            if (target.width > 0 && activeThumb.layoutParams.width != target.width) {
                activeThumb.layoutParams.width = target.width
                activeThumb.requestLayout()
            }
            activeThumb.x = target.x
        }
        parent.addView(track, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 5) })
    }

    // Thick tvOS-style glossy pill progress slider supporting D-Pad Left/Right & Touch
    fun addGlossyPillSlider(
        ctx: Context,
        parent: LinearLayout,
        lp: LauncherPreferences,
        icon: String,
        title: String,
        initialPct: Int,
        onChange: (Int) -> Unit
    ) {
        var currentPct = initialPct.coerceIn(0, 100)
        val sliderView = object : View(ctx) {
            private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.DEFAULT_BOLD
            }
            private val clipPath = Path()
            private val rect = RectF()
            private var isFocusedState = false

            init {
                isFocusable = true
                isClickable = true
                setOnFocusChangeListener { v, f ->
                    isFocusedState = f
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 5f)
                    invalidate()
                }
                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                currentPct = (currentPct - 5).coerceIn(0, 100)
                                onChange(currentPct)
                                invalidate()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                currentPct = (currentPct + 5).coerceIn(0, 100)
                                onChange(currentPct)
                                invalidate()
                                true
                            }
                            else -> false
                        }
                    } else false
                }
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    val ratio = (event.x / width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    currentPct = (ratio * 100).toInt()
                    onChange(currentPct)
                    invalidate()
                    return true
                }
                return super.onTouchEvent(event)
            }

            override fun onDraw(canvas: Canvas) {
                val w = width.toFloat()
                val h = height.toFloat()
                if (w <= 0f || h <= 0f) return
                val r = LauncherUtils.dpF(ctx, 14f)
                rect.set(0f, 0f, w, h)
                clipPath.reset()
                clipPath.addRoundRect(rect, r, r, Path.Direction.CW)

                canvas.save()
                canvas.clipPath(clipPath)

                trackPaint.color = Color.argb(58, 16, 24, 52)
                canvas.drawRect(rect, trackPaint)

                val fillW = w * (currentPct / 100f)
                fillPaint.shader = LinearGradient(
                    0f, 0f, fillW.coerceAtLeast(1f), 0f,
                    intArrayOf(LauncherUtils.withAlpha(lp.accentColor, 210), LauncherUtils.withAlpha(lp.accentColor, 130)),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, fillW, h, fillPaint)
                canvas.restore()

                borderPaint.strokeWidth = LauncherUtils.dpF(ctx, if (isFocusedState) 2f else 1f)
                borderPaint.color = if (isFocusedState) Color.WHITE else Color.argb(75, 255, 255, 255)
                canvas.drawRoundRect(rect, r, r, borderPaint)

                textPaint.textSize = LauncherUtils.dpF(ctx, 10.5f)
                textPaint.textAlign = Paint.Align.LEFT
                val ty = (h / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
                canvas.drawText("$icon  $title", LauncherUtils.dpF(ctx, 12f), ty, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("$currentPct%", w - LauncherUtils.dpF(ctx, 12f), ty, textPaint)
            }
        }
        parent.addView(sliderView, LinearLayout.LayoutParams(-1, dp(ctx, 40)).apply { bottomMargin = dp(ctx, 7) })
    }
    // Dual-tone gradient mini-orb for theme palette cards
    fun createThemeOrbView(ctx: Context, colors: IntArray, selected: Boolean): View = object : View(ctx) {
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val r = minOf(cx, cy) - LauncherUtils.dpF(ctx, 2f)
            if (r <= 0f) return
            fillPaint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), colors, null, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, r, fillPaint)
            ringPaint.strokeWidth = LauncherUtils.dpF(ctx, if (selected) 2f else 1f)
            ringPaint.color = if (selected) Color.WHITE else Color.argb(120, 255, 255, 255)
            canvas.drawCircle(cx, cy, r, ringPaint)
        }
    }

    fun makeActionRow(
        ctx: Context,
        lp: LauncherPreferences,
        icon: String,
        title: String,
        sub: String,
        trailingText: String = "›",
        onClick: () -> Unit
    ) = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9))
        isFocusable = true
        isClickable = true
        background = glassCardBg(ctx, lp.accentColor, false)
        setOnFocusChangeListener { v, f ->
            MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f)
            v.background = glassCardBg(ctx, lp.accentColor, f)
        }
        setOnClickListener { onClick() }
        addView(TextView(ctx).apply {
            text = icon
            textSize = 14f
            setTextColor(lp.accentColor)
            setPadding(0, 0, dp(ctx, 10), 0)
        })
        val mid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(TextView(ctx).apply {
            text = title
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
        })
        mid.addView(TextView(ctx).apply {
            text = sub
            textSize = 8.5f
            setTextColor(Color.argb(190, 210, 230, 245))
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
        })
        addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        addView(TextView(ctx).apply {
            text = trailingText
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.argb(200, 255, 255, 255))
        })
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
