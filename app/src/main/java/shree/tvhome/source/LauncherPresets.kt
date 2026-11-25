package shree.tvhome.source

import android.view.View
import android.view.ViewGroup

object LauncherPresets {

    data class MoodPresetSpec(
        val key: String,
        val icon: String,
        val title: String,
        val tileTheme: String,
        val accentKey: String,
        val motionKey: String,
        val sceneKey: String,
        val gridLayout: String,
        val gridAnim: String
    )

    data class GridAnimSpec(
        val key: String,
        val label: String
    )

    // Single source of truth for One-Tap Mood Presets
    val MOOD_PRESETS = listOf(
        MoodPresetSpec(
            key = "cinema",
            icon = "🎬",
            title = "Cinema",
            tileTheme = "dark",
            accentKey = "sunset",
            motionKey = "silk",
            sceneKey = LauncherBackground.MODE_SUNSET,
            gridLayout = "cinema",
            gridAnim = "wave"
        ),
        MoodPresetSpec(
            key = "cyber",
            icon = "⚡",
            title = "Cyber",
            tileTheme = "cyber",
            accentKey = "magenta",
            motionKey = "jelly",
            sceneKey = LauncherBackground.MODE_PASTEL,
            gridLayout = "bento",
            gridAnim = "pop"
        ),
        MoodPresetSpec(
            key = "crystal",
            icon = "💎",
            title = "Crystal",
            tileTheme = "crystal",
            accentKey = "royal",
            motionKey = "levitate",
            sceneKey = LauncherBackground.MODE_MORNING,
            gridLayout = "classic",
            gridAnim = "rise"
        ),
        MoodPresetSpec(
            key = "emerald",
            icon = "🌿",
            title = "Emerald",
            tileTheme = "emerald",
            accentKey = "emerald",
            motionKey = "elastic",
            sceneKey = LauncherBackground.MODE_AQUA,
            gridLayout = "bento",
            gridAnim = "wave"
        ),
        MoodPresetSpec(
            key = "obsidian",
            icon = "👑",
            title = "Gold",
            tileTheme = "obsidian",
            accentKey = "sunset",
            motionKey = "flip3d",
            sceneKey = LauncherBackground.MODE_COSMOS,
            gridLayout = "cinema",
            gridAnim = "deck"
        )
    )

    // Single source of truth for App Drawer / Grid Entrance Animations
    val GRID_ANIMATIONS = listOf(
        GridAnimSpec("wave", "🌊 Wave"),
        GridAnimSpec("pop", "✨ Pop"),
        GridAnimSpec("rise", "💨 Rise"),
        GridAnimSpec("deck", "🃏 Deck")
    )

    fun applyMoodPreset(lp: LauncherPreferences, preset: MoodPresetSpec) {
        lp.motionStyle = preset.motionKey
        lp.gridLayoutMode = preset.gridLayout
        lp.gridAnimStyle = preset.gridAnim
        lp.prefs.edit().remove(LauncherPreferences.PREF_WALLPAPER).apply()
        lp.liveWallpaperMode = preset.sceneKey
        lp.accentColor = TileThemePalette.getAccentByName(preset.accentKey)
        lp.tileColor = preset.tileTheme
    }

    fun isPresetActive(lp: LauncherPreferences, preset: MoodPresetSpec): Boolean {
        return lp.tileColor == preset.tileTheme &&
            lp.accentColor == TileThemePalette.getAccentByName(preset.accentKey) &&
            lp.motionStyle == preset.motionKey &&
            lp.gridLayoutMode == preset.gridLayout &&
            lp.gridAnimStyle == preset.gridAnim
    }

    // Staggered entrance animation for App Drawer tiles (Favorites & All Apps)
    fun animateGridEntrance(container: ViewGroup, styleKey: String) {
        val count = container.childCount
        if (count == 0) return
        val d = container.resources.displayMetrics.density

        for (i in 0 until count) {
            val child = container.getChildAt(i)
            child.animate().cancel()
            val delay = (i.coerceAtMost(18) * 24L)

            when (styleKey) {
                "pop" -> {
                    child.alpha = 0f
                    child.scaleX = 0.78f
                    child.scaleY = 0.78f
                    child.animate()
                        .alpha(1f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delay)
                        .setDuration(340L)
                        .setInterpolator(LauncherUtils.FLUID_SPRING)
                        .start()
                }
                "deck" -> {
                    child.alpha = 0f
                    child.rotation = if (i % 2 == 0) -6f else 6f
                    child.translationX = 28f * d
                    child.animate()
                        .alpha(1f).rotation(0f).translationX(0f)
                        .setStartDelay(delay)
                        .setDuration(360L)
                        .setInterpolator(LauncherUtils.FLUID_SPRING)
                        .start()
                }
                "rise" -> {
                    child.alpha = 0f
                    child.translationY = 26f * d
                    child.animate()
                        .alpha(1f).translationY(0f)
                        .setStartDelay(delay)
                        .setDuration(340L)
                        .setInterpolator(LauncherUtils.SILKY_EASE)
                        .start()
                }
                else -> {
                    // "wave" cascade
                    child.alpha = 0f
                    child.scaleX = 0.90f
                    child.scaleY = 0.90f
                    child.translationY = 16f * d
                    child.animate()
                        .alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                        .setStartDelay(delay)
                        .setDuration(350L)
                        .setInterpolator(LauncherUtils.FLUID_SPRING)
                        .start()
                }
            }
        }
    }
}
