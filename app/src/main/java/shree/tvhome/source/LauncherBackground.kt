package shree.tvhome.source

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object LauncherBackground {
    const val MODE_SMART = "smart"
    const val MODE_AUTO_TIME = "auto"
    const val MODE_MORNING = "morning"
    const val MODE_PASTEL = "pastel"
    const val MODE_SUNSET = "sunset"
    const val MODE_AQUA = "aqua"
    const val MODE_RAIN = "rain"
    const val MODE_COSMOS = "cosmos"

    data class SmartModeSpec(
        val key: String,
        val icon: String,
        val name: String,
        val subtitle: String,
        val swatch: IntArray
    )

    data class SceneSpec(
        val key: String,
        val icon: String,
        val name: String,
        val swatch: IntArray,
        val isLightSky: Boolean,
        val particleKind: String,
        val skyColors: IntArray,
        val orb1Colors: IntArray,
        val orb2Colors: IntArray,
        val showMoon: Boolean = false
    )

    val SMART_MODES = listOf(
        SmartModeSpec(
            key = MODE_SMART,
            icon = "✨",
            name = "Smart Auto",
            subtitle = "Time + Weather",
            swatch = intArrayOf(Color.rgb(52, 128, 225), Color.rgb(155, 72, 185))
        ),
        SmartModeSpec(
            key = MODE_AUTO_TIME,
            icon = "🕒",
            name = "Time Flow",
            subtitle = "Day to Night",
            swatch = intArrayOf(Color.rgb(235, 138, 72), Color.rgb(42, 62, 135))
        )
    )

    // Vibrant jewel-tone scenes balanced for TV glass contrast (never pale/washed-out or dull dark)
    val SCENES = listOf(
        SceneSpec(
            key = MODE_MORNING,
            icon = "🌌",
            name = "Aurora Jade",
            swatch = intArrayOf(Color.rgb(16, 145, 135), Color.rgb(88, 65, 185)),
            isLightSky = false,
            particleKind = "orb",
            skyColors = intArrayOf(Color.rgb(14, 52, 88), Color.rgb(18, 138, 132), Color.rgb(76, 56, 168)),
            orb1Colors = intArrayOf(Color.argb(195, 52, 235, 185), Color.argb(75, 32, 185, 220), Color.TRANSPARENT),
            orb2Colors = intArrayOf(Color.argb(150, 145, 88, 255), Color.TRANSPARENT)
        ),
        SceneSpec(
            key = MODE_PASTEL,
            icon = "🔮",
            name = "Prism Orchid",
            swatch = intArrayOf(Color.rgb(125, 48, 185), Color.rgb(38, 148, 215)),
            isLightSky = false,
            particleKind = "orb",
            skyColors = intArrayOf(Color.rgb(54, 24, 112), Color.rgb(138, 46, 168), Color.rgb(28, 118, 188)),
            orb1Colors = intArrayOf(Color.argb(190, 235, 82, 185), Color.argb(70, 165, 65, 245), Color.TRANSPARENT),
            orb2Colors = intArrayOf(Color.argb(165, 56, 215, 245), Color.TRANSPARENT)
        ),
        SceneSpec(
            key = MODE_SUNSET,
            icon = "🌇",
            name = "Velvet Sunset",
            swatch = intArrayOf(Color.rgb(185, 48, 98), Color.rgb(245, 128, 52)),
            isLightSky = false,
            particleKind = "orb",
            skyColors = intArrayOf(Color.rgb(48, 20, 86), Color.rgb(178, 46, 96), Color.rgb(235, 118, 54)),
            orb1Colors = intArrayOf(Color.argb(215, 255, 185, 72), Color.argb(90, 245, 78, 92), Color.TRANSPARENT),
            orb2Colors = intArrayOf(Color.argb(135, 165, 52, 168), Color.TRANSPARENT)
        ),
        SceneSpec(
            key = MODE_AQUA,
            icon = "🌊",
            name = "Electric Lagoon",
            swatch = intArrayOf(Color.rgb(14, 98, 175), Color.rgb(28, 198, 178)),
            isLightSky = false,
            particleKind = "orb",
            skyColors = intArrayOf(Color.rgb(12, 46, 108), Color.rgb(16, 125, 178), Color.rgb(26, 188, 168)),
            orb1Colors = intArrayOf(Color.argb(185, 68, 245, 225), Color.argb(65, 32, 165, 245), Color.TRANSPARENT),
            orb2Colors = intArrayOf(Color.argb(145, 65, 135, 255), Color.TRANSPARENT)
        ),
        SceneSpec(
            key = MODE_RAIN,
            icon = "🌧️",
            name = "Rainy Mist",
            swatch = intArrayOf(Color.rgb(24, 58, 92), Color.rgb(65, 135, 175)),
            isLightSky = false,
            particleKind = "rain",
            skyColors = intArrayOf(Color.rgb(16, 36, 62), Color.rgb(28, 68, 96), Color.rgb(14, 28, 48)),
            orb1Colors = intArrayOf(Color.argb(120, 92, 195, 245), Color.TRANSPARENT),
            orb2Colors = intArrayOf(Color.argb(85, 75, 155, 235), Color.TRANSPARENT)
        ),
        SceneSpec(
            key = MODE_COSMOS,
            icon = "🌙",
            name = "Royal Moon",
            swatch = intArrayOf(Color.rgb(16, 24, 54), Color.rgb(48, 64, 125)),
            isLightSky = false,
            particleKind = "star",
            skyColors = intArrayOf(Color.rgb(10, 16, 36), Color.rgb(18, 28, 56), Color.rgb(8, 12, 28)),
            orb1Colors = intArrayOf(Color.argb(220, 255, 245, 190), Color.argb(90, 255, 220, 130), Color.argb(25, 255, 200, 100), Color.TRANSPARENT),
            orb2Colors = intArrayOf(Color.argb(75, 92, 184, 255), Color.TRANSPARENT),
            showMoon = true
        )
    )

    private val sceneMap by lazy { SCENES.associateBy { it.key } }

    fun getScene(key: String): SceneSpec = sceneMap[key] ?: SCENES.last()

    private var activeWallpaperView: ImageView? = null
    private var activePhotoAuraView: PhotoWeatherAuraView? = null
    private var activeCanvasView: DynamicCanvasView? = null
    private var baseZoom = 1.08f
    private var baseOffsetY = 0

    fun apply(ctx: Context, root: FrameLayout, lp: LauncherPreferences) {
        val sp = lp.prefs
        val wallpaper = sp.getString(LauncherPreferences.PREF_WALLPAPER, null)
        activeWallpaperView = null
        activePhotoAuraView = null
        activeCanvasView = null

        if (wallpaper != null) {
            root.background = null
            val image = ImageView(ctx).apply {
                scaleType = when (lp.wpScaleMode) {
                    "width" -> ImageView.ScaleType.FIT_XY
                    "height" -> ImageView.ScaleType.FIT_CENTER
                    else -> ImageView.ScaleType.CENTER_CROP
                }
                baseZoom = maxOf(1.08f, lp.wpZoom)
                baseOffsetY = lp.wpOffsetY
                scaleX = baseZoom; scaleY = baseZoom
                translationX = lp.wpOffsetX.toFloat()
                translationY = baseOffsetY.toFloat()
                val bmp = decodeSampledBitmapFromUri(ctx, Uri.parse(wallpaper))
                if (bmp != null) setImageBitmap(bmp)
                else try { setImageURI(Uri.parse(wallpaper)) } catch (_: Exception) {}
            }
            root.addView(image, 0, FrameLayout.LayoutParams(-1, -1))
            activeWallpaperView = image

            if (lp.photoWeatherAura) {
                val aura = PhotoWeatherAuraView(ctx, lp)
                root.addView(aura, 1, FrameLayout.LayoutParams(-1, -1))
                activePhotoAuraView = aura
            }
        } else {
            root.background = null
            val canvasView = DynamicCanvasView(ctx, lp)
            root.addView(canvasView, 0, FrameLayout.LayoutParams(-1, -1))
            activeCanvasView = canvasView
        }
    }

    private fun decodeSampledBitmapFromUri(ctx: Context, uri: Uri): Bitmap? {
        return try {
            val dm = ctx.resources.displayMetrics
            val reqW = maxOf(1280, dm.widthPixels)
            val reqH = maxOf(720, dm.heightPixels)

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            var inSampleSize = 1
            if (options.outHeight > reqH || options.outWidth > reqW) {
                val halfH = options.outHeight / 2
                val halfW = options.outWidth / 2
                while (halfH / inSampleSize >= reqH && halfW / inSampleSize >= reqW) {
                    inSampleSize *= 2
                }
            }

            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
        } catch (_: Exception) {
            null
        }
    }

    fun updateThemeAndAccent(root: FrameLayout, lp: LauncherPreferences) {
        if (lp.prefs.getString(LauncherPreferences.PREF_WALLPAPER, null) == null) {
            activeCanvasView?.updateConfig()
        } else {
            activePhotoAuraView?.invalidate()
        }
    }

    fun onScrollParallax(scrollY: Int) {
        val shift = minOf(22f, scrollY * 0.07f)
        activeWallpaperView?.translationY = baseOffsetY - shift
        activeCanvasView?.setParallaxShift(shift)
    }

    private fun resolveEffectiveMode(lp: LauncherPreferences): String {
        val mode = lp.liveWallpaperMode
        if (mode == MODE_SMART) {
            val wCode = lp.cachedWeatherCode
            if (wCode in 51..67 || wCode in 80..99) return MODE_RAIN
        }
        if (mode == MODE_SMART || mode == MODE_AUTO_TIME) {
            val hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hr) {
                in 6..10 -> MODE_MORNING
                in 11..15 -> MODE_PASTEL
                in 16..18 -> MODE_SUNSET
                else -> MODE_COSMOS
            }
        }
        return if (sceneMap.containsKey(mode)) mode else SCENES.last().key
    }

    private fun blendColor(c1: Int, c2: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)
        val a = (Color.alpha(c1) + (Color.alpha(c2) - Color.alpha(c1)) * t).toInt()
        val r = (Color.red(c1) + (Color.red(c2) - Color.red(c1)) * t).toInt()
        val g = (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * t).toInt()
        val b = (Color.blue(c1) + (Color.blue(c2) - Color.blue(c1)) * t).toInt()
        return Color.argb(a, r, g, b)
    }

    private fun tintWithAccent(colors: IntArray, accent: Int): IntArray {
        return colors.mapIndexed { idx, col ->
            if (idx == 0) blendColor(col, accent, 0.22f) else col
        }.toIntArray()
    }

    private class PhotoWeatherAuraView(ctx: Context, private val lp: LauncherPreferences) : View(ctx) {
        private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 200, 235, 255)
            strokeCap = Paint.Cap.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f || h <= 0f) return
            val wCode = lp.cachedWeatherCode
            val isRain = wCode in 51..67 || wCode in 80..99
            val isDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 6..17

            val glowCol = when {
                isRain -> Color.argb(52, 65, 165, 225)
                isDay -> Color.argb(48, 255, 210, 110)
                else -> Color.argb(45, 110, 145, 245)
            }
            edgePaint.shader = RadialGradient(w * 0.88f, h * 0.16f, w * 0.42f, intArrayOf(glowCol, Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, edgePaint)

            if (isRain) {
                rainPaint.strokeWidth = LauncherUtils.dpF(context, 1.2f)
                for (i in 0 until 14) {
                    val rx = w * (0.72f + (i % 5) * 0.06f)
                    val ry = h * (0.06f + (i * 0.06f))
                    canvas.drawLine(rx, ry, rx - 8f, ry + 18f, rainPaint)
                }
            }
        }
    }

    private class DynamicCanvasView(
        ctx: Context,
        private val lp: LauncherPreferences
    ) : View(ctx) {

        private val skyPaint = Paint(Paint.DITHER_FLAG)
        private val orb1Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val orb2Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val prevSkyPaint = Paint(Paint.DITHER_FLAG)
        private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; strokeCap = Paint.Cap.ROUND }
        private val moonDiscPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val craterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(30, 165, 185, 215) }
        private val contrastVeilPaint = Paint().apply { color = Color.argb(58, 10, 15, 32) }

        private val starPath = Path()
        private var flowAnim: ValueAnimator? = null
        private var transitionAnim: ValueAnimator? = null
        private var transitionAlpha = 0f
        private var phase = 0f
        private var parallaxY = 0f
        private var activeEffectiveMode = resolveEffectiveMode(lp)
        private var lastCalculatedMinute = -1

        private data class Particle(val xRatio: Float, val yRatio: Float, val sizePx: Float, val speed: Float, val offset: Float)
        private val particles = List(42) { i ->
            val rnd = Random(i * 997 + 13)
            val d = resources.displayMetrics.density
            Particle(
                0.02f + rnd.nextFloat() * 0.96f,
                0.02f + rnd.nextFloat() * 0.88f,
                (2.0f + rnd.nextFloat() * 4.8f) * d,
                0.9f + rnd.nextFloat() * 2.8f,
                rnd.nextFloat() * 10f
            )
        }

        fun updateConfig() {
            val nextEffective = resolveEffectiveMode(lp)
            val modeChanged = nextEffective != activeEffectiveMode
            lastCalculatedMinute = -1

            if (modeChanged && width > 0 && height > 0) {
                prevSkyPaint.shader = skyPaint.shader
                activeEffectiveMode = nextEffective
                buildShaders(width, height)
                startSmoothCrossFade()
            } else {
                activeEffectiveMode = nextEffective
                buildShaders(width, height)
                invalidate()
            }
        }

        private fun startSmoothCrossFade() {
            transitionAnim?.cancel()
            transitionAlpha = 1f
            transitionAnim = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 1400L
                addUpdateListener { transitionAlpha = it.animatedValue as Float; invalidate() }
                start()
            }
        }

        fun setParallaxShift(shift: Float) {
            if (parallaxY != shift) { parallaxY = shift; invalidate() }
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w <= 0 || h <= 0) return
            lastCalculatedMinute = -1
            activeEffectiveMode = resolveEffectiveMode(lp)
            buildShaders(w, h)
            flowAnim?.cancel()
            flowAnim = ValueAnimator.ofFloat(0f, 6.28318f).apply {
                duration = 20000L; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
                addUpdateListener {
                    phase = it.animatedValue as Float
                    val mode = lp.liveWallpaperMode
                    if (mode == MODE_SMART || mode == MODE_AUTO_TIME) {
                        val curMin = Calendar.getInstance().get(Calendar.MINUTE)
                        if (curMin != lastCalculatedMinute) {
                            lastCalculatedMinute = curMin
                            activeEffectiveMode = resolveEffectiveMode(lp)
                            buildShaders(width, height)
                        }
                    }
                    invalidate()
                }
                start()
            }
        }

        private fun getAutoSkyPalette(): IntArray {
            val cal = Calendar.getInstance()
            val totalMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            lastCalculatedMinute = cal.get(Calendar.MINUTE)

            val morning = getScene(MODE_MORNING).skyColors
            val pastel = getScene(MODE_PASTEL).skyColors
            val sunset = getScene(MODE_SUNSET).skyColors
            val night = getScene(MODE_COSMOS).skyColors

            val (from, to, ratio) = when {
                totalMins in 300..419 -> Triple(night, morning, (totalMins - 300) / 120f)
                totalMins in 420..659 -> Triple(morning, pastel, (totalMins - 420) / 240f)
                totalMins in 660..959 -> Triple(pastel, sunset, (totalMins - 660) / 300f)
                totalMins in 960..1139 -> Triple(sunset, night, (totalMins - 960) / 180f)
                else -> Triple(night, night, 0f)
            }
            return intArrayOf(blendColor(from[0], to[0], ratio), blendColor(from[1], to[1], ratio), blendColor(from[2], to[2], ratio))
        }

        private fun buildShaders(w: Int, h: Int) {
            if (w <= 0 || h <= 0) return
            val wf = w.toFloat(); val hf = h.toFloat()
            val accentColor = lp.accentColor
            val isAuto = lp.liveWallpaperMode == MODE_SMART || lp.liveWallpaperMode == MODE_AUTO_TIME
            val spec = getScene(activeEffectiveMode)

            val activeSky = if (isAuto && spec.key != MODE_RAIN && spec.key != MODE_AQUA) {
                getAutoSkyPalette()
            } else {
                spec.skyColors
            }
            skyPaint.shader = LinearGradient(0f, 0f, wf, hf, activeSky, floatArrayOf(0f, 0.52f, 1f), Shader.TileMode.CLAMP)

            val orb1X = if (spec.showMoon) wf * 0.5f else wf * 0.25f
            val orb1Y = if (spec.showMoon) hf * 0.15f else hf * 0.28f
            orb1Paint.shader = RadialGradient(
                orb1X, orb1Y, wf * 0.58f,
                tintWithAccent(spec.orb1Colors, accentColor), null, Shader.TileMode.CLAMP
            )
            orb2Paint.shader = RadialGradient(
                wf * 0.75f, hf * 0.72f, wf * 0.56f,
                tintWithAccent(spec.orb2Colors, accentColor), null, Shader.TileMode.CLAMP
            )

            if (spec.showMoon) {
                val moonX = wf * 0.5f; val moonY = hf * 0.15f; val moonR = 24f * resources.displayMetrics.density
                moonDiscPaint.shader = RadialGradient(
                    moonX - moonR * 0.2f, moonY - moonR * 0.2f, moonR * 1.2f,
                    intArrayOf(Color.rgb(255, 254, 245), Color.rgb(225, 236, 255)),
                    null, Shader.TileMode.CLAMP
                )
            }
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            if (w <= 0f || h <= 0f) return

            canvas.drawRect(0f, 0f, w, h, skyPaint)
            if (transitionAlpha > 0.01f && prevSkyPaint.shader != null) {
                prevSkyPaint.alpha = (transitionAlpha * 255).toInt()
                canvas.drawRect(0f, 0f, w, h, prevSkyPaint)
            }

            val driftX = sin(phase) * w * 0.04f
            val driftY = cos(phase * 0.8f) * h * 0.03f
            canvas.save(); canvas.translate(driftX, driftY - parallaxY * 0.3f); canvas.drawRect(-w * 0.1f, -h * 0.1f, w * 1.1f, h * 1.1f, orb1Paint); canvas.restore()
            canvas.save(); canvas.translate(-driftX * 0.8f, -driftY * 0.8f); canvas.drawRect(-w * 0.1f, -h * 0.1f, w * 1.1f, h * 1.1f, orb2Paint); canvas.restore()

            val sceneSpec = getScene(activeEffectiveMode)
            if (sceneSpec.showMoon) {
                val moonX = w * 0.5f; val moonY = h * 0.15f - parallaxY * 0.35f; val moonR = 24f * resources.displayMetrics.density
                canvas.drawCircle(moonX, moonY, moonR, moonDiscPaint)
                canvas.drawCircle(moonX - moonR * 0.32f, moonY - moonR * 0.18f, moonR * 0.22f, craterPaint)
                canvas.drawCircle(moonX + moonR * 0.28f, moonY + moonR * 0.24f, moonR * 0.16f, craterPaint)
            }

            for (s in particles) {
                val wave = sin(phase * s.speed + s.offset) * cos(phase * s.speed * 0.6f + s.offset)
                val pulse = 0.3f + 0.7f * Math.abs(wave)
                val sx = s.xRatio * w; val sy = s.yRatio * h - parallaxY * 0.25f; val r = s.sizePx * (0.55f + 0.45f * pulse)

                when (sceneSpec.particleKind) {
                    "star" -> {
                        particlePaint.color = LauncherUtils.withAlpha(Color.WHITE, (pulse * 240).toInt())
                        starPath.reset(); val inner = r * 0.22f
                        starPath.moveTo(sx, sy - r); starPath.lineTo(sx + inner, sy - inner); starPath.lineTo(sx + r, sy)
                        starPath.lineTo(sx + inner, sy + inner); starPath.lineTo(sx, sy + r); starPath.lineTo(sx - inner, sy + inner)
                        starPath.lineTo(sx - r, sy); starPath.lineTo(sx - inner, sy - inner); starPath.close()
                        canvas.drawPath(starPath, particlePaint)
                    }
                    "rain" -> {
                        val dropY = ((s.yRatio + (phase * 0.15f * s.speed)) % 1.0f) * h
                        particlePaint.strokeWidth = LauncherUtils.dpF(context, 1.4f)
                        particlePaint.color = Color.argb(95, 195, 235, 255)
                        canvas.drawLine(sx, dropY, sx - 6f, dropY + 18f, particlePaint)
                    }
                    else -> {
                        particlePaint.color = Color.argb((pulse * 120).toInt(), 255, 255, 255)
                        canvas.drawCircle(sx, sy, r * 1.3f, particlePaint)
                    }
                }
            }

            if (sceneSpec.isLightSky) canvas.drawRect(0f, 0f, w, h, contrastVeilPaint)
        }

        override fun onDetachedFromWindow() { super.onDetachedFromWindow(); flowAnim?.cancel(); transitionAnim?.cancel() }
    }
}
