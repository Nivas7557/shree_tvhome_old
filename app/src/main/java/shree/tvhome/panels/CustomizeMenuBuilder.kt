package shree.tvhome.panels

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import shree.tvhome.source.LauncherBackground
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.LauncherUtils
import shree.tvhome.source.MotionEngine
import shree.tvhome.source.TileThemePalette

object CustomizeMenuBuilder {

    private data class MoodPresetSpec(
        val icon: String,
        val title: String,
        val tileTheme: String,
        val accentKey: String,
        val motionKey: String,
        val sceneKey: String
    )

    private val moodPresets = listOf(
        MoodPresetSpec("🎬", "Cinema", "dark", "sunset", "silk", LauncherBackground.MODE_SUNSET),
        MoodPresetSpec("⚡", "Cyber", "cyber", "magenta", "jelly", LauncherBackground.MODE_PASTEL),
        MoodPresetSpec("💎", "Crystal", "crystal", "royal", "levitate", LauncherBackground.MODE_MORNING)
    )

    private var activeSubMenuRow: LinearLayout? = null
    private var activeAnchorButton: View? = null

    fun isSubMenuOpen(): Boolean = activeSubMenuRow != null

    fun closeSubMenuWithAnimation() {
        val row = activeSubMenuRow ?: return
        activeSubMenuRow = null
        activeAnchorButton = null
        row.animate().alpha(0f).scaleY(0.85f).setDuration(180L).withEndAction {
            (row.parent as? LinearLayout)?.removeView(row)
        }.start()
    }

    fun closeSubMenuAndNavigate(moveDown: Boolean) {
        val row = activeSubMenuRow ?: return
        activeSubMenuRow = null
        activeAnchorButton = null
        (row.parent as? LinearLayout)?.removeView(row)
    }

    fun build(
        ctx: Context,
        panel: LinearLayout,
        lp: LauncherPreferences,
        onLiveRefresh: () -> Unit,
        onRefreshPanel: () -> Unit,
        onEditProfile: () -> Unit,
        onOpenWallpaper: () -> Unit,
        onOpenHidden: () -> Unit,
        onCloseDrawer: () -> Unit
    ) {
        activeSubMenuRow = null
        activeAnchorButton = null

        PanelUiKit.addSectionHeader(ctx, panel, lp, "ONE-TAP MOOD PRESETS")
        val moodRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        moodPresets.forEachIndexed { idx, preset ->
            val btn = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(ctx, 6), dp(ctx, 8), dp(ctx, 6), dp(ctx, 8))
                isFocusable = true
                isClickable = true
                background = PanelUiKit.glassCardBg(ctx, lp.accentColor, false)
                setOnFocusChangeListener { v, f ->
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.04f, 4f)
                    v.background = PanelUiKit.glassCardBg(ctx, lp.accentColor, f)
                }
                setOnClickListener {
                    lp.tileColor = preset.tileTheme
                    lp.accentColor = TileThemePalette.getAccentByName(preset.accentKey)
                    lp.motionStyle = preset.motionKey
                    lp.prefs.edit().remove(LauncherPreferences.PREF_WALLPAPER).apply()
                    lp.liveWallpaperMode = preset.sceneKey
                    onLiveRefresh()
                    onRefreshPanel()
                    Toast.makeText(ctx, "${preset.title} Mood Applied", Toast.LENGTH_SHORT).show()
                }
                addView(TextView(ctx).apply { text = preset.icon; textSize = 14f; gravity = Gravity.CENTER })
                addView(TextView(ctx).apply {
                    text = preset.title
                    textSize = 9.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                })
            }
            moodRow.addView(btn, LinearLayout.LayoutParams(0, -2, 1f).apply {
                if (idx < moodPresets.lastIndex) rightMargin = dp(ctx, 6)
            })
        }
        panel.addView(moodRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })

        PanelUiKit.addSectionHeader(ctx, panel, lp, "ACCOUNT & VISIBILITY")
        val topRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        topRow.addView(
            makeActionTile(ctx, lp, "👤 ", lp.userName, "Edit Profile & DP") { onEditProfile() },
            LinearLayout.LayoutParams(0, dp(ctx, 52), 1f).apply { rightMargin = dp(ctx, 6) }
        )

        val hiddenCount = lp.getHiddenApps().size
        topRow.addView(
            makeActionTile(ctx, lp, "👁 ", "Hidden Apps", "$hiddenCount hidden") {
                onCloseDrawer()
                onOpenHidden()
            },
            LinearLayout.LayoutParams(0, dp(ctx, 52), 1f)
        )
        panel.addView(topRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })

        val greetSub = lp.greetingPhrase.ifBlank { "Auto time-based greeting" }
        panel.addView(
            PanelUiKit.makeActionRow(ctx, lp, "💬", "Custom Greeting Status", greetSub) {
                showGreetingDialog(ctx, lp) {
                    onLiveRefresh()
                    onRefreshPanel()
                }
            },
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) }
        )

        panel.addView(
            PanelUiKit.makeActionRow(ctx, lp, "🖼", "Wallpaper & Live Effects", "Dynamic scenes, blur & gallery photos") {
                onOpenWallpaper()
            },
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 4) }
        )
        PanelUiKit.addSectionHeader(ctx, panel, lp, "APP NAMEPLATES")
        PanelUiKit.addSegmentedTrack(
            ctx, panel, lp,
            options = listOf("always" to "Always", "focus" to "On Focus", "hidden" to "Hidden"),
            getSelected = { lp.nameplateMode },
            onSelect = { key ->
                lp.nameplateMode = key
                onLiveRefresh()
            }
        )

        PanelUiKit.addSectionHeader(ctx, panel, lp, "TILE SIZE")
        PanelUiKit.addSegmentedTrack(
            ctx, panel, lp,
            options = TileThemePalette.TILE_SIZES.map { it.key to it.label },
            getSelected = { lp.tileSize },
            onSelect = { key ->
                lp.tileSize = key
                onLiveRefresh()
            }
        )

        PanelUiKit.addSectionHeader(ctx, panel, lp, "STAGE & PANEL WIDTH")
        PanelUiKit.addSegmentedTrack(
            ctx, panel, lp,
            options = TileThemePalette.PANEL_WIDTH_OPTIONS.map { pct -> pct.toString() to "$pct%" },
            getSelected = { lp.panelWidthPct.toString() },
            onSelect = { key ->
                lp.panelWidthPct = key.toIntOrNull() ?: TileThemePalette.PANEL_WIDTH_OPTIONS.first()
                onLiveRefresh()
            }
        )

        PanelUiKit.addSectionHeader(ctx, panel, lp, "ICON SHAPE")
        PanelUiKit.addSegmentedTrack(
            ctx, panel, lp,
            options = TileThemePalette.TILE_SHAPES.map { it.key to it.label },
            getSelected = { lp.tileShape },
            onSelect = { key ->
                lp.tileShape = key
                onLiveRefresh()
            }
        )

        PanelUiKit.addSectionHeader(ctx, panel, lp, "GLOBAL MOTION STYLE (TAP TO PREVIEW)")
        val allMotionButtons = ArrayList<Pair<String, TextView>>()
        MotionEngine.STYLES.chunked(4).forEach { rowChunk ->
            val r = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
            }
            rowChunk.forEachIndexed { idx, spec ->
                val isSel = lp.motionStyle == spec.key
                val btn = TextView(ctx).apply {
                    text = spec.label
                    textSize = 9.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    isFocusable = true
                    isClickable = true
                    background = PanelUiKit.glassCardBg(ctx, lp.accentColor, isSel)
                    setOnFocusChangeListener { v, f ->
                        MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 3f)
                        v.background = PanelUiKit.glassCardBg(ctx, lp.accentColor, f || lp.motionStyle == spec.key)
                    }
                    setOnClickListener {
                        lp.motionStyle = spec.key
                        allMotionButtons.forEach { (k, b) ->
                            b.background = PanelUiKit.glassCardBg(ctx, lp.accentColor, b.hasFocus() || k == lp.motionStyle)
                        }
                        val hostPanel = (panel.parent?.parent as? View) ?: panel
                        MotionEngine.playDemoPreview(hostPanel, spec.key)
                    }
                }
                allMotionButtons.add(spec.key to btn)
                r.addView(btn, LinearLayout.LayoutParams(0, dp(ctx, 34), 1f).apply {
                    if (idx < rowChunk.lastIndex) rightMargin = dp(ctx, 5)
                })
            }
            panel.addView(r, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 5) })
        }

        PanelUiKit.addSectionHeader(ctx, panel, lp, "ACCENT AURA")
        val swatchRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        val swatchViews = ArrayList<Pair<Int, View>>()
        TileThemePalette.ACCENTS.forEachIndexed { idx, spec ->
            val col = spec.color
            val isSel = lp.accentColor == col
            val dot = View(ctx).apply {
                isFocusable = true
                isClickable = true
                background = swatchBg(ctx, col, isSel)
                setOnFocusChangeListener { v, f ->
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.12f, 5f)
                    v.background = swatchBg(ctx, col, f || lp.accentColor == col)
                }
                setOnClickListener {
                    lp.accentColor = col
                    lp.accentName = spec.label
                    swatchViews.forEach { (c, v) ->
                        v.background = swatchBg(ctx, c, v.hasFocus() || c == lp.accentColor)
                    }
                    onLiveRefresh()
                }
            }
            swatchViews.add(col to dot)
            swatchRow.addView(dot, LinearLayout.LayoutParams(0, dp(ctx, 32), 1f).apply {
                if (idx < TileThemePalette.ACCENTS.lastIndex) rightMargin = dp(ctx, 6)
            })
        }
        panel.addView(swatchRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })

        PanelUiKit.addSectionHeader(ctx, panel, lp, "TILE GLASS PALETTE")
        TileThemePalette.THEMES.chunked(2).forEach { rowChunk ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
            }
            rowChunk.forEachIndexed { idx, theme ->
                val isSel = lp.tileColor == theme.key
                val card = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(ctx, 8), dp(ctx, 6), dp(ctx, 8), dp(ctx, 6))
                    isFocusable = true
                    isClickable = true
                    background = PanelUiKit.glassCardBg(ctx, lp.accentColor, isSel)
                    setOnFocusChangeListener { v, f ->
                        MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 4f)
                        v.background = PanelUiKit.glassCardBg(ctx, lp.accentColor, f || lp.tileColor == theme.key)
                    }
                    setOnClickListener {
                        lp.tileColor = theme.key
                        onLiveRefresh()
                        onRefreshPanel()
                    }
                    addView(
                        PanelUiKit.createThemeOrbView(ctx, theme.focusedColors, isSel),
                        LinearLayout.LayoutParams(dp(ctx, 18), dp(ctx, 18)).apply { rightMargin = dp(ctx, 7) }
                    )
                    addView(TextView(ctx).apply {
                        text = theme.name.substringBefore(" ")
                        textSize = 9.5f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(TileThemePalette.getPrimaryTextColor(lp.tileColor))
                        isSingleLine = true
                    })
                }
                row.addView(card, LinearLayout.LayoutParams(0, dp(ctx, 36), 1f).apply {
                    if (idx < rowChunk.lastIndex) rightMargin = dp(ctx, 6)
                })
            }
            panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 5) })
        }

        PanelUiKit.addSectionHeader(ctx, panel, lp, "FAVORITE BADGE")
        PanelUiKit.addSegmentedTrack(
            ctx, panel, lp,
            options = TileThemePalette.BADGE_MODES.map { it.key to it.label },
            getSelected = { lp.badgeMode },
            onSelect = { key ->
                lp.badgeMode = key
                onLiveRefresh()
            }
        )
    }

    private fun showGreetingDialog(ctx: Context, lp: LauncherPreferences, onSaved: () -> Unit) {
        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 22), dp(ctx, 18), dp(ctx, 22), dp(ctx, 18))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(20, 32, 62), Color.rgb(12, 18, 38))
            ).apply {
                cornerRadius = LauncherUtils.dpF(ctx, 20f)
                setStroke(dp(ctx, 1), Color.argb(120, 255, 255, 255))
            }
        }
        box.addView(TextView(ctx).apply {
            text = "Custom Greeting Phrase"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(ctx, 10))
        })
        val input = EditText(ctx).apply {
            isSingleLine = true
            hint = "Leave blank for Auto (e.g. Movie Night)"
            setText(lp.greetingPhrase)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.argb(150, 200, 220, 245))
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            background = GradientDrawable().apply {
                setColor(Color.argb(55, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(ctx, 12f)
                setStroke(dp(ctx, 1), lp.accentColor)
            }
        }
        box.addView(input, LinearLayout.LayoutParams(dp(ctx, 310), -2))
        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        btnRow.addView(
            PanelUiKit.makeActionRow(ctx, lp, "↺", "Auto Reset", "Time-based", "") {
                lp.greetingPhrase = ""
                onSaved()
                dialog.dismiss()
            },
            LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(ctx, 8) }
        )
        btnRow.addView(
            PanelUiKit.makeActionRow(ctx, lp, "✓", "Save", "Apply", "") {
                lp.greetingPhrase = input.text.toString().trim()
                onSaved()
                dialog.dismiss()
            },
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        box.addView(btnRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 12) })
        dialog.setContentView(box)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        dialog.show()
    }

    private fun makeActionTile(ctx: Context, lp: LauncherPreferences, icon: String, title: String, sub: String, onClick: () -> Unit) =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 10), dp(ctx, 6), dp(ctx, 10), dp(ctx, 6))
            isFocusable = true
            isClickable = true

            val titleTv = TextView(ctx).apply {
                text = "$icon$title"
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                isSingleLine = true
            }
            val subTv = TextView(ctx).apply {
                text = sub
                textSize = 8.2f
                isSingleLine = true
            }

            val syncColors = {
                background = PanelUiKit.glassCardBg(ctx, lp.accentColor, hasFocus(), lp.tileColor)
                titleTv.setTextColor(TileThemePalette.getPrimaryTextColor(lp.tileColor))
                val surfaceColors = TileThemePalette.getTheme(lp.tileColor).normalColors
                subTv.setTextColor(
                    if (TileThemePalette.isLightSurface(surfaceColors)) {
                        TileThemePalette.getAdaptiveTextColor(surfaceColors, secondary = true)
                    } else {
                        lp.accentColor
                    }
                )
            }
            syncColors()
            lp.addStyleListener(syncColors)

            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 4f)
                syncColors()
            }
            setOnClickListener { onClick() }
            addView(titleTv)
            addView(subTv)
        }
        
    private fun swatchBg(ctx: Context, col: Int, active: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(col)
        setStroke(dp(ctx, if (active) 3 else 1), if (active) Color.WHITE else Color.argb(110, 255, 255, 255))
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
