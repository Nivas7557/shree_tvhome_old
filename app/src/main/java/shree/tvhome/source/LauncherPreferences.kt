package shree.tvhome.source

import android.content.Context
import android.content.SharedPreferences

class LauncherPreferences(context: Context) {

    companion object {
        const val PREF_SORT_ASC = "sort_ascending"
        const val PREF_SORT_ORDER = "sort_order"
        const val PREF_LIVE_WP_MODE = "live_wp_mode"
        const val PREF_PHOTO_WEATHER_AURA = "photo_weather_aura"
        const val PREF_CACHED_WEATHER_CODE = "cached_weather_code"
        const val PREF_MOTION_STYLE = "motion_style"
        const val PREF_WEATHER_CITY = "weather_city"

        // Profile Pic Keys
        const val PREF_PROFILE_PIC_URI = "profile_pic_uri"
        const val PREF_PROFILE_PIC_ZOOM = "profile_pic_zoom"
        const val PREF_PROFILE_PIC_PAN_X = "profile_pic_pan_x"
        const val PREF_PROFILE_PIC_PAN_Y = "profile_pic_pan_y"

        // Wallpaper Transform Keys
        const val PREF_WP_SCALE_MODE = "wp_scale_mode"
        const val PREF_WP_ZOOM = "wp_zoom"
        const val PREF_WP_OFFSET_X = "wp_offset_x"
        const val PREF_WP_OFFSET_Y = "wp_offset_y"

        // Weather Cache Keys
        const val PREF_CACHED_TEMP = "cached_temp"
        const val PREF_CACHED_COND = "cached_cond"
        const val PREF_CACHED_CITY = "cached_city"
        const val PREF_SAVED_LAT = "saved_lat"
        const val PREF_SAVED_LON = "saved_lon"
    }

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var accentColor: Int
        get() = prefs.getInt(PREF_ACCENT, TileThemePalette.ACCENTS.first().color)
        set(value) = prefs.edit().putInt(PREF_ACCENT, value).apply()

    var accentName: String
        get() = prefs.getString(PREF_ACCENT_NAME, TileThemePalette.ACCENTS.first().label)
            ?: TileThemePalette.ACCENTS.first().label
        set(value) = prefs.edit().putString(PREF_ACCENT_NAME, value).apply()

    var tileSize: String
        get() = prefs.getString(PREF_TILE_SIZE, TileThemePalette.getTileSizeSpec("medium").key)
            ?: TileThemePalette.getTileSizeSpec("medium").key
        set(value) = prefs.edit().putString(PREF_TILE_SIZE, value).apply()

    var tileShape: String
        get() = prefs.getString(PREF_TILE_SHAPE, TileThemePalette.TILE_SHAPES.first().key)
            ?: TileThemePalette.TILE_SHAPES.first().key
        set(value) = prefs.edit().putString(PREF_TILE_SHAPE, value).apply()

    var tileColor: String
        get() = prefs.getString(PREF_TILE_COLOR, TileThemePalette.THEMES.first().key)
            ?: TileThemePalette.THEMES.first().key
        set(value) = prefs.edit().putString(PREF_TILE_COLOR, value).apply()

    var badgeMode: String
        get() = prefs.getString(PREF_BADGE, TileThemePalette.BADGE_MODES.first().key)
            ?: TileThemePalette.BADGE_MODES.first().key
        set(value) = prefs.edit().putString(PREF_BADGE, value).apply()

    var columns: Int
        get() = prefs.getInt(PREF_COLUMNS, 6).coerceIn(TileThemePalette.GRID_COL_RANGE)
        set(value) = prefs.edit().putInt(PREF_COLUMNS, value.coerceIn(TileThemePalette.GRID_COL_RANGE)).apply()

    var panelWidthPct: Int
        get() = prefs.getInt(PREF_PANEL_WIDTH, TileThemePalette.PANEL_WIDTH_OPTIONS.getOrElse(1) { TileThemePalette.PANEL_WIDTH_OPTIONS.first() })
            .coerceIn(TileThemePalette.PANEL_WIDTH_OPTIONS.first(), TileThemePalette.PANEL_WIDTH_OPTIONS.last())
        set(value) = prefs.edit().putInt(
            PREF_PANEL_WIDTH,
            value.coerceIn(TileThemePalette.PANEL_WIDTH_OPTIONS.first(), TileThemePalette.PANEL_WIDTH_OPTIONS.last())
        ).apply()

    var userName: String
        get() = prefs.getString(PREF_USER_NAME, "Shree") ?: "Shree"
        set(value) = prefs.edit().putString(PREF_USER_NAME, value.ifBlank { "Shree" }).apply()

    var greetingPhrase: String
        get() = prefs.getString(PREF_GREETING_PHRASE, "") ?: ""
        set(value) = prefs.edit().putString(PREF_GREETING_PHRASE, value).apply()

    var isSortAscending: Boolean
        get() = prefs.getBoolean(PREF_SORT_ASC, true)
        set(value) = prefs.edit().putBoolean(PREF_SORT_ASC, value).apply()

    var sortOrder: String
        get() = prefs.getString("sort_order", "az") ?: "az"
        set(value) = prefs.edit().putString("sort_order", value).apply()

    var liveWallpaperMode: String
        get() = prefs.getString("live_wp_mode", LauncherBackground.MODE_SMART) ?: LauncherBackground.MODE_SMART
        set(value) = prefs.edit().putString("live_wp_mode", value).apply()

    var photoWeatherAura: Boolean
        get() = prefs.getBoolean("photo_weather_aura", true)
        set(value) = prefs.edit().putBoolean("photo_weather_aura", value).apply()

    var cachedWeatherCode: Int
        get() = prefs.getInt("cached_weather_code", 0)
        set(value) = prefs.edit().putInt("cached_weather_code", value).apply()

    var motionStyle: String
        get() = prefs.getString("motion_style", MotionEngine.STYLES.first().key) ?: MotionEngine.STYLES.first().key
        set(value) = prefs.edit().putString("motion_style", value).apply()

    var weatherCity: String
        get() = prefs.getString("weather_city", "") ?: ""
        set(value) = prefs.edit().putString("weather_city", value).apply()

    var userEmail: String
        get() = prefs.getString("user_email", "") ?: ""
        set(value) = prefs.edit().putString("user_email", value).apply()

    fun getOrderedFavorites(): MutableList<String> {
        val raw = prefs.getString(PREF_FAVORITES_ORDER, null)
        if (!raw.isNullOrEmpty()) {
            return raw.split(",").filter { it.isNotBlank() }.toMutableList()
        }
        val legacy = prefs.getStringSet(PREF_FAVORITES, emptySet()) ?: emptySet()
        return legacy.toMutableList()
    }

    fun getFavorites(): Set<String> = getOrderedFavorites().toSet()

    fun setFavorites(favs: Collection<String>) {
        val list = favs.distinct()
        prefs.edit()
            .putString(PREF_FAVORITES_ORDER, list.joinToString(","))
            .putStringSet(PREF_FAVORITES, list.toSet())
            .apply()
    }

    fun moveFavorite(pkg: String, delta: Int): Boolean {
        val list = getOrderedFavorites()
        val idx = list.indexOf(pkg)
        val target = idx + delta
        if (idx < 0 || target !in list.indices) return false
        list.removeAt(idx)
        list.add(target, pkg)
        setFavorites(list)
        return true
    }

    fun getHiddenApps(): MutableSet<String> {
        return HashSet(prefs.getStringSet(PREF_HIDDEN_APPS, emptySet()) ?: emptySet())
    }

    fun setHiddenApps(hidden: Set<String>) {
        prefs.edit().putStringSet(PREF_HIDDEN_APPS, HashSet(hidden)).apply()
    }

    fun getAccentColorValue(name: String?): Int = TileThemePalette.getAccentByName(name)
}
