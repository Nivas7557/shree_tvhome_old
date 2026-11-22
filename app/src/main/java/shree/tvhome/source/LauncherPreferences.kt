package shree.tvhome.source

import android.content.Context
import android.content.SharedPreferences

class LauncherPreferences(context: Context) {
    
    private val styleListeners = java.util.concurrent.CopyOnWriteArrayList<() -> Unit>()

    // Register lightweight listeners so existing views repaint colors naturally without recreating layouts
    fun addStyleListener(listener: () -> Unit) {
        if (!styleListeners.contains(listener)) styleListeners.add(listener)
    }

    fun removeStyleListener(listener: () -> Unit) {
        styleListeners.remove(listener)
    }

    private fun notifyStyleChanged() {
        for (listener in styleListeners) listener()
    }
        
    companion object {
        const val PREFS = "launcher_preferences"
        const val PREF_ACCENT = "accent_color"
        const val PREF_ACCENT_NAME = "accent_name"
        const val PREF_TILE_SIZE = "tile_size"
        const val PREF_TILE_SHAPE = "tile_shape"
        const val PREF_TILE_COLOR = "tile_color"
        const val PREF_BADGE = "icon_badge"
        const val PREF_COLUMNS = "grid_columns"
        const val PREF_PANEL_WIDTH = "panel_width_pct"
        const val PREF_WALLPAPER = "custom_wallpaper_uri"
        const val PREF_FAVORITES = "favorites"
        const val PREF_FAVORITES_ORDER = "favorites_ordered_list"
        const val PREF_HIDDEN_APPS = "hidden_apps_set"
        const val PREF_USER_NAME = "launcher_user_name"
        const val PREF_GREETING_PHRASE = "launcher_greeting_phrase"
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

        // Display & Picture Mode Keys
        const val PREF_PICTURE_MODE = "picture_mode"
        const val PREF_CUSTOM_BRIGHTNESS = "custom_brightness_pct"
        const val PREF_CUSTOM_WARMTH = "custom_warmth_pct"

        // New Dynamic Upgrade Keys
        const val PREF_NAMEPLATE_MODE = "nameplate_mode"
        const val PREF_DND_ENABLED = "dnd_enabled"
        const val PREF_SLEEP_TIMER_END_MS = "sleep_timer_end_ms"
    }

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var accentColor: Int
        get() = prefs.getInt(PREF_ACCENT, TileThemePalette.ACCENTS.first().color)
        set(value) {
            prefs.edit().putInt(PREF_ACCENT, value).apply()
            notifyStyleChanged()
        }

    var accentName: String
        get() = prefs.getString(PREF_ACCENT_NAME, TileThemePalette.ACCENTS.first().label) ?: TileThemePalette.ACCENTS.first().label
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
        get() = prefs.getString(PREF_TILE_COLOR, TileThemePalette.THEMES.first().key) ?: TileThemePalette.THEMES.first().key
        set(value) {
            prefs.edit().putString(PREF_TILE_COLOR, value).apply()
            notifyStyleChanged()
        }

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

    // Dynamically resolve actual user-configured TV Device Name from Android Settings
    private val defaultDeviceOwner: String by lazy {
        val resolver = context.contentResolver
        val globalName = try {
            android.provider.Settings.Global.getString(resolver, android.provider.Settings.Global.DEVICE_NAME)
        } catch (_: Exception) { null }

        val btName = try {
            android.provider.Settings.Secure.getString(resolver, "bluetooth_name")
        } catch (_: Exception) { null }

        val manufacturer = android.os.Build.MANUFACTURER
            ?.replaceFirstChar { it.uppercase() }
            ?.takeIf { it.isNotBlank() }

        listOf(globalName, btName, manufacturer, "TV")
            .firstOrNull { !it.isNullOrBlank() && !it.equals("unknown", ignoreCase = true) }
            ?.trim() ?: "TV"
    }

    var userName: String
        get() = prefs.getString(PREF_USER_NAME, defaultDeviceOwner) ?: defaultDeviceOwner
        set(value) = prefs.edit().putString(PREF_USER_NAME, value.ifBlank { defaultDeviceOwner }).apply()
        
    var greetingPhrase: String
        get() = prefs.getString(PREF_GREETING_PHRASE, "") ?: ""
        set(value) = prefs.edit().putString(PREF_GREETING_PHRASE, value).apply()

    var sortOrder: String
        get() = prefs.getString(PREF_SORT_ORDER, "az") ?: "az"
        set(value) = prefs.edit().putString(PREF_SORT_ORDER, value).apply()

    var liveWallpaperMode: String
        get() = prefs.getString(PREF_LIVE_WP_MODE, LauncherBackground.MODE_SMART) ?: LauncherBackground.MODE_SMART
        set(value) = prefs.edit().putString(PREF_LIVE_WP_MODE, value).apply()

    var photoWeatherAura: Boolean
        get() = prefs.getBoolean(PREF_PHOTO_WEATHER_AURA, true)
        set(value) = prefs.edit().putBoolean(PREF_PHOTO_WEATHER_AURA, value).apply()

    var cachedWeatherCode: Int
        get() = prefs.getInt(PREF_CACHED_WEATHER_CODE, 0)
        set(value) = prefs.edit().putInt(PREF_CACHED_WEATHER_CODE, value).apply()

    var motionStyle: String
        get() = prefs.getString(PREF_MOTION_STYLE, MotionEngine.STYLES.first().key) ?: MotionEngine.STYLES.first().key
        set(value) = prefs.edit().putString(PREF_MOTION_STYLE, value).apply()

    var weatherCity: String
        get() = prefs.getString(PREF_WEATHER_CITY, "") ?: ""
        set(value) = prefs.edit().putString(PREF_WEATHER_CITY, value).apply()

    var profilePicUri: String?
        get() = prefs.getString(PREF_PROFILE_PIC_URI, null)
        set(value) = prefs.edit().putString(PREF_PROFILE_PIC_URI, value).apply()

    var profilePicZoom: Float
        get() = prefs.getFloat(PREF_PROFILE_PIC_ZOOM, 1.0f)
        set(value) = prefs.edit().putFloat(PREF_PROFILE_PIC_ZOOM, value).apply()

    var profilePicPanX: Float
        get() = prefs.getFloat(PREF_PROFILE_PIC_PAN_X, 0f)
        set(value) = prefs.edit().putFloat(PREF_PROFILE_PIC_PAN_X, value).apply()

    var profilePicPanY: Float
        get() = prefs.getFloat(PREF_PROFILE_PIC_PAN_Y, 0f)
        set(value) = prefs.edit().putFloat(PREF_PROFILE_PIC_PAN_Y, value).apply()

    var wpScaleMode: String
        get() = prefs.getString(PREF_WP_SCALE_MODE, "fit") ?: "fit"
        set(value) = prefs.edit().putString(PREF_WP_SCALE_MODE, value).apply()

    var wpZoom: Float
        get() = prefs.getFloat(PREF_WP_ZOOM, 1.0f)
        set(value) = prefs.edit().putFloat(PREF_WP_ZOOM, value).apply()

    var wpOffsetX: Int
        get() = prefs.getInt(PREF_WP_OFFSET_X, 0)
        set(value) = prefs.edit().putInt(PREF_WP_OFFSET_X, value).apply()

    var wpOffsetY: Int
        get() = prefs.getInt(PREF_WP_OFFSET_Y, 0)
        set(value) = prefs.edit().putInt(PREF_WP_OFFSET_Y, value).apply()

    var pictureMode: String
        get() = prefs.getString(PREF_PICTURE_MODE, "Standard") ?: "Standard"
        set(value) = prefs.edit().putString(PREF_PICTURE_MODE, value).apply()

    var customBrightnessPct: Int
        get() = prefs.getInt(PREF_CUSTOM_BRIGHTNESS, 80).coerceIn(10, 100)
        set(value) = prefs.edit().putInt(PREF_CUSTOM_BRIGHTNESS, value.coerceIn(10, 100)).apply()

    var customWarmthPct: Int
        get() = prefs.getInt(PREF_CUSTOM_WARMTH, 0).coerceIn(0, 75)
        set(value) = prefs.edit().putInt(PREF_CUSTOM_WARMTH, value.coerceIn(0, 75)).apply()

    // Nameplate modes: "always", "focus", "hidden"
    var nameplateMode: String
        get() = prefs.getString(PREF_NAMEPLATE_MODE, "always") ?: "always"
        set(value) = prefs.edit().putString(PREF_NAMEPLATE_MODE, value).apply()

    var dndEnabled: Boolean
        get() = prefs.getBoolean(PREF_DND_ENABLED, false)
        set(value) = prefs.edit().putBoolean(PREF_DND_ENABLED, value).apply()

    var sleepTimerEndMs: Long
        get() = prefs.getLong(PREF_SLEEP_TIMER_END_MS, 0L)
        set(value) = prefs.edit().putLong(PREF_SLEEP_TIMER_END_MS, value).apply()

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
