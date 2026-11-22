package shree.tvhome.source

import android.content.Context
import android.graphics.Color
import android.net.wifi.WifiManager

object TileThemePalette {

    data class ThemeSpec(
        val key: String,
        val name: String,
        val subtitle: String,
        val normalColors: IntArray,
        val focusedColors: IntArray
    )

    data class AccentSpec(
        val key: String,
        val label: String,
        val color: Int
    )

    data class TileSizeSpec(
        val key: String,
        val label: String,
        val badgeDp: Int,
        val desiredGridDp: Int,
        val hiddenScale: Float
    )

    data class OptionSpec(
        val key: String,
        val label: String
    )

    val THEMES = listOf(
        ThemeSpec(
            key = "frost",
            name = "Royal Indigo",
            subtitle = "Deep Sapphire Glass",
            normalColors = intArrayOf(Color.argb(215, 28, 52, 118), Color.argb(195, 14, 28, 72), Color.argb(210, 44, 76, 158)),
            focusedColors = intArrayOf(Color.argb(245, 58, 108, 215), Color.argb(230, 28, 62, 145), Color.argb(245, 88, 145, 255))
        ),
        ThemeSpec(
            key = "ocean",
            name = "Ocean Breeze",
            subtitle = "Electric Cyan & Sea",
            normalColors = intArrayOf(Color.argb(215, 10, 92, 165), Color.argb(195, 6, 48, 105), Color.argb(210, 18, 132, 205)),
            focusedColors = intArrayOf(Color.argb(245, 22, 155, 245), Color.argb(230, 10, 88, 178), Color.argb(245, 48, 195, 255))
        ),
        ThemeSpec(
            key = "cyber",
            name = "Cyber Neon",
            subtitle = "Violet & Magenta Glow",
            normalColors = intArrayOf(Color.argb(215, 88, 24, 158), Color.argb(195, 42, 14, 96), Color.argb(210, 148, 32, 178)),
            focusedColors = intArrayOf(Color.argb(245, 155, 52, 245), Color.argb(230, 82, 24, 168), Color.argb(245, 225, 65, 215))
        ),
        ThemeSpec(
            key = "sunset",
            name = "Sunset Glow",
            subtitle = "Crimson & Amber",
            normalColors = intArrayOf(Color.argb(215, 155, 34, 82), Color.argb(195, 92, 18, 54), Color.argb(210, 198, 62, 78)),
            focusedColors = intArrayOf(Color.argb(245, 235, 68, 108), Color.argb(230, 165, 34, 78), Color.argb(245, 255, 128, 72))
        ),
        ThemeSpec(
            key = "emerald",
            name = "Emerald Forest",
            subtitle = "Deep Jade & Mint",
            normalColors = intArrayOf(Color.argb(215, 14, 118, 88), Color.argb(195, 8, 66, 52), Color.argb(210, 22, 148, 112)),
            focusedColors = intArrayOf(Color.argb(245, 24, 185, 138), Color.argb(230, 12, 115, 86), Color.argb(245, 52, 225, 175))
        ),
        ThemeSpec(
            key = "dark",
            name = "Slate Dark",
            subtitle = "Obsidian Graphite",
            normalColors = intArrayOf(Color.argb(225, 26, 32, 48), Color.argb(215, 14, 18, 28), Color.argb(225, 38, 46, 68)),
            focusedColors = intArrayOf(Color.argb(245, 52, 64, 92), Color.argb(235, 28, 36, 54), Color.argb(245, 74, 88, 125))
        ),
        ThemeSpec(
            key = "crystal",
            name = "Frosted Crystal",
            subtitle = "Bright Ice Glass",
            normalColors = intArrayOf(Color.argb(215, 215, 232, 252), Color.argb(195, 182, 208, 242), Color.argb(210, 228, 242, 255)),
            focusedColors = intArrayOf(Color.argb(245, 240, 248, 255), Color.argb(230, 205, 228, 255), Color.argb(245, 255, 255, 255))
        ),
        ThemeSpec(
            key = "obsidian",
            name = "Golden Obsidian",
            subtitle = "Royal Black & Gold",
            normalColors = intArrayOf(Color.argb(225, 34, 28, 18), Color.argb(215, 16, 14, 12), Color.argb(225, 58, 46, 24)),
            focusedColors = intArrayOf(Color.argb(245, 96, 76, 34), Color.argb(235, 42, 34, 20), Color.argb(245, 148, 115, 44))
        )
    )

    val ACCENTS = listOf(
        AccentSpec("royal", "Royal", Color.rgb(92, 184, 255)),
        AccentSpec("emerald", "Mint", Color.rgb(46, 232, 138)),
        AccentSpec("magenta", "Pink", Color.rgb(255, 110, 145)),
        AccentSpec("purple", "Purple", Color.rgb(185, 135, 255)),
        AccentSpec("sunset", "Gold", Color.rgb(255, 195, 75)),
        AccentSpec("cyan", "Aqua", Color.rgb(75, 235, 245))
    )

    val TILE_SIZES = listOf(
        TileSizeSpec("small", "Small", badgeDp = 64, desiredGridDp = 86, hiddenScale = 0.75f),
        TileSizeSpec("medium", "Medium", badgeDp = 78, desiredGridDp = 104, hiddenScale = 0.85f),
        TileSizeSpec("large", "Large", badgeDp = 94, desiredGridDp = 124, hiddenScale = 0.96f)
    )

    val TILE_SHAPES = listOf(
        OptionSpec("squircle", "◐ Squircle"),
        OptionSpec("circle", "● Circle"),
        OptionSpec("heart", "♥ Heart"),
        OptionSpec("classic", "■ Classic")
    )

    val BADGE_MODES = listOf(
        OptionSpec("accent", "● Dot"),
        OptionSpec("heart", "♥ Heart"),
        OptionSpec("star", "★ Star"),
        OptionSpec("none", "Off")
    )

    val PANEL_WIDTH_OPTIONS = listOf(20, 25, 30, 35)
    const val PANEL_MIN_WIDTH_DP = 165
    val GRID_COL_RANGE = 3..8

    private val themeMap by lazy { THEMES.associateBy { it.key } }
    private val sizeMap by lazy { TILE_SIZES.associateBy { it.key } }
    private val accentMap by lazy { ACCENTS.associateBy { it.key.lowercase() } }

    fun getTheme(key: String?): ThemeSpec = themeMap[key] ?: THEMES.first()

    // Compute relative luminance dynamically from any theme color array without hardcoded theme checks
    private fun computeAverageLuminance(colors: IntArray): Float {
        if (colors.isEmpty()) return 0f
        var sum = 0f
        for (c in colors) {
            val r = Color.red(c) / 255f
            val g = Color.green(c) / 255f
            val b = Color.blue(c) / 255f
            sum += (0.2126f * r + 0.7152f * g + 0.0722f * b)
        }
        return sum / colors.size
    }

    fun isLightSurface(colors: IntArray): Boolean = computeAverageLuminance(colors) > 0.45f

    // Derive optimal high-contrast font color mathematically from the surface's own hue & luminance
    fun getAdaptiveTextColor(colors: IntArray, secondary: Boolean = false): Int {
        val sample = colors.firstOrNull() ?: return Color.WHITE
        val hsv = FloatArray(3)
        Color.colorToHSV(sample, hsv)
        val lightBg = isLightSurface(colors)
        return if (lightBg) {
            // Deep harmonic shade of the background's own hue for light surfaces
            hsv[1] = (hsv[1] * 0.85f).coerceIn(0.25f, 0.85f)
            hsv[2] = if (secondary) 0.32f else 0.14f
            Color.HSVToColor(if (secondary) 215 else 255, hsv)
        } else {
            // Crisp high-luminance harmonic tint of the background's hue for dark/jewel surfaces
            hsv[1] = (hsv[1] * 0.12f).coerceIn(0f, 0.14f)
            hsv[2] = if (secondary) 0.88f else 0.99f
            Color.HSVToColor(if (secondary) 205 else 255, hsv)
        }
    }

    fun getPrimaryTextColor(themeKey: String?): Int =
        getAdaptiveTextColor(getTheme(themeKey).normalColors, secondary = false)

    fun getSecondaryTextColor(themeKey: String?): Int =
        getAdaptiveTextColor(getTheme(themeKey).normalColors, secondary = true)

    fun getTopBottomColors(themeKey: String?, focused: Boolean): IntArray {
        val spec = getTheme(themeKey)
        return if (focused) spec.focusedColors else spec.normalColors
    }

    fun getGlassPanelColors(themeKey: String?, accentColor: Int, elevated: Boolean = false): IntArray {
        val base = getTheme(themeKey).normalColors
        val topCol = base.firstOrNull() ?: Color.rgb(22, 34, 72)
        val botCol = base.lastOrNull() ?: topCol
        val alphaBoost = if (elevated) 242 else 220
        val c1 = LauncherUtils.withAlpha(topCol, alphaBoost)
        val c2 = LauncherUtils.withAlpha(botCol, (alphaBoost + 8).coerceAtMost(252))
        val c3 = LauncherUtils.blendWithAccent(botCol, accentColor, if (elevated) 0.22f else 0.14f, alphaBoost)
        return intArrayOf(c1, c2, c3)
    }

    fun getTileSizeSpec(key: String?): TileSizeSpec = sizeMap[key] ?: sizeMap["medium"] ?: TILE_SIZES.first()

    fun getAccentByName(name: String?): Int = accentMap[name?.lowercase()]?.color ?: ACCENTS.first().color

    fun getConnectedSsid(ctx: Context): String {
        return try {
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val raw = wm?.connectionInfo?.ssid?.replace("\"", "") ?: ""
            if (raw.isNotEmpty() && raw != "<unknown ssid>") raw else "Wi-Fi"
        } catch (_: Exception) {
            "Wi-Fi"
        }
    }
}
