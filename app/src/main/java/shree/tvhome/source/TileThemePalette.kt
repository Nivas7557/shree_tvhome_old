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
            subtitle = "Glossy & Deep",
            normalColors = intArrayOf(Color.argb(145, 26, 40, 82), Color.argb(100, 16, 26, 56), Color.argb(130, 38, 56, 108)),
            focusedColors = intArrayOf(Color.argb(210, 58, 92, 168), Color.argb(155, 32, 54, 112), Color.argb(195, 78, 118, 205))
        ),
        ThemeSpec(
            key = "ocean",
            name = "Ocean Breeze",
            subtitle = "Cyan & Deep Sea",
            normalColors = intArrayOf(Color.argb(150, 12, 58, 120), Color.argb(110, 6, 36, 82), Color.argb(135, 16, 86, 155)),
            focusedColors = intArrayOf(Color.argb(215, 22, 115, 215), Color.argb(165, 10, 65, 145), Color.argb(200, 32, 155, 245))
        ),
        ThemeSpec(
            key = "cyber",
            name = "Cyber Neon",
            subtitle = "Electric & Cyber",
            normalColors = intArrayOf(Color.argb(155, 58, 22, 115), Color.argb(115, 32, 12, 72), Color.argb(145, 88, 28, 155)),
            focusedColors = intArrayOf(Color.argb(220, 135, 52, 235), Color.argb(175, 78, 24, 165), Color.argb(215, 185, 72, 255))
        ),
        ThemeSpec(
            key = "sunset",
            name = "Sunset Glow",
            subtitle = "Crimson & Amber",
            normalColors = intArrayOf(Color.argb(155, 120, 32, 68), Color.argb(115, 78, 18, 46), Color.argb(145, 155, 48, 72)),
            focusedColors = intArrayOf(Color.argb(225, 225, 78, 108), Color.argb(175, 155, 38, 75), Color.argb(215, 255, 115, 92))
        ),
        ThemeSpec(
            key = "emerald",
            name = "Emerald Forest",
            subtitle = "Deep Jungle & Mint",
            normalColors = intArrayOf(Color.argb(150, 14, 78, 62), Color.argb(110, 8, 48, 38), Color.argb(135, 22, 105, 82)),
            focusedColors = intArrayOf(Color.argb(220, 26, 158, 120), Color.argb(175, 14, 105, 78), Color.argb(215, 48, 205, 155))
        ),
        ThemeSpec(
            key = "dark",
            name = "Slate Dark",
            subtitle = "Graphite & Matte",
            normalColors = intArrayOf(Color.argb(180, 18, 22, 34), Color.argb(160, 10, 14, 22), Color.argb(175, 26, 30, 44)),
            focusedColors = intArrayOf(Color.argb(230, 38, 46, 68), Color.argb(200, 22, 28, 44), Color.argb(225, 52, 62, 92))
        ),
        ThemeSpec(
            key = "crystal",
            name = "Frosted Crystal",
            subtitle = "Vision Pure Glass",
            normalColors = intArrayOf(Color.argb(95, 235, 245, 255), Color.argb(55, 185, 215, 250), Color.argb(85, 220, 240, 255)),
            focusedColors = intArrayOf(Color.argb(155, 255, 255, 255), Color.argb(105, 210, 235, 255), Color.argb(145, 240, 250, 255))
        ),
        ThemeSpec(
            key = "obsidian",
            name = "Golden Obsidian",
            subtitle = "Luxury Black & Gold",
            normalColors = intArrayOf(Color.argb(195, 18, 16, 14), Color.argb(175, 10, 10, 12), Color.argb(190, 34, 28, 18)),
            focusedColors = intArrayOf(Color.argb(235, 68, 54, 26), Color.argb(210, 28, 24, 18), Color.argb(230, 115, 88, 36))
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
    // Allow 20% panel width on all TV screens without clamping to 25%
    const val PANEL_MIN_WIDTH_DP = 165
    val GRID_COL_RANGE = 3..8

    private val themeMap by lazy { THEMES.associateBy { it.key } }
    private val sizeMap by lazy { TILE_SIZES.associateBy { it.key } }
    private val accentMap by lazy { ACCENTS.associateBy { it.key.lowercase() } }

    fun getTheme(key: String?): ThemeSpec = themeMap[key] ?: THEMES.first()

    fun getTopBottomColors(themeKey: String?, focused: Boolean): IntArray {
        val spec = getTheme(themeKey)
        return if (focused) spec.focusedColors else spec.normalColors
    }

    // Global glass surface gradient applied across SideDrawer, AmbientStage, and Panel Cards
    fun getGlassPanelColors(themeKey: String?, accentColor: Int, elevated: Boolean = false): IntArray {
        val base = getTheme(themeKey).normalColors
        val alphaBoost = if (elevated) 238 else 210
        val c1 = LauncherUtils.withAlpha(base[0], alphaBoost)
        val c2 = LauncherUtils.withAlpha(base[1], (alphaBoost + 12).coerceAtMost(250))
        val c3 = LauncherUtils.blendWithAccent(base[2], accentColor, if (elevated) 0.24f else 0.16f, alphaBoost)
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
