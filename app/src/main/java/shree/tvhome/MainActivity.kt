package shree.tvhome

import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import shree.tvhome.design.AppGridManager
import shree.tvhome.design.AppTileFactory
import shree.tvhome.panels.CustomizeMenuBuilder
import shree.tvhome.panels.SideControlDrawer
import shree.tvhome.source.AmbientStageWidget
import shree.tvhome.source.LauncherBackground
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.LauncherUtils
import shree.tvhome.source.MotionEngine
import shree.tvhome.source.NightNudgeReceiver
import shree.tvhome.source.TileThemePalette
import shree.tvhome.source.TopNavBar

class MainActivity : Activity() {
    companion object {
        private const val REQ_PICK_WALLPAPER = 1001
        private const val REQ_PICK_PROFILE_PIC = 1002
    }

    private lateinit var rootLayout: FrameLayout
    private lateinit var mainPage: LinearLayout
    private lateinit var mainBody: LinearLayout
    private lateinit var topNavBar: TopNavBar
    private lateinit var hiddenSection: LinearLayout
    private lateinit var hiddenRow: LinearLayout
    private lateinit var hiddenCount: TextView
    private lateinit var favSection: LinearLayout
    private lateinit var favRow: LinearLayout
    private lateinit var appGrid: LinearLayout
    private lateinit var drawerLayout: LinearLayout
    private lateinit var favCount: TextView
    private lateinit var allCount: TextView
    private lateinit var drawerScrim: View
    private lateinit var lp: LauncherPreferences
    private lateinit var sideDrawer: SideControlDrawer
    private lateinit var ambientStage: AmbientStageWidget

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppGridManager.clearCache()
            syncAppGrid(allowFocusRestore = false)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = LauncherUtils.calculateAdaptiveScale(newBase)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lp = LauncherPreferences(this)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        buildScreen()
        if (lp.pictureMode == "Custom") {
            shree.tvhome.panels.SystemSubPanels.applyWarmTintOverlay(this, lp.customWarmthPct)
        } else {
            val preset = shree.tvhome.panels.SystemSubPanels.PICTURE_MODES.find { it.key == lp.pictureMode }
            if (preset != null) shree.tvhome.panels.SystemSubPanels.applyWarmTintOverlay(this, preset.warmth)
        }
        
        NightNudgeReceiver.scheduleNextNudge(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pkgReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(pkgReceiver, filter)
        }

        val ambientFilter = IntentFilter("shree.tvhome.ACTION_ENTER_AMBIENT")
        val ambientReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                toggleAmbientScreensaver(true)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(ambientReceiver, ambientFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(ambientReceiver, ambientFilter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(pkgReceiver) } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        if (::appGrid.isInitialized) syncAppGrid(allowFocusRestore = false)
        if (::ambientStage.isInitialized) ambientStage.triggerWeatherRefresh(false)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (sideDrawer.isOpen) {
            if (CustomizeMenuBuilder.isSubMenuOpen()) {
                CustomizeMenuBuilder.closeSubMenuWithAnimation()
            } else {
                sideDrawer.close()
            }
        } else if (::hiddenSection.isInitialized && hiddenSection.visibility == View.VISIBLE) {
            closeHiddenRibbon()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private var isAmbientScreensaverActive = false

    private fun toggleAmbientScreensaver(enable: Boolean) {
        if (isAmbientScreensaverActive == enable) return
        isAmbientScreensaverActive = enable
        if (enable) {
            if (::sideDrawer.isInitialized && sideDrawer.isOpen) sideDrawer.close()
            topNavBar.animate().alpha(0f).setDuration(350L).start()
            mainBody.animate().alpha(0f).setDuration(350L).withEndAction {
                mainBody.visibility = View.INVISIBLE
            }.start()
            ambientStage.animate().scaleX(1.22f).scaleY(1.22f).translationX(-dp(80).toFloat()).setDuration(450L).start()
        } else {
            mainBody.visibility = View.VISIBLE
            topNavBar.animate().alpha(1f).setDuration(300L).start()
            mainBody.animate().alpha(1f).setDuration(300L).start()
            ambientStage.animate().scaleX(1f).scaleY(1f).translationX(0f).setDuration(350L).start()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isAmbientScreensaverActive && event.action == KeyEvent.ACTION_DOWN) {
            toggleAmbientScreensaver(false)
            return true
        }
        if (::sideDrawer.isInitialized && sideDrawer.isOpen &&
            FocusTrap.handleKeyEvent(
                event = event,
                isDrawerOpen = true,
                drawerLayout = drawerLayout,
                topNavBar = topNavBar,
                activeParentTag = sideDrawer.getParentNavTag(),
                activePanel = sideDrawer.getActivePanelView()
            )
        ) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(req, res, data)
        if (res != RESULT_OK || data?.data == null) return
        val uri = data.data!!
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}

        if (req == REQ_PICK_WALLPAPER) {
            lp.prefs.edit()
                .putString(LauncherPreferences.PREF_WALLPAPER, uri.toString())
                .putString(LauncherPreferences.PREF_WP_SCALE_MODE, "fit")
                .putFloat(LauncherPreferences.PREF_WP_ZOOM, 1.08f)
                .putInt(LauncherPreferences.PREF_WP_OFFSET_X, 0)
                .putInt(LauncherPreferences.PREF_WP_OFFSET_Y, 0)
                .apply()
            applyWallpaperOnly()
            sideDrawer.open(SideControlDrawer.MODE_WALLPAPER_EDIT)
        } else if (req == REQ_PICK_PROFILE_PIC) {
            lp.profilePicUri = uri.toString()
            topNavBar.refreshProfileAvatar()
            Toast.makeText(this, "Profile avatar updated!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        when (requestCode) {
            shree.tvhome.panels.WifiPanelBuilder.REQ_WIFI_LOCATION -> {
                if (granted) {
                    Toast.makeText(this, "Location granted • Scanning Wi-Fi...", Toast.LENGTH_SHORT).show()
                    if (::sideDrawer.isInitialized && sideDrawer.isOpen) {
                        sideDrawer.open(SideControlDrawer.MODE_WIFI)
                    }
                } else {
                    Toast.makeText(this, "Location permission needed to view Wi-Fi networks", Toast.LENGTH_SHORT).show()
                }
            }
            shree.tvhome.panels.SystemSubPanels.REQ_BLUETOOTH_CONNECT -> {
                if (granted) {
                    Toast.makeText(this, "Bluetooth access granted", Toast.LENGTH_SHORT).show()
                    if (::sideDrawer.isInitialized && sideDrawer.isOpen) {
                        sideDrawer.open(SideControlDrawer.MODE_SUB_BLUETOOTH)
                    }
                } else {
                    Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun openImagePicker(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, requestCode)
        } catch (_: Exception) { Toast.makeText(this, "No image picker found", Toast.LENGTH_SHORT).show() }
    }

    private fun computeRightStageWidth(): Int {
        val minStageW = LauncherUtils.dp(this, TileThemePalette.PANEL_MIN_WIDTH_DP)
        return maxOf(minStageW, (resources.displayMetrics.widthPixels * (lp.panelWidthPct / 100f)).toInt())
    }

    private fun buildScreen() {
        rootLayout = FrameLayout(this)
        LauncherBackground.apply(this, rootLayout, lp)

        mainPage = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(dp(28), dp(24), dp(28), dp(12))
        }
        rootLayout.addView(mainPage, FrameLayout.LayoutParams(-1, -1))

        topNavBar = TopNavBar(this, lp, object : TopNavBar.NavCallbacks {
            override fun onProfileClick() = sideDrawer.showProfileEditDialog()
            override fun onVoiceSearch() {
                val actions = listOf(Intent.ACTION_ASSIST, RecognizerIntent.ACTION_WEB_SEARCH, Intent.ACTION_VOICE_COMMAND)
                for (act in actions) { try { startActivity(Intent(act)); return } catch (_: Exception) {} }
                Toast.makeText(this@MainActivity, "Voice Assistant not installed", Toast.LENGTH_SHORT).show()
            }
            override fun onWifiClick() = sideDrawer.toggle(SideControlDrawer.MODE_WIFI)
            override fun onAlertsClick() = sideDrawer.toggle(SideControlDrawer.MODE_ALERTS)
            override fun onCustomizeClick() = sideDrawer.toggle(SideControlDrawer.MODE_CUSTOMIZE)
            override fun onQuickSettingsClick() = sideDrawer.toggle(SideControlDrawer.MODE_QUICK)
            override fun onPowerClick() = sideDrawer.toggle(SideControlDrawer.MODE_POWER)
        })
        mainPage.addView(topNavBar, LinearLayout.LayoutParams(-1, dp(42)).apply { bottomMargin = dp(18) })

        val contentArea = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }
        mainPage.addView(contentArea, LinearLayout.LayoutParams(-1, 0, 1f))

        val rightStageW = computeRightStageWidth()
        mainBody = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), rightStageW + dp(16), 0)
            layoutTransition = LayoutTransition().apply {
                setDuration(320L)
                enableTransitionType(LayoutTransition.CHANGING)
                setInterpolator(LayoutTransition.CHANGING, LauncherUtils.FLUID_SPRING)
            }
        }
        contentArea.addView(mainBody, FrameLayout.LayoutParams(-1, -1))

        hiddenSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(10), dp(6), dp(10), dp(10))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(95, 255, 95, 125), Color.argb(45, 32, 24, 58))
            ).apply {
                cornerRadius = LauncherUtils.dpF(this@MainActivity, 18f)
                setStroke(dp(1), Color.argb(130, 255, 130, 155))
            }
        }
        val hiddenHeader = AppGridManager.createHeader(this, "👁  HIDDEN APPS", true)
        hiddenCount = hiddenHeader.findViewWithTag("count_tv")
        val closeHiddenBtn = TextView(this).apply {
            text = "Done ✕"; textSize = 10f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); isFocusable = true; isClickable = true
            setPadding(dp(12), dp(4), dp(12), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.argb(85, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(this@MainActivity, 14f)
                setStroke(dp(1), Color.argb(140, 255, 255, 255))
            }
            setOnFocusChangeListener { v, f -> MotionEngine.animateFocus(v, f, lp.motionStyle, 1.06f, 4f) }
            setOnClickListener { closeHiddenRibbon() }
        }
        hiddenHeader.addView(closeHiddenBtn, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
        hiddenSection.addView(hiddenHeader)

        val hiddenHScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false; clipChildren = false
        }
        hiddenRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false }
        hiddenHScroll.addView(hiddenRow)
        hiddenSection.addView(hiddenHScroll)
        mainBody.addView(hiddenSection, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        favSection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val favHeader = AppGridManager.createHeader(this, "♥  FAVORITES & QUICK ACCESS", true)
        favCount = favHeader.findViewWithTag("count_tv")
        favSection.addView(favHeader)

        val favHScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false; clipChildren = false
        }
        favRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false }
        favHScroll.addView(favRow); favSection.addView(favHScroll); mainBody.addView(favSection)

        val allHeader = AppGridManager.createHeader(this, "⊞  ALL APPS", false, lp) { syncAppGrid(allowFocusRestore = false) }
        allCount = allHeader.findViewWithTag("count_tv")
        mainBody.addView(allHeader, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false; isSmoothScrollingEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER; clipToPadding = false; setPadding(0, dp(4), 0, dp(24))
            viewTreeObserver.addOnScrollChangedListener { LauncherBackground.onScrollParallax(scrollY) }
        }

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutTransition = LayoutTransition().apply {
                setDuration(200L)
                disableTransitionType(LayoutTransition.APPEARING)
                disableTransitionType(LayoutTransition.DISAPPEARING)
            }
        }
        appGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; clipChildren = false }
        scrollContent.addView(appGrid); scroll.addView(scrollContent)
        mainBody.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setupRightDrawer(contentArea)
        val nudgeView = topNavBar.createFloatingNudgeView()
        val nudgeParams = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply {
            topMargin = dp(52)
        }
        rootLayout.addView(nudgeView, nudgeParams)
        
        // Position nudge bubble dynamically after TopNavBar measures its profile card width
        topNavBar.post {
            val profileGroup = topNavBar.getChildAt(1)
            nudgeParams.leftMargin = (profileGroup?.right ?: dp(180)) + dp(12)
            nudgeView.requestLayout()
        }
        setContentView(rootLayout)
        // Request focus on the first app tile when launcher starts fresh
        syncAppGrid(allowFocusRestore = true)
    }

    private fun openHiddenRibbon() {
        AppGridManager.populateHiddenRibbon(this, lp, hiddenSection, hiddenRow, hiddenCount) { pkg, label ->
            val updated = lp.getHiddenApps().apply { remove(pkg) }
            lp.setHiddenApps(updated)
            Toast.makeText(this, "$label restored to Home", Toast.LENGTH_SHORT).show()
            syncAppGrid(allowFocusRestore = false, restorePkg = pkg)
            if (updated.isEmpty()) closeHiddenRibbon(pkg) else openHiddenRibbon()
        }
        if (hiddenSection.visibility == View.VISIBLE) {
            hiddenSection.alpha = 0f; hiddenSection.scaleY = 0.90f
            hiddenSection.animate().alpha(1f).scaleY(1f).setDuration(320L).setInterpolator(LauncherUtils.FLUID_SPRING).start()
        }
    }

    private fun closeHiddenRibbon(focusPkg: String? = null) {
        if (hiddenSection.visibility != View.VISIBLE) return
        hiddenSection.animate().alpha(0f).scaleY(0.90f).setDuration(220L).setInterpolator(LauncherUtils.SILKY_EASE)
            .withEndAction {
                hiddenSection.visibility = View.GONE; hiddenSection.alpha = 1f; hiddenSection.scaleY = 1f
                syncAppGrid(allowFocusRestore = false, restorePkg = focusPkg)
            }.start()
    }

    private fun setupRightDrawer(contentArea: FrameLayout) {
        val drawerW = computeRightStageWidth()
        ambientStage = AmbientStageWidget(this, lp)
        contentArea.addView(ambientStage, FrameLayout.LayoutParams(drawerW, -2, Gravity.END or Gravity.CENTER_VERTICAL))

        drawerScrim = View(this).apply {
            setBackgroundColor(0x33000000); visibility = View.GONE; isClickable = true
            setOnClickListener { sideDrawer.close() }
        }
        contentArea.addView(drawerScrim, FrameLayout.LayoutParams(-1, -1))

        drawerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; visibility = View.GONE; translationX = (drawerW + dp(30)).toFloat()
        }
        contentArea.addView(drawerLayout, FrameLayout.LayoutParams(drawerW, -1, Gravity.END).apply { topMargin = dp(6); bottomMargin = dp(10) })

        sideDrawer = SideControlDrawer(
            this, drawerLayout, drawerScrim, mainPage, lp,
            ::applyLiveVisuals,
            ::applyWallpaperOnly,
            { openImagePicker(REQ_PICK_WALLPAPER) },
            { openImagePicker(REQ_PICK_PROFILE_PIC) },
            ::openHiddenRibbon
        )
        sideDrawer.attachStageWidget(ambientStage)
    }

    private fun applyLiveVisuals() {
        topNavBar.refreshProfileAvatar()
        topNavBar.refreshGreeting()
        LauncherBackground.updateThemeAndAccent(rootLayout, lp)

        val newW = computeRightStageWidth()
        mainBody.setPadding(0, dp(4), newW + dp(16), 0)
        ambientStage.layoutParams.width = newW
        ambientStage.refreshStyle()
        ambientStage.triggerWeatherRefresh(true)

        val currentW = drawerLayout.layoutParams.width
        if (currentW != newW && currentW > 0 && sideDrawer.isOpen) {
            ValueAnimator.ofInt(currentW, newW).apply {
                duration = 420L
                interpolator = MotionEngine.getInterpolator(lp.motionStyle)
                addUpdateListener { anim ->
                    drawerLayout.layoutParams.width = anim.animatedValue as Int
                    drawerLayout.requestLayout()
                }
                start()
            }
        } else if (currentW != newW) {
            drawerLayout.layoutParams.width = newW
            drawerLayout.requestLayout()
        }

        syncAppGrid(allowFocusRestore = false)
    }

    private fun applyWallpaperOnly() {
        while (rootLayout.childCount > 0 && rootLayout.getChildAt(0) !== mainPage) {
            rootLayout.removeViewAt(0)
        }
        LauncherBackground.apply(this, rootLayout, lp)
    }

    private fun syncAppGrid(allowFocusRestore: Boolean, restorePkg: String? = null) {
        val isDrawerOpen = if (::sideDrawer.isInitialized) sideDrawer.isOpen else false
        AppGridManager.populate(
            this, lp, favSection, favRow, favCount, appGrid, allCount,
            allowFocusRestore, restorePkg, isDrawerOpen,
            object : AppTileFactory.TileActions {
                override fun onToggleFavorite(pkg: String, label: String) {
                    val list = lp.getOrderedFavorites(); val added = !list.contains(pkg)
                    if (added) list.add(pkg) else list.remove(pkg)
                    lp.setFavorites(list)
                    Toast.makeText(this@MainActivity, if (added) "$label added to Favorites" else "$label removed", Toast.LENGTH_SHORT).show()
                    syncAppGrid(allowFocusRestore = false, restorePkg = pkg)
                }
                override fun onMoveFavorite(pkg: String, delta: Int) {
                    if (lp.moveFavorite(pkg, delta)) syncAppGrid(allowFocusRestore = false, restorePkg = pkg)
                }
                override fun onHideApp(pkg: String, label: String) {
                    val hidden = lp.getHiddenApps().apply { add(pkg) }; lp.setHiddenApps(hidden)
                    Toast.makeText(this@MainActivity, "$label hidden", Toast.LENGTH_SHORT).show()
                    syncAppGrid(allowFocusRestore = false)
                }
            }
        )
    }

    private fun dp(value: Int) = LauncherUtils.dp(this, value)
}
