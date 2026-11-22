package shree.tvhome.panels

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.LauncherUtils
import shree.tvhome.source.MotionEngine
import shree.tvhome.source.TileThemePalette
import java.util.Locale

object CustomizeMenuBuilder {

    private var activeSubMenuRow: LinearLayout? = null
    private var activeAnchorButton: View? = null

    fun isSubMenuOpen(): Boolean = activeSubMenuRow != null

    fun closeSubMenuWithAnimation() {
        val row = activeSubMenuRow ?: return
        val anchor = activeAnchorButton
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

        addHeader(ctx, panel, "ACCOUNT & VISIBILITY")
        val topRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        topRow.addView(
            makeActionTile(ctx, lp, "👤 ", lp.userName.ifBlank { "Shree" }, "Edit Profile & DP") {
                onEditProfile()
            },
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

        addActionCard(ctx, panel, lp, "🖼", "Wallpaper & Live Effects", "Gallery photos, blur, dim & animations") {
            onOpenWallpaper()
        }

        // 1. TILE SIZE (Driven from TileThemePalette.TILE_SIZES)
        addHeader(ctx, panel, "TILE SIZE")
        addInPlaceChipRow(
            ctx, panel, lp,
            options = TileThemePalette.TILE_SIZES.map { it.key to it.label },
            getSelected = { lp.tileSize },
            onSelect = { key ->
                lp.tileSize = key
                onLiveRefresh()
            }
        )

        // 2. STAGE & PANEL WIDTH (Driven from TileThemePalette.PANEL_WIDTH_OPTIONS)
        addHeader(ctx, panel, "STAGE & PANEL WIDTH")
        addInPlaceChipRow(
            ctx, panel, lp,
            options = TileThemePalette.PANEL_WIDTH_OPTIONS.map { pct -> pct.toString() to "$pct%" },
            getSelected = { lp.panelWidthPct.toString() },
            onSelect = { key ->
                lp.panelWidthPct = key.toIntOrNull() ?: TileThemePalette.PANEL_WIDTH_OPTIONS.first()
                onLiveRefresh()
            }
        )

        // 3. ICON SHAPE (Driven from TileThemePalette.TILE_SHAPES)
        addHeader(ctx, panel, "ICON SHAPE")
        addInPlaceChipRow(
            ctx, panel, lp,
            options = TileThemePalette.TILE_SHAPES.map { it.key to it.label },
            getSelected = { lp.tileShape },
            onSelect = { key ->
                lp.tileShape = key
                onLiveRefresh()
            }
        )

        // 4. GLOBAL MOTION STYLE (Driven from MotionEngine.STYLES)
        addHeader(ctx, panel, "GLOBAL MOTION STYLE (TAP TO PREVIEW)")
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
                    background = chipBg(ctx, lp.accentColor, isSel)
                    setOnFocusChangeListener { v, f ->
                        MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 2f)
                        v.background = chipBg(ctx, lp.accentColor, f || lp.motionStyle == spec.key)
                    }
                    setOnClickListener {
                        lp.motionStyle = spec.key
                        allMotionButtons.forEach { (k, b) ->
                            b.background = chipBg(ctx, lp.accentColor, b.hasFocus() || k == lp.motionStyle)
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

        // 5. ACCENT AURA (Driven from TileThemePalette.ACCENTS)
        addHeader(ctx, panel, "ACCENT AURA")
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

        // 6. TILE GLASS PALETTE (Driven from TileThemePalette.THEMES)
        addHeader(ctx, panel, "TILE GLASS PALETTE")
        val themePairs = TileThemePalette.THEMES.map { spec ->
            spec.key to spec.name.substringBefore(" ")
        }
        themePairs.chunked(4).forEach { rowChunk ->
            addInPlaceChipRow(
                ctx, panel, lp,
                options = rowChunk,
                getSelected = { lp.tileColor },
                onSelect = { key ->
                    lp.tileColor = key
                    onLiveRefresh()
                }
            )
        }

        // 7. FAVORITE BADGE STYLE (Driven from TileThemePalette.BADGE_MODES)
        addHeader(ctx, panel, "FAVORITE BADGE")
        addInPlaceChipRow(
            ctx, panel, lp,
            options = TileThemePalette.BADGE_MODES.map { it.key to it.label },
            getSelected = { lp.badgeMode },
            onSelect = { key ->
                lp.badgeMode = key
                onLiveRefresh()
            }
        )
    }

    private fun addInPlaceChipRow(
        ctx: Context,
        panel: LinearLayout,
        lp: LauncherPreferences,
        options: List<Pair<String, String>>,
        getSelected: () -> String,
        onSelect: (String) -> Unit
    ) {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        val chips = ArrayList<Pair<String, TextView>>()
        options.forEachIndexed { idx, (key, label) ->
            val isSel = getSelected() == key
            val chip = TextView(ctx).apply {
                text = label
                textSize = 9.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                isFocusable = true
                isClickable = true
                background = chipBg(ctx, lp.accentColor, isSel)
                setOnFocusChangeListener { v, f ->
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.05f, 4f)
                    v.background = chipBg(ctx, lp.accentColor, f || getSelected() == key)
                }
                setOnClickListener {
                    onSelect(key)
                    chips.forEach { (k, cView) ->
                        cView.background = chipBg(ctx, lp.accentColor, cView.hasFocus() || getSelected() == k)
                    }
                }
            }
            chips.add(key to chip)
            row.addView(chip, LinearLayout.LayoutParams(0, dp(ctx, 34), 1f).apply {
                if (idx < options.lastIndex) rightMargin = dp(ctx, 5)
            })
        }
        panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 4) })
    }

    private fun makeActionTile(ctx: Context, lp: LauncherPreferences, icon: String, title: String, sub: String, onClick: () -> Unit) =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 10), dp(ctx, 6), dp(ctx, 10), dp(ctx, 6))
            isFocusable = true
            isClickable = true
            background = chipBg(ctx, lp.accentColor, false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 4f)
                v.background = chipBg(ctx, lp.accentColor, f)
            }
            setOnClickListener { onClick() }
            addView(TextView(ctx).apply {
                text = "$icon$title"
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                isSingleLine = true
            })
            addView(TextView(ctx).apply {
                text = sub
                textSize = 8.2f
                setTextColor(lp.accentColor)
                isSingleLine = true
            })
        }

    private fun addActionCard(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, icon: String, title: String, sub: String, onClick: () -> Unit) {
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9))
            isFocusable = true
            isClickable = true
            background = chipBg(ctx, lp.accentColor, false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f)
                v.background = chipBg(ctx, lp.accentColor, f)
            }
            setOnClickListener { onClick() }
        }
        card.addView(TextView(ctx).apply {
            text = icon
            textSize = 14f
            setTextColor(lp.accentColor)
            setPadding(0, 0, dp(ctx, 10), 0)
        })
        val mid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(TextView(ctx).apply { text = title; textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        mid.addView(TextView(ctx).apply { text = sub; textSize = 8.5f; setTextColor(Color.argb(185, 210, 230, 245)) })
        card.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(TextView(ctx).apply { text = "›"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        panel.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 4) })
    }

    private fun chipBg(ctx: Context, accent: Int, active: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (active) intArrayOf(LauncherUtils.withAlpha(accent, 165), LauncherUtils.withAlpha(accent, 95))
        else intArrayOf(Color.argb(52, 255, 255, 255), Color.argb(26, 255, 255, 255))
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 12f)
        setStroke(dp(ctx, if (active) 2 else 1), if (active) Color.WHITE else Color.argb(65, 255, 255, 255))
    }

    private fun swatchBg(ctx: Context, col: Int, active: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(col)
        setStroke(dp(ctx, if (active) 3 else 1), if (active) Color.WHITE else Color.argb(110, 255, 255, 255))
    }

    private fun addHeader(ctx: Context, panel: LinearLayout, label: String) {
        panel.addView(TextView(ctx).apply {
            text = label.uppercase(Locale.ROOT)
            textSize = 8.8f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.argb(210, 195, 220, 255))
            letterSpacing = 0.08f
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 8); bottomMargin = dp(ctx, 4) })
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
