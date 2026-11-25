package shree.tvhome.panels

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import shree.tvhome.source.LauncherBackground
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.LauncherUtils

object WallpaperEditorPanel {
    private var lastFocusedTag = ""

    fun build(
        ctx: Context,
        panel: LinearLayout,
        lp: LauncherPreferences,
        onLiveUpdate: () -> Unit,
        onPickNewImage: () -> Unit,
        onDone: () -> Unit
    ) {
        panel.clipChildren = false
        panel.clipToPadding = false
        val hasCustomImage = lp.prefs.getString(LauncherPreferences.PREF_WALLPAPER, null) != null
        val curMode = lp.liveWallpaperMode

        addSection(ctx, panel, "SMART DYNAMIC MODES")
        LauncherBackground.SMART_MODES.chunked(2).forEach { modePair ->
            val autoRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
            }
            modePair.forEachIndexed { idx, spec ->
                val tagStr = "wp_${spec.key}"
                autoRow.addView(
                    makeVisualCard(
                        ctx, lp, tagStr, spec.icon, spec.name, spec.subtitle,
                        spec.swatch,
                        !hasCustomImage && curMode == spec.key
                    ) {
                        lastFocusedTag = tagStr
                        lp.prefs.edit().remove(LauncherPreferences.PREF_WALLPAPER).apply()
                        lp.liveWallpaperMode = spec.key
                        onLiveUpdate()
                        rebuild(ctx, panel, lp, onLiveUpdate, onPickNewImage, onDone)
                    },
                    LinearLayout.LayoutParams(0, dp(ctx, 54), 1f).apply {
                        if (idx < modePair.lastIndex) rightMargin = dp(ctx, 7)
                    }
                )
            }
            panel.addView(autoRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })
        }

        addSection(ctx, panel, "LIVE CANVAS SCENES")
        LauncherBackground.SCENES.chunked(2).forEach { scenePair ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
            }
            scenePair.forEachIndexed { idx, spec ->
                val tagStr = "wp_${spec.key}"
                val sel = !hasCustomImage && curMode == spec.key
                val card = makeVisualCard(
                    ctx, lp, tagStr, spec.icon, spec.name,
                    if (sel) "Active" else "Tap to set", spec.swatch, sel
                ) {
                    lastFocusedTag = tagStr
                    lp.prefs.edit().remove(LauncherPreferences.PREF_WALLPAPER).apply()
                    lp.liveWallpaperMode = spec.key
                    onLiveUpdate()
                    rebuild(ctx, panel, lp, onLiveUpdate, onPickNewImage, onDone)
                }
                val p = LinearLayout.LayoutParams(0, dp(ctx, 50), 1f).apply {
                    if (idx < scenePair.lastIndex) rightMargin = dp(ctx, 7)
                }
                row.addView(card, p)
            }
            panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 7) })
        }

        addSection(ctx, panel, "CUSTOM GALLERY PHOTO")
        val pickCard = makeVisualCard(
            ctx, lp, "wp_pick", "🖼", "Choose Photo from Storage",
            if (hasCustomImage) "Custom photo active • Tap to change" else "Select 4K / HD image",
            intArrayOf(Color.argb(85, 255, 255, 255), Color.argb(38, 255, 255, 255)),
            hasCustomImage
        ) {
            lastFocusedTag = "wp_pick"
            onPickNewImage()
        }
        panel.addView(pickCard, LinearLayout.LayoutParams(-1, dp(ctx, 50)).apply { bottomMargin = dp(ctx, 8) })

        if (hasCustomImage) {
            val auraOn = lp.photoWeatherAura
            val auraCard = makeVisualCard(
                ctx, lp, "wp_aura", if (auraOn) "🌦️" else "🚫",
                "Subtle Corner Weather Aura",
                if (auraOn) "Enabled • Soft edge glow (Tap to Off)" else "Disabled • Pure photo (Tap to On)",
                intArrayOf(Color.argb(80, 68, 165, 235), Color.argb(40, 32, 52, 95)),
                auraOn
            ) {
                lastFocusedTag = "wp_aura"
                lp.photoWeatherAura = !auraOn
                onLiveUpdate()
                rebuild(ctx, panel, lp, onLiveUpdate, onPickNewImage, onDone)
            }
            panel.addView(auraCard, LinearLayout.LayoutParams(-1, dp(ctx, 50)).apply { bottomMargin = dp(ctx, 8) })

            addSection(ctx, panel, "PHOTO FIT & ZOOM")
            val curScaleMode = lp.wpScaleMode
            addPillRow(ctx, panel, lp, "fit", listOf("Fit Screen", "Fill Screen"), listOf(curScaleMode == "fit", curScaleMode == "width")) { idx ->
                lp.wpScaleMode = if (idx == 0) "fit" else "width"
                onLiveUpdate()
                rebuild(ctx, panel, lp, onLiveUpdate, onPickNewImage, onDone)
            }
            addPillRow(ctx, panel, lp, "zoom", listOf("Zoom -", "Reset", "Zoom +"), listOf(false, false, false)) { idx ->
                var z = lp.wpZoom
                z = when (idx) { 0 -> maxOf(1.0f, z - 0.1f); 1 -> 1.08f; else -> minOf(2.2f, z + 0.1f) }
                lp.wpZoom = z
                onLiveUpdate()
            }
            addPillRow(ctx, panel, lp, "pos", listOf("⬅", "⬆", "⬇", "➡"), listOf(false, false, false, false)) { idx ->
                var ox = lp.wpOffsetX
                var oy = lp.wpOffsetY
                val step = dp(ctx, 24)
                when (idx) { 0 -> ox -= step; 1 -> oy -= step; 2 -> oy += step; else -> ox += step }
                lp.wpOffsetX = ox
                lp.wpOffsetY = oy
                onLiveUpdate()
            }
        }

        val donePill = TextView(ctx).apply {
            tag = "wp_done"
            text = "✓  Apply & Back"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            background = pillBg(ctx, false, true, lp.accentColor)
            setOnFocusChangeListener { v, f ->
                LauncherUtils.applyFocusSpring(v, f, 1.02f, 3f)
                v.background = pillBg(ctx, f, true, lp.accentColor)
            }
            setOnClickListener { lastFocusedTag = ""; onDone() }
        }
        panel.addView(donePill, LinearLayout.LayoutParams(-1, dp(ctx, 42)).apply { topMargin = dp(ctx, 10); bottomMargin = dp(ctx, 36) })
    }

    private fun rebuild(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onLive: () -> Unit, onPick: () -> Unit, onDone: () -> Unit) {
        panel.removeAllViews()
        build(ctx, panel, lp, onLive, onPick, onDone)
    }

    private fun makeVisualCard(
        ctx: Context, lp: LauncherPreferences, tagStr: String, icon: String, title: String, sub: String,
        gradColors: IntArray, selected: Boolean, onClick: () -> Unit
    ) = LinearLayout(ctx).apply {
        tag = tagStr
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(ctx, 10), dp(ctx, 6), dp(ctx, 10), dp(ctx, 6))
        isFocusable = true
        background = cardBg(ctx, false, selected, gradColors, lp.accentColor)
        setOnFocusChangeListener { v, f ->
            LauncherUtils.applyFocusSpring(v, f, 1.02f, 4f)
            v.background = cardBg(ctx, f, selected, gradColors, lp.accentColor)
        }
        setOnClickListener { onClick() }

        addView(TextView(ctx).apply { text = icon; textSize = 15f; setPadding(0, 0, dp(ctx, 8), 0) })
        val mid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(TextView(ctx).apply { text = title; textSize = 9.5f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); isSingleLine = true })
        mid.addView(TextView(ctx).apply { text = sub; textSize = 7.8f; setTextColor(Color.argb(210, 235, 245, 255)); isSingleLine = true })
        addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        if (selected) addView(TextView(ctx).apply { text = "✓"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
    }

    private fun addPillRow(
        ctx: Context, panel: LinearLayout, lp: LauncherPreferences, prefix: String,
        labels: List<String>, selectedFlags: List<Boolean>, onClick: (Int) -> Unit
    ) {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        labels.forEachIndexed { i, txt ->
            val tagStr = "wp_${prefix}_$i"
            val sel = selectedFlags.getOrElse(i) { false }
            val pill = TextView(ctx).apply {
                tag = tagStr
                text = txt
                textSize = 9.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                isFocusable = true
                background = pillBg(ctx, false, sel, lp.accentColor)
                setOnFocusChangeListener { v, f ->
                    LauncherUtils.applyFocusSpring(v, f, 1.03f, 3f)
                    v.background = pillBg(ctx, f, sel, lp.accentColor)
                }
                setOnClickListener { lastFocusedTag = tagStr; onClick(i) }
            }
            row.addView(pill, LinearLayout.LayoutParams(0, dp(ctx, 36), 1f).apply { if (i < labels.lastIndex) rightMargin = dp(ctx, 6) })
        }
        panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 7) })
    }

    private fun cardBg(ctx: Context, focused: Boolean, selected: Boolean, colors: IntArray, accent: Int) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR, colors
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 14f)
        setStroke(dp(ctx, if (focused || selected) 2 else 1), when {
            focused -> Color.WHITE
            selected -> accent
            else -> Color.argb(85, 255, 255, 255)
        })
    }

    private fun pillBg(ctx: Context, focused: Boolean, selected: Boolean, accent: Int) = GradientDrawable().apply {
        setColor(when { focused -> Color.argb(120, 255, 255, 255); selected -> LauncherUtils.withAlpha(accent, 130); else -> Color.argb(45, 255, 255, 255) })
        cornerRadius = LauncherUtils.dpF(ctx, 12f)
        setStroke(dp(ctx, if (focused || selected) 2 else 1), if (focused) Color.WHITE else accent)
    }

    private fun addSection(ctx: Context, panel: LinearLayout, label: String) {
        val t = TextView(ctx).apply {
            text = label
            textSize = 8.5f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            setTextColor(Color.rgb(110, 215, 255))
        }
        panel.addView(t, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 8); bottomMargin = dp(ctx, 5) })
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
