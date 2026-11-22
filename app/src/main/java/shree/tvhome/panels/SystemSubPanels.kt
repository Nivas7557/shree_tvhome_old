package shree.tvhome.panels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.LauncherUtils
import shree.tvhome.source.MotionEngine
import shree.tvhome.source.SystemCapabilities
import shree.tvhome.source.TileThemePalette
import java.util.Locale

object SystemSubPanels {

    const val REQ_BLUETOOTH_CONNECT = 202
    private const val WARM_OVERLAY_TAG = "shree_warm_tint_overlay"

    data class PicturePreset(val key: String, val label: String, val brightness: Int, val warmth: Int)
    data class TimeoutOption(val millis: Int, val label: String)

    val PICTURE_MODES = listOf(
        PicturePreset("Standard", "Standard", 80, 0),
        PicturePreset("Cinema", "Cinema / Movie", 65, 18),
        PicturePreset("Sports", "Sports", 95, 0),
        PicturePreset("Vivid", "Vivid", 100, 0),
        PicturePreset("EyeCare", "Night Eye-Care", 40, 38),
        PicturePreset("Custom", "Custom (Adjust)", 80, 10)
    )

    private data class QuickTileSpec(val icon: String, val title: String, val subtitle: String, val targetMode: String)

    fun buildQuickSettingsMain(
        ctx: Context,
        panel: LinearLayout,
        lp: LauncherPreferences,
        onNavigate: (String) -> Unit
    ) {
        addHeader(ctx, panel, lp, "QUICK SETTINGS")

        val curBright = getCurrentBrightnessPct(ctx)
        val activeAudio = SystemCapabilities.getAudioOutputs(ctx).lastOrNull()?.typeName
            ?: SystemCapabilities.mapAudioDeviceType(0)
        val connectedWifi = TileThemePalette.getConnectedSsid(ctx)

        // Build tiles dynamically based on actual TV hardware capabilities
        val dynamicTiles = buildList {
            add(QuickTileSpec("☀", "Display & Picture", "${lp.pictureMode} • $curBright%", SideControlDrawer.MODE_SUB_PICTURE))
            add(QuickTileSpec("🔊", "Sound & Audio", activeAudio, SideControlDrawer.MODE_SUB_SOUND))
            add(QuickTileSpec("📶", "Wi-Fi Network", connectedWifi, SideControlDrawer.MODE_WIFI))

            if (SystemCapabilities.hasBluetoothHardware(ctx)) {
                add(QuickTileSpec("🔵", "Bluetooth", "Remotes & Audio", SideControlDrawer.MODE_SUB_BLUETOOTH))
            }
            val inputs = SystemCapabilities.getTvInputs(ctx)
            if (inputs.isNotEmpty()) {
                add(QuickTileSpec("📺", "TV Inputs", "${inputs.size} Sources", SideControlDrawer.MODE_SUB_INPUTS))
            }
            add(QuickTileSpec("💾", "Storage & Apps", getFreeStorageSummary(), SideControlDrawer.MODE_SUB_STORAGE))
        }

        dynamicTiles.chunked(2).forEachIndexed { rowIdx, pair ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
                clipToPadding = false
            }
            pair.forEachIndexed { colIdx, tile ->
                val card = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(ctx, 10), dp(ctx, 6), dp(ctx, 10), dp(ctx, 6))
                    isFocusable = true
                    background = cardBg(ctx, lp.accentColor, false)
                    setOnFocusChangeListener { v, f ->
                        MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 4f)
                        v.background = cardBg(ctx, lp.accentColor, f)
                    }
                    setOnClickListener { onNavigate(tile.targetMode) }
                    addView(text(ctx, "${tile.icon}  ${tile.title}", 10f, Color.WHITE, true))
                    addView(text(ctx, tile.subtitle, 8.2f, lp.accentColor, false).apply {
                        isSingleLine = true; ellipsize = TextUtils.TruncateAt.END; setPadding(0, dp(ctx, 2), 0, 0)
                    })
                }
                row.addView(
                    card,
                    LinearLayout.LayoutParams(0, dp(ctx, 56), 1f).apply {
                        if (colIdx < pair.lastIndex) rightMargin = dp(ctx, 6)
                    }
                )
            }
            val bottomPad = if (rowIdx == dynamicTiles.chunked(2).lastIndex) dp(ctx, 8) else dp(ctx, 6)
            panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = bottomPad })
        }

        PanelUiKit.addSectionHeader(ctx, panel, lp, "HARDWARE CONTROLS")
        PanelUiKit.addGlossyPillSlider(ctx, panel, lp, "☀", "Screen Brightness", curBright) { p ->
            lp.pictureMode = "Custom"
            lp.customBrightnessPct = p
            applyBrightness(ctx, p)
        }

        if (!SystemCapabilities.isVolumeFixed(ctx)) {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val maxVol = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            val curVol = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10
            val volPct = ((curVol * 100f) / maxOf(1, maxVol)).toInt()
            PanelUiKit.addGlossyPillSlider(ctx, panel, lp, "🔊", "Master Volume", volPct) { p ->
                am?.setStreamVolume(AudioManager.STREAM_MUSIC, ((p / 100f) * maxVol).toInt(), AudioManager.FLAG_SHOW_UI)
            }
        }

        val ambientRow = PanelUiKit.makeActionRow(ctx, lp, "🕰️", "Ambient Clock Screensaver", "Full-screen clock, weather & live scene") {
            ctx.sendBroadcast(Intent("shree.tvhome.ACTION_ENTER_AMBIENT").setPackage(ctx.packageName))
        }
        panel.addView(ambientRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })

        val ramRow = PanelUiKit.makeActionRow(ctx, lp, "🚀", "Boost Memory (RAM)", getRamStatusText(ctx)) {
            Runtime.getRuntime().gc()
            Toast.makeText(ctx, "Memory optimized!", Toast.LENGTH_SHORT).show()
        }
        panel.addView(ramRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })

        // Dynamic OS Settings discovered on THIS specific TV model
        val nativeSettings = SystemCapabilities.getAvailableSettings(ctx)
        if (nativeSettings.isNotEmpty()) {
            addHeader(ctx, panel, lp, "TV SYSTEM SETTINGS")
            for (item in nativeSettings) {
                val row = makeActionRow(ctx, lp, item.icon, item.title, item.subtitle) {
                    try { ctx.startActivity(item.intent) } catch (_: Exception) {}
                }
                panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
            }
        }
    }
    fun buildPictureAndDisplayPanel(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onBack: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(ctx)) {
            val permBtn = makeActionRow(ctx, lp, "🔓", "Enable System Brightness Control", "Allow launcher to adjust TV backlight") {
                try {
                    ctx.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                } catch (_: Exception) {}
            }
            panel.addView(permBtn, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })
        }

        addHeader(ctx, panel, lp, "DISPLAY PICTURE MODES")
        val customSlidersBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }

        val rebuildCustomControls = {
            customSlidersBox.removeAllViews()
            if (lp.pictureMode == "Custom") {
                addSliderCard(ctx, customSlidersBox, lp, "☀  Custom Brightness", lp.customBrightnessPct) { p ->
                    lp.customBrightnessPct = p
                    applyBrightness(ctx, p)
                }
                addSliderCard(ctx, customSlidersBox, lp, "🌅  Warmth / Eye-Care Tint", lp.customWarmthPct) { w ->
                    lp.customWarmthPct = w
                    applyWarmTintOverlay(ctx, w)
                }
            }
            // Probe if TV exposes native Sharpness / Contrast / Picture Menu
            SystemCapabilities.resolveIntent(ctx, Settings.ACTION_DISPLAY_SETTINGS)?.let { dispIntent ->
                val hwBtn = makeActionRow(ctx, lp, "🎛", "TV Sharpness, Contrast & HDR", "Open native TV hardware picture menu") {
                    try { ctx.startActivity(dispIntent) } catch (_: Exception) {}
                }
                customSlidersBox.addView(hwBtn, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
            }
        }

        val modeCards = ArrayList<Pair<LinearLayout, TextView>>()
        PICTURE_MODES.forEach { preset ->
            val isSel = lp.pictureMode == preset.key
            val targetBright = if (preset.key == "Custom") lp.customBrightnessPct else preset.brightness
            val titleTv = text(ctx, if (isSel) "✓  ${preset.label}" else "○  ${preset.label}", 10.5f, if (isSel) lp.accentColor else Color.WHITE, true)
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9))
                isFocusable = true
                background = cardBg(ctx, lp.accentColor, isSel)
                setOnFocusChangeListener { v, f ->
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 3f)
                    v.background = cardBg(ctx, lp.accentColor, f || lp.pictureMode == preset.key)
                }
                setOnClickListener {
                    lp.pictureMode = preset.key
                    val b = if (preset.key == "Custom") lp.customBrightnessPct else preset.brightness
                    val w = if (preset.key == "Custom") lp.customWarmthPct else preset.warmth
                    applyBrightness(ctx, b)
                    applyWarmTintOverlay(ctx, w)
                    modeCards.forEachIndexed { i, (cView, tView) ->
                        val active = PICTURE_MODES[i].key == lp.pictureMode
                        tView.text = if (active) "✓  ${PICTURE_MODES[i].label}" else "○  ${PICTURE_MODES[i].label}"
                        tView.setTextColor(if (active) lp.accentColor else Color.WHITE)
                        cView.background = cardBg(ctx, lp.accentColor, cView.hasFocus() || active)
                    }
                    rebuildCustomControls()
                }
            }
            card.addView(titleTv, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(text(ctx, "$targetBright%", 9f, Color.argb(190, 220, 235, 255), true))
            modeCards.add(card to titleTv)
            panel.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
        }

        panel.addView(customSlidersBox, LinearLayout.LayoutParams(-1, -2))
        rebuildCustomControls()

        // Dynamic Screen Sleep Timeout (includes current TV value if non-standard)
        addHeader(ctx, panel, lp, "SCREEN SLEEP TIMEOUT")
        val currentTimeout = try {
            Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 900_000)
        } catch (_: Exception) { 900_000 }

        val baseTimeouts = mutableListOf(
            TimeoutOption(300_000, "5 Minutes"),
            TimeoutOption(900_000, "15 Minutes"),
            TimeoutOption(1_800_000, "30 Minutes"),
            TimeoutOption(2_147_483_647, "Never Sleep")
        )
        if (baseTimeouts.none { isTimeoutMatch(currentTimeout, it.millis) } && currentTimeout > 0) {
            val mins = maxOf(1, currentTimeout / 60_000)
            baseTimeouts.add(0, TimeoutOption(currentTimeout, "$mins Minutes (TV Default)"))
        }

        var activeTimeout = currentTimeout
        val tCards = ArrayList<Pair<LinearLayout, TextView>>()
        baseTimeouts.forEach { opt ->
            val isSel = isTimeoutMatch(activeTimeout, opt.millis)
            val tv = text(ctx, if (isSel) "✓  ${opt.label}" else "○  ${opt.label}", 10.5f, if (isSel) lp.accentColor else Color.WHITE, true)
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9)); isFocusable = true
                background = cardBg(ctx, lp.accentColor, isSel)
                setOnFocusChangeListener { v, f ->
                    MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 3f)
                    v.background = cardBg(ctx, lp.accentColor, f || isTimeoutMatch(activeTimeout, opt.millis))
                }
                setOnClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(ctx)) {
                        try {
                            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, opt.millis)
                            activeTimeout = opt.millis
                            tCards.forEachIndexed { idx, (cView, tView) ->
                                val act = isTimeoutMatch(activeTimeout, baseTimeouts[idx].millis)
                                tView.text = if (act) "✓  ${baseTimeouts[idx].label}" else "○  ${baseTimeouts[idx].label}"
                                tView.setTextColor(if (act) lp.accentColor else Color.WHITE)
                                cView.background = cardBg(ctx, lp.accentColor, cView.hasFocus() || act)
                            }
                            Toast.makeText(ctx, "Sleep timer: ${opt.label}", Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {}
                    } else {
                        Toast.makeText(ctx, "Enable System Brightness/Write Control above first", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            card.addView(tv, LinearLayout.LayoutParams(0, -2, 1f))
            tCards.add(card to tv)
            panel.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
        }
        addBackBtn(ctx, panel, lp, onBack)
    }
    fun buildSoundSubPanel(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onBack: () -> Unit) {
        addHeader(ctx, panel, lp, "ACTIVE AUDIO OUTPUTS")
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val devices = SystemCapabilities.getAudioOutputs(ctx)
        if (devices.isEmpty()) {
            panel.addView(makeActionRow(ctx, lp, "🔊", SystemCapabilities.mapAudioDeviceType(0), "Default TV audio output") {},
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
        } else {
            devices.forEach { dev ->
                panel.addView(makeActionRow(ctx, lp, "🎧", dev.typeName, dev.productName) {},
                    LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
            }
        }
        val initialMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am?.isStreamMute(AudioManager.STREAM_MUSIC) == true else false
        val muteCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9)); isFocusable = true; background = cardBg(ctx, lp.accentColor, false)
            setOnFocusChangeListener { v, f -> MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f); v.background = cardBg(ctx, lp.accentColor, f) }
        }
        val iconTv = text(ctx, if (initialMuted) "🔇" else "🔈", 14f, lp.accentColor, true).apply { setPadding(0, 0, dp(ctx, 10), 0) }
        val titleTv = text(ctx, if (initialMuted) "Unmute TV Audio" else "Mute TV Audio", 10.5f, Color.WHITE, true)
        val mid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; addView(titleTv); addView(text(ctx, "Instant audio toggle", 8.5f, Color.argb(185, 210, 230, 245), false)) }
        muteCard.addView(iconTv); muteCard.addView(mid, LinearLayout.LayoutParams(0, -2, 1f)); muteCard.addView(text(ctx, "›", 15f, Color.argb(180, 255, 255, 255), true))
        muteCard.setOnClickListener {
            am?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
            val nowMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am?.isStreamMute(AudioManager.STREAM_MUSIC) == true else !initialMuted
            iconTv.text = if (nowMuted) "🔇" else "🔈"
            titleTv.text = if (nowMuted) "Unmute TV Audio" else "Mute TV Audio"
        }
        panel.addView(muteCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
        addBackBtn(ctx, panel, lp, onBack)
    }

    fun buildInputsSubPanel(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onBack: () -> Unit) {
        addHeader(ctx, panel, lp, "DETECTED TV & HDMI INPUTS")
        val inputs = SystemCapabilities.getTvInputs(ctx)
        if (inputs.isEmpty()) {
            panel.addView(text(ctx, "No external HDMI tuner inputs reported by this TV.", 9.5f, Color.LTGRAY, false).apply {
                setPadding(dp(ctx, 4), dp(ctx, 8), dp(ctx, 4), dp(ctx, 12))
            })
        } else {
            for (inp in inputs) {
                panel.addView(makeActionRow(ctx, lp, "📺", inp.label, "Tap to configure or switch source") {
                    try { inp.setupIntent?.let { ctx.startActivity(it) } } catch (_: Exception) {}
                }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
            }
        }
        addBackBtn(ctx, panel, lp, onBack)
    }

    @SuppressLint("MissingPermission")
    fun buildBluetoothPanel(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onBack: () -> Unit) {
        val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bm?.adapter ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            panel.addView(text(ctx, "Bluetooth hardware not detected on this device.", 10.5f, Color.LTGRAY, false))
            addBackBtn(ctx, panel, lp, onBack)
            return
        }
        val hasBtPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        var isBtOn = try { adapter.isEnabled } catch (_: Exception) { false }

        val statusCard = makeActionRow(ctx, lp, "🔵", if (isBtOn) "Bluetooth Enabled" else "Bluetooth Turned Off", "Tap to toggle Bluetooth radio") {
            if (!hasBtPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (ctx as? Activity)?.requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQ_BLUETOOTH_CONNECT)
                return@makeActionRow
            }
            try {
                @Suppress("DEPRECATION")
                if (isBtOn) adapter.disable() else adapter.enable()
                isBtOn = !isBtOn
                Toast.makeText(ctx, if (isBtOn) "Bluetooth Turning On..." else "Bluetooth Turning Off...", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(ctx, "Bluetooth state controlled by TV OS", Toast.LENGTH_SHORT).show()
            }
        }
        panel.addView(statusCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 10) })

        addHeader(ctx, panel, lp, "PAIRED REMOTES & AUDIO")
        if (!hasBtPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            panel.addView(makeActionRow(ctx, lp, "🔓", "Allow Bluetooth Access", "Tap to view paired devices") {
                (ctx as? Activity)?.requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQ_BLUETOOTH_CONNECT)
            }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })
        } else {
            val bonded = try { adapter.bondedDevices?.toList() ?: emptyList() } catch (_: Exception) { emptyList() }
            if (bonded.isEmpty()) {
                panel.addView(text(ctx, "No paired Bluetooth devices found.", 9.5f, Color.LTGRAY, false))
            } else {
                for (dev in bonded) {
                    val dName = try { dev.name ?: dev.address } catch (_: Exception) { "Bluetooth Device" }
                    panel.addView(makeActionRow(ctx, lp, "🎧", dName, "Paired • ${dev.address}") {},
                        LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
                }
            }
        }
        addBackBtn(ctx, panel, lp, onBack)
    }

    fun buildStorageAndAppsPanel(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onBack: () -> Unit) {
        addHeader(ctx, panel, lp, "INTERNAL STORAGE")
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockSizeLong * stat.blockCountLong
        val freeBytes = stat.blockSizeLong * stat.availableBlocksLong
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
        val usedPct = ((usedBytes.toDouble() / totalBytes.coerceAtLeast(1L)) * 100).toInt()
        val totalGb = String.format(Locale.US, "%.1f GB", totalBytes / 1_073_741_824.0)
        val freeGb = String.format(Locale.US, "%.1f GB", freeBytes / 1_073_741_824.0)
        val usedGb = String.format(Locale.US, "%.1f GB", usedBytes / 1_073_741_824.0)

        val storageCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            background = cardBg(ctx, lp.accentColor, true)
        }
        storageCard.addView(text(ctx, "💾  Used: $usedGb / $totalGb ($usedPct%)", 10.5f, Color.WHITE, true))
        storageCard.addView(text(ctx, "Available Space: $freeGb Free", 9f, Color.rgb(125, 240, 175), true))
        panel.addView(storageCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 10) })

        addHeader(ctx, panel, lp, "INSTALLED APPS")
        val pm = ctx.packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase(Locale.ROOT) }

        for (info in apps) {
            val pkg = info.activityInfo.packageName
            val lbl = info.loadLabel(pm).toString()
            panel.addView(makeActionRow(ctx, lp, "📦", lbl, pkg) {
                try { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))) } catch (_: Exception) {}
            }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
        }
        addBackBtn(ctx, panel, lp, onBack)
    }

    fun getCurrentBrightnessPct(ctx: Context): Int = try {
        val raw = Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 200)
        ((raw / 255f) * 100f).toInt().coerceIn(10, 100)
    } catch (_: Exception) { 80 }

    fun applyBrightness(ctx: Context, pct: Int) {
        val clamped = pct.coerceIn(10, 100)
        (ctx as? Activity)?.window?.let { win ->
            val attr = win.attributes
            attr.screenBrightness = clamped / 100f
            win.attributes = attr
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(ctx)) {
            try {
                Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, ((clamped / 100f) * 255).toInt())
            } catch (_: Exception) {}
        }
    }

    fun applyWarmTintOverlay(ctx: Context, warmthPct: Int) {
        val root = (ctx as? Activity)?.window?.decorView as? ViewGroup ?: return
        var overlay = root.findViewWithTag<View>(WARM_OVERLAY_TAG)
        if (warmthPct <= 0) {
            if (overlay != null) root.removeView(overlay)
            return
        }
        if (overlay == null) {
            overlay = View(ctx).apply { tag = WARM_OVERLAY_TAG; isClickable = false; isFocusable = false }
            root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        }
        val alpha = ((warmthPct / 100f) * 110).toInt().coerceIn(0, 95)
        overlay.background = ColorDrawable(Color.argb(alpha, 255, 145, 45))
    }

    private fun isTimeoutMatch(cur: Int, opt: Int): Boolean =
        kotlin.math.abs(cur - opt) < 60_000 || (opt > 100_000_000 && cur > 100_000_000)

    private fun getFreeStorageSummary(): String = try {
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeGb = (stat.blockSizeLong * stat.availableBlocksLong) / 1_073_741_824.0
        String.format(Locale.US, "%.1f GB Free", freeGb)
    } catch (_: Exception) { "Space & Apps" }

    private fun getRamStatusText(ctx: Context): String = try {
        val mi = ActivityManager.MemoryInfo()
        (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        "Free: ${mi.availMem / (1024 * 1024)}MB / ${mi.totalMem / (1024 * 1024)}MB • Tap to boost"
    } catch (_: Exception) { "Optimize background memory" }

    private fun addSliderCard(ctx: Context, parent: LinearLayout, lp: LauncherPreferences, label: String, initPct: Int, onChange: (Int) -> Unit) {
        val card = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9)); background = cardBg(ctx, lp.accentColor, false) }
        val top = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val pct = text(ctx, "$initPct%", 10.5f, lp.accentColor, true)
        top.addView(text(ctx, label, 10.5f, Color.WHITE, true)); top.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f)); top.addView(pct)
        card.addView(top)
        val sb = SeekBar(ctx).apply {
            max = 100; progress = initPct; isFocusable = true
            setOnFocusChangeListener { _, f -> card.background = cardBg(ctx, lp.accentColor, f) }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) { pct.text = "$p%"; if (user) onChange(p) }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        card.addView(sb, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 5) })
        parent.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 7) })
    }

    private fun makeActionRow(ctx: Context, lp: LauncherPreferences, icon: String, title: String, sub: String, onClick: () -> Unit) =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9)); isFocusable = true; isClickable = true
            background = cardBg(ctx, lp.accentColor, false)
            setOnFocusChangeListener { v, f -> MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f); v.background = cardBg(ctx, lp.accentColor, f) }
            setOnClickListener { onClick() }
            addView(text(ctx, icon, 14f, lp.accentColor, true).apply { setPadding(0, 0, dp(ctx, 10), 0) })
            val mid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            mid.addView(text(ctx, title, 10.5f, Color.WHITE, true).apply { isSingleLine = true; ellipsize = TextUtils.TruncateAt.END })
            mid.addView(text(ctx, sub, 8.5f, Color.argb(185, 210, 230, 245), false).apply { isSingleLine = true; ellipsize = TextUtils.TruncateAt.END })
            addView(mid, LinearLayout.LayoutParams(0, -2, 1f))
            addView(text(ctx, "›", 15f, Color.argb(180, 255, 255, 255), true))
        }

    private fun addBackBtn(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onBack: () -> Unit) {
        val btn = TextView(ctx).apply {
            text = "‹  Back to Quick Settings"; textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); gravity = Gravity.CENTER; isFocusable = true; background = cardBg(ctx, lp.accentColor, true)
            setOnFocusChangeListener { v, f -> MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 3f) }
            setOnClickListener { onBack() }
        }
        panel.addView(btn, LinearLayout.LayoutParams(-1, dp(ctx, 40)).apply { topMargin = dp(ctx, 10); bottomMargin = dp(ctx, 20) })
    }

    private fun addHeader(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, label: String) {
        panel.addView(text(ctx, label.uppercase(Locale.ROOT), 9f, lp.accentColor, true).apply { letterSpacing = 0.08f },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 8); bottomMargin = dp(ctx, 5) })
    }

    private fun cardBg(ctx: Context, accent: Int, active: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (active) intArrayOf(LauncherUtils.withAlpha(accent, 115), Color.argb(65, 255, 255, 255))
        else intArrayOf(Color.argb(52, 255, 255, 255), Color.argb(26, 255, 255, 255))
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 14f)
        setStroke(dp(ctx, if (active) 2 else 1), if (active) accent else Color.argb(65, 255, 255, 255))
    }

    private fun text(ctx: Context, v: String, sz: Float, col: Int, bld: Boolean) = TextView(ctx).apply {
        text = v; textSize = sz; setTextColor(col); if (bld) typeface = Typeface.DEFAULT_BOLD
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
