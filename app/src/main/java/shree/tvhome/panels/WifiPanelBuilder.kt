package shree.tvhome.panels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.LauncherUtils
import shree.tvhome.source.MotionEngine
import shree.tvhome.source.TileThemePalette
import java.util.Locale

object WifiPanelBuilder {

    const val REQ_WIFI_LOCATION = 201

    @Suppress("DEPRECATION")
    fun build(ctx: Context, panel: LinearLayout, lp: LauncherPreferences, onRefreshPanel: () -> Unit) {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val isWifiOn = try { wm?.isWifiEnabled == true } catch (_: Exception) { true }

        val statusCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            isFocusable = true
            background = cardBg(ctx, lp.accentColor, isWifiOn)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f)
                v.background = cardBg(ctx, lp.accentColor, f || (try { wm?.isWifiEnabled == true } catch (_: Exception) { true }))
            }
        }

        val infoCol = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val connectedSsid = TileThemePalette.getConnectedSsid(ctx)
        val titleTv = TextView(ctx).apply {
            text = if (isWifiOn) connectedSsid else "Wi-Fi Turned Off"
            textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
        }
        val subTv = TextView(ctx).apply {
            text = if (isWifiOn) "● Online • Tap to toggle Wi-Fi" else "○ Tap to turn on Wi-Fi"
            textSize = 9.5f; setTextColor(if (isWifiOn) Color.rgb(120, 245, 175) else Color.LTGRAY)
        }
        infoCol.addView(titleTv)
        infoCol.addView(subTv)
        statusCard.addView(infoCol, LinearLayout.LayoutParams(0, -2, 1f))

        val togglePill = TextView(ctx).apply {
            text = if (isWifiOn) "ON" else "OFF"
            textSize = 10f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
            setPadding(dp(ctx, 12), dp(ctx, 5), dp(ctx, 12), dp(ctx, 5))
            background = GradientDrawable().apply {
                setColor(if (isWifiOn) Color.rgb(34, 185, 110) else Color.argb(85, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(ctx, 14f)
            }
        }
        statusCard.addView(togglePill)
        panel.addView(statusCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 8) })

        val dynamicListContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL; clipChildren = false; clipToPadding = false
        }

        val renderDynamicWifiContent = {
            dynamicListContainer.removeAllViews()
            val curOn = try { wm?.isWifiEnabled == true } catch (_: Exception) { true }
            val curSsid = TileThemePalette.getConnectedSsid(ctx)

            titleTv.text = if (curOn) curSsid else "Wi-Fi Turned Off"
            subTv.text = if (curOn) "● Online • Tap to toggle Wi-Fi" else "○ Tap to turn on Wi-Fi"
            subTv.setTextColor(if (curOn) Color.rgb(120, 245, 175) else Color.LTGRAY)
            togglePill.text = if (curOn) "ON" else "OFF"
            togglePill.background = GradientDrawable().apply {
                setColor(if (curOn) Color.rgb(34, 185, 110) else Color.argb(85, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(ctx, 14f)
            }

            if (curOn && wm != null) {
                val info = try { wm.connectionInfo } catch (_: Exception) { null }
                val ipInt = info?.ipAddress ?: 0
                val ipStr = if (ipInt != 0) String.format(
                    Locale.US, "%d.%d.%d.%d",
                    ipInt and 0xff, ipInt shr 8 and 0xff, ipInt shr 16 and 0xff, ipInt shr 24 and 0xff
                ) else "Connected"
                val speed = info?.linkSpeed ?: 0

                val metricCard = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL; setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8))
                    background = cardBg(ctx, lp.accentColor, false)
                }
                val col1 = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
                col1.addView(TextView(ctx).apply { text = "LINK SPEED"; textSize = 8f; setTextColor(Color.LTGRAY) })
                col1.addView(TextView(ctx).apply { text = if (speed > 0) "$speed Mbps" else "Active"; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
                metricCard.addView(col1, LinearLayout.LayoutParams(0, -2, 1f))

                val col2 = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
                col2.addView(TextView(ctx).apply { text = "IP ADDRESS"; textSize = 8f; setTextColor(Color.LTGRAY) })
                col2.addView(TextView(ctx).apply { text = ipStr; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
                metricCard.addView(col2, LinearLayout.LayoutParams(0, -2, 1f))
                dynamicListContainer.addView(metricCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 10) })
            }

            dynamicListContainer.addView(TextView(ctx).apply {
                text = "NEARBY NETWORKS (TAP TO CONNECT IN-APP)"; textSize = 9f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(lp.accentColor); letterSpacing = 0.08f
            }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })

            val hasLocPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (!hasLocPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val permBtn = makeGlassBtn(ctx, lp, "📍  Allow Location to Scan Wi-Fi") {
                    (ctx as? Activity)?.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQ_WIFI_LOCATION)
                }
                dynamicListContainer.addView(permBtn, LinearLayout.LayoutParams(-1, dp(ctx, 40)).apply { bottomMargin = dp(ctx, 8) })
            }
