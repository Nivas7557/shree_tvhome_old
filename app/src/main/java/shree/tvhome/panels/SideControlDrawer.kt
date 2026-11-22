package shree.tvhome.panels

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import shree.tvhome.FocusTrap
import shree.tvhome.design.AppGridManager
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.LauncherUtils
import shree.tvhome.source.MotionEngine
import shree.tvhome.source.TileThemePalette
import shree.tvhome.source.TvNotificationService
import java.util.Locale

class SideControlDrawer(
    private val context: Context,
    private val drawerLayout: LinearLayout,
    private val scrim: View,
    private val mainPage: View,
    private val lp: LauncherPreferences,
    private val onLiveRefresh: () -> Unit,
    private val onWallpaperRefresh: () -> Unit,
    private val onPickWallpaper: () -> Unit,
    private val onPickProfilePic: () -> Unit,
    private val onOpenHiddenRibbon: () -> Unit
) {
    companion object {
        const val MODE_CUSTOMIZE = "customize"
        const val MODE_QUICK = "quick"
        const val MODE_WIFI = "wifi"
        const val MODE_ALERTS = "alerts"
        const val MODE_POWER = "power"
        const val MODE_WALLPAPER_EDIT = "wp_edit"
        const val MODE_SUB_INPUTS = "sub_inputs"
        const val MODE_SUB_PICTURE = "sub_picture"
        const val MODE_SUB_SOUND = "sub_sound"
        const val MODE_SUB_BLUETOOTH = "sub_bluetooth"
        const val MODE_SUB_STORAGE = "sub_storage"
    }

    private data class DrawerHeaderSpec(
        val mode: String,
        val icon: String,
        val title: String,
        val navTag: String,
        val render: (LinearLayout) -> Unit
    )

    // Single data-driven registry for all drawer panels without hardcoded when-routing
    private val drawerHeaders: List<DrawerHeaderSpec> by lazy {
        listOf(
            DrawerHeaderSpec(MODE_QUICK, "⚙  ", "Quick Settings", "nav_quick") { panel ->
                SystemSubPanels.buildQuickSettingsMain(context, panel, lp) { targetMode -> open(targetMode) }
            },
            DrawerHeaderSpec(MODE_CUSTOMIZE, "✦  ", "Customize Launcher", "nav_customize") { panel ->
                CustomizeMenuBuilder.build(
                    context, panel, lp, onLiveRefresh, { populateActivePanel() },
                    ::showProfileEditDialog,
                    { open(MODE_WALLPAPER_EDIT) },
                    onOpenHiddenRibbon,
                    ::close
                )
            },
            DrawerHeaderSpec(MODE_WIFI, "📶  ", "Network & Internet", "nav_wifi") { panel ->
                WifiPanelBuilder.build(context, panel, lp) { populateActivePanel() }
            },
            DrawerHeaderSpec(MODE_ALERTS, "🔔  ", "Alerts & Notifications", "nav_alerts") { panel ->
                buildRealAlertsPanel(panel)
            },
            DrawerHeaderSpec(MODE_POWER, "⏻  ", "Power & System", "nav_power") { panel ->
                buildPowerPanel(panel)
            },
            DrawerHeaderSpec(MODE_WALLPAPER_EDIT, "🖼  ", "Wallpaper Studio", "nav_customize") { panel ->
                WallpaperEditorPanel.build(context, panel, lp, onWallpaperRefresh, onPickWallpaper) { open(MODE_CUSTOMIZE) }
            },
            DrawerHeaderSpec(MODE_SUB_INPUTS, "📺  ", "TV Inputs & HDMI", "nav_quick") { panel ->
                SystemSubPanels.buildInputsSubPanel(context, panel, lp) { open(MODE_QUICK) }
            },
            DrawerHeaderSpec(MODE_SUB_PICTURE, "☀  ", "Display & Sleep Timer", "nav_quick") { panel ->
                SystemSubPanels.buildPictureAndDisplayPanel(context, panel, lp) { open(MODE_QUICK) }
            },
            DrawerHeaderSpec(MODE_SUB_SOUND, "🔊  ", "Sound & Audio Out", "nav_quick") { panel ->
                SystemSubPanels.buildSoundSubPanel(context, panel, lp) { open(MODE_QUICK) }
            },
            DrawerHeaderSpec(MODE_SUB_BLUETOOTH, "🔵  ", "Bluetooth & Remotes", "nav_quick") { panel ->
                SystemSubPanels.buildBluetoothPanel(context, panel, lp) { open(MODE_QUICK) }
            },
            DrawerHeaderSpec(MODE_SUB_STORAGE, "💾  ", "Storage & Installed Apps", "nav_quick") { panel ->
                SystemSubPanels.buildStorageAndAppsPanel(context, panel, lp) { open(MODE_QUICK) }
            }
        )
    }

    var isOpen = false
        private set
    private var currentMode = ""
    private var activeScroll: ScrollView? = null
    private var activePanel: LinearLayout? = null
    private var stageWidget: View? = null

    init {
        // Repaint drawer shell naturally whenever Accent or Glass Palette changes
        lp.addStyleListener {
            if (isOpen) {
                drawerLayout.background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    TileThemePalette.getGlassPanelColors(lp.tileColor, lp.accentColor, elevated = true)
                ).apply {
                    cornerRadius = LauncherUtils.dpF(context, 24f)
                    setStroke(dp(1.5f), LauncherUtils.withAlpha(lp.accentColor, 185))
                }
            }
        }
    }

    fun attachStageWidget(widget: View) {
        stageWidget = widget
    }

    fun getActivePanelView(): ViewGroup? = activePanel

    fun getParentNavTag(): String =
        drawerHeaders.find { it.mode == currentMode }?.navTag ?: drawerHeaders.first().navTag

    fun toggle(mode: String) {
        if (isOpen && mode == currentMode) close() else open(mode)
    }

    // Dynamically lock or unlock focus on all background sibling containers to prevent focus jumps
    private fun setBackgroundSiblingsFocusable(enabled: Boolean) {
        val parent = drawerLayout.parent as? ViewGroup ?: return
        val focusability = if (enabled) ViewGroup.FOCUS_AFTER_DESCENDANTS else ViewGroup.FOCUS_BLOCK_DESCENDANTS
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child !== drawerLayout && child !== scrim) {
                (child as? ViewGroup)?.descendantFocusability = focusability
            }
        }
    }

    fun open(mode: String) {
        val wasOpen = isOpen
        currentMode = mode
        isOpen = true

        setBackgroundSiblingsFocusable(false)

        val screenW = context.resources.displayMetrics.widthPixels
        val minStageW = LauncherUtils.dp(context, TileThemePalette.PANEL_MIN_WIDTH_DP)
        val dynamicDrawerW = maxOf(minStageW, (screenW * (lp.panelWidthPct / 100f)).toInt())
        drawerLayout.layoutParams.width = dynamicDrawerW
        drawerLayout.clipChildren = true
        drawerLayout.clipToPadding = true
        drawerLayout.requestLayout()

        // Apply user-selected Glass Palette theme dynamically to the entire Side Drawer
        drawerLayout.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            TileThemePalette.getGlassPanelColors(lp.tileColor, lp.accentColor, elevated = true)
        ).apply {
            cornerRadius = LauncherUtils.dpF(context, 24f)
            setStroke(dp(1.5f), LauncherUtils.withAlpha(lp.accentColor, 165))
        }
        drawerLayout.outlineProvider = null

        scrim.setBackgroundColor(Color.TRANSPARENT)
        scrim.visibility = View.VISIBLE
        drawerLayout.visibility = View.VISIBLE

        setupDrawerSkeleton()
        populateActivePanel()

        // Mark the first focusable child as default so Android's native focus engine selects it naturally
        FocusTrap.findFirstFocusable(activePanel ?: drawerLayout)?.let { firstView ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                firstView.isFocusedByDefault = true
            }
        }

        if (!wasOpen) {
            MotionEngine.resetTransform(drawerLayout)
            MotionEngine.animatePanelEnter(drawerLayout, stageWidget, dynamicDrawerW, lp.motionStyle) {}
        } else {
            MotionEngine.playDemoPreview(drawerLayout, lp.motionStyle)
        }
    }


    fun close() {
        if (!isOpen) return
        val returnTag = getParentNavTag()
        isOpen = false
        currentMode = ""
        val out = maxOf(dp(340f), drawerLayout.width + dp(30f))

        setBackgroundSiblingsFocusable(true)

        MotionEngine.animatePanelExit(drawerLayout, stageWidget, out, lp.motionStyle) {
            scrim.visibility = View.GONE
            drawerLayout.visibility = View.GONE
            mainPage.findViewWithTag<View>(returnTag)?.requestFocus()
        }
        drawerLayout.post { onLiveRefresh() }
    }

    private fun setupDrawerSkeleton() {
        drawerLayout.removeAllViews()
        drawerLayout.setPadding(dp(14f), dp(14f), dp(14f), dp(14f))

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(dp(6f), dp(4f), dp(6f), dp(10f))
        }

        val headerSpec = drawerHeaders.find { it.mode == currentMode } ?: drawerHeaders.first()
        val isSubPanel = currentMode.startsWith("sub_") || currentMode == MODE_WALLPAPER_EDIT
        if (isSubPanel) {
            val parentMode = if (currentMode == MODE_WALLPAPER_EDIT) MODE_CUSTOMIZE else MODE_QUICK
            val parentLabel = if (currentMode == MODE_WALLPAPER_EDIT) "Customize" else "Quick Settings"
            val backCrumb = TextView(context).apply {
                text = "‹ $parentLabel"
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(lp.accentColor)
                isFocusable = true
                isClickable = true
                setPadding(dp(6f), dp(3f), dp(8f), dp(3f))
                background = glassCardBg(false)
                setOnFocusChangeListener { v, f ->
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.04f, 3f)
                    v.background = glassCardBg(f)
                }
                setOnClickListener { open(parentMode) }
            }
            topRow.addView(backCrumb, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8f) })
        }
        topRow.addView(text("${headerSpec.icon}${headerSpec.title}", 13f, Color.WHITE, true))
        topRow.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))

        val closeX = TextView(context).apply {
            text = "✕"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = xBtnBg(false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.12f, 4f)
                v.background = xBtnBg(f)
            }
            setOnClickListener { close() }
        }
        topRow.addView(closeX, LinearLayout.LayoutParams(dp(28f), dp(28f)))
        drawerLayout.addView(topRow)

        val divider = View(context).apply { setBackgroundColor(Color.argb(60, 255, 255, 255)) }
        drawerLayout.addView(divider, LinearLayout.LayoutParams(-1, dp(1f)).apply { bottomMargin = dp(8f) })

        activeScroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isFillViewport = true
            clipChildren = true
            clipToPadding = true
            setPadding(0, dp(2f), 0, dp(2f))
        }
        activePanel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = true
            clipToPadding = true
            setPadding(dp(6f), dp(6f), dp(6f), dp(20f))
        }
        drawerLayout.addView(activeScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        activeScroll?.addView(activePanel)
    }

    private fun populateActivePanel() {
        val panel = activePanel ?: return
        panel.removeAllViews()
        // Dynamically render active panel from data-driven registry
        val spec = drawerHeaders.find { it.mode == currentMode } ?: drawerHeaders.first()
        spec.render(panel)
    }

    private fun buildRealAlertsPanel(panel: LinearLayout) {
        if (!TvNotificationService.isPermissionGranted(context)) {
            addLabel(panel, "PERMISSION REQUIRED")
            addActionButton(panel, "🔓", "Enable Notification Access", "Allow launcher to show real system & app alerts") {
                try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } catch (_: Exception) {}
            }
            return
        }
        val dndState = lp.dndEnabled
        val dndRow = PanelUiKit.makeActionRow(
            context, lp,
            if (dndState) "🔕" else "🔔",
            if (dndState) "Do Not Disturb: ON" else "Do Not Disturb: OFF",
            if (dndState) "Night nudges & alert popups muted" else "Tap to mute night nudges",
            if (dndState) "ON" else "OFF"
        ) {
            lp.dndEnabled = !lp.dndEnabled
            populateActivePanel()
        }
        panel.addView(dndRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8f) })

        val alerts = TvNotificationService.getAlerts()
        val topBar = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val headerCountTv = text("LIVE SYSTEM ALERTS (${alerts.size})", 9f, lp.accentColor, true)
        topBar.addView(headerCountTv)
        topBar.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
        val listHolder = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; clipChildren = false; clipToPadding = false }
        if (alerts.isNotEmpty()) {
            topBar.addView(text("Clear All", 9f, Color.WHITE, true).apply {
                setPadding(dp(8f), dp(3f), dp(8f), dp(3f))
                isFocusable = true
                background = glassCardBg(false)
                setOnFocusChangeListener { v, f ->
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 3f)
                    v.background = glassCardBg(f)
                }
                setOnClickListener {
                    TvNotificationService.clearAllAlerts()
                    listHolder.removeAllViews()
                    headerCountTv.text = "LIVE SYSTEM ALERTS (0)"
                    listHolder.addView(text("No active notifications right now", 10.5f, Color.argb(180, 210, 230, 250), false).apply {
                        gravity = Gravity.CENTER
                        setPadding(0, dp(28f), 0, dp(28f))
                    })
                }
            })
        }
        panel.addView(topBar, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8f) })
        panel.addView(listHolder, LinearLayout.LayoutParams(-1, -2))
        if (alerts.isEmpty()) {
            listHolder.addView(text("No active notifications right now", 10.5f, Color.argb(180, 210, 230, 250), false).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(28f), 0, dp(28f))
            })
            return
        }
        for (item in alerts) {
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(11f), dp(9f), dp(11f), dp(9f))
                isFocusable = true
                background = glassCardBg(false)
                setOnFocusChangeListener { v, f ->
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f)
                    v.background = glassCardBg(f)
                }
                setOnClickListener { v ->
                    // Launch source app if available, then dismiss alert
                    item.packageName?.let { pkg ->
                        try {
                            context.packageManager.getLaunchIntentForPackage(pkg)?.let { context.startActivity(it) }
                        } catch (_: Exception) {}
                    }
                    TvNotificationService.dismissAlert(item.key)
                    listHolder.removeView(v)
                    val rem = TvNotificationService.getAlerts().size
                    headerCountTv.text = "LIVE SYSTEM ALERTS ($rem)"
                    if (rem == 0) {
                        listHolder.addView(text("No active notifications right now", 10.5f, Color.argb(180, 210, 230, 250), false).apply {
                            gravity = Gravity.CENTER
                            setPadding(0, dp(28f), 0, dp(28f))
                        })
                    }
                }
            }
            val row1 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            row1.addView(text("🔔  ${item.appName}", 9f, lp.accentColor, true), LinearLayout.LayoutParams(0, -2, 1f))
            row1.addView(text("${item.timeStr}  ›", 8.5f, Color.argb(180, 255, 255, 255), true))
            card.addView(row1)
            card.addView(text(item.title, 10.5f, Color.WHITE, true).apply {
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, dp(3f), 0, dp(1f))
            })
            card.addView(text(item.text, 9f, Color.argb(195, 215, 232, 250), false).apply {
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            })
            listHolder.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(7f) })
        }
    }
    private fun buildPowerPanel(panel: LinearLayout) {
        addLabel(panel, "DEVICE POWER & LAUNCHER")
        addActionButton(panel, "🕰️", "Ambient Clock Screensaver", "Enter full-screen clock & weather view") {
            close()
            context.sendBroadcast(Intent("shree.tvhome.ACTION_ENTER_AMBIENT").setPackage(context.packageName))
        }
        addActionButton(panel, "🌙", "Sleep / Home Standby", "Return to system standby") {
            try { context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)) } catch (_: Exception) {}
            close()
        }
        addActionButton(panel, "🔄", "Soft Restart Launcher", "Reload all apps, icons & visual shaders") {
            AppGridManager.clearCache()
            onLiveRefresh()
            close()
        }
    }

    fun showProfileEditDialog() {
        ProfileCropDialog.show(context, lp, onPickProfilePic) {
            onLiveRefresh()
            if (isOpen) populateActivePanel()
        }
    }

    private fun addActionButton(panel: LinearLayout, icon: String, title: String, sub: String, act: () -> Unit) {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), dp(9f), dp(12f), dp(9f))
            isFocusable = true
            background = glassCardBg(false)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f)
                v.background = glassCardBg(f)
            }
            setOnClickListener { act() }
        }
        card.addView(text(icon, 14f, lp.accentColor, true).apply { setPadding(0, 0, dp(10f), 0) })
        val mid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(text(title, 10.5f, Color.WHITE, true))
        mid.addView(text(sub, 8.5f, Color.argb(185, 210, 230, 245), false))
        card.addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(text("›", 15f, Color.argb(180, 255, 255, 255), true))
        panel.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6f) })
    }

    private fun xBtnBg(foc: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(if (foc) Color.argb(165, 255, 75, 85) else Color.argb(55, 255, 255, 255))
        setStroke(dp(1f), if (foc) Color.WHITE else Color.argb(85, 255, 255, 255))
    }

    private fun glassCardBg(foc: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (foc) intArrayOf(Color.argb(115, 255, 255, 255), Color.argb(75, 255, 255, 255))
        else intArrayOf(Color.argb(52, 255, 255, 255), Color.argb(26, 255, 255, 255))
    ).apply {
        cornerRadius = LauncherUtils.dpF(context, 14f)
        setStroke(dp(if (foc) 2f else 1f), if (foc) lp.accentColor else Color.argb(65, 255, 255, 255))
    }

    private fun text(v: String, sz: Float, col: Int, bld: Boolean) = TextView(context).apply {
        text = v
        textSize = sz
        setTextColor(col)
        if (bld) typeface = Typeface.DEFAULT_BOLD
    }

    private fun addLabel(panel: LinearLayout, lbl: String) = panel.addView(
        text(lbl.uppercase(Locale.ROOT), 9f, lp.accentColor, true).apply { letterSpacing = 0.09f },
        LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8f); bottomMargin = dp(5f) }
    )

    private fun dp(v: Float) = LauncherUtils.dpF(context, v).toInt()
}
