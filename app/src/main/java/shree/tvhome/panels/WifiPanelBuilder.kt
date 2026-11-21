            @SuppressLint("MissingPermission")
            val results = if (hasLocPerm) {
                try { wm?.scanResults ?: emptyList() } catch (_: Exception) { emptyList() }
            } else emptyList()
            val unique = results.filter { !it.SSID.isNullOrBlank() }.distinctBy { it.SSID }.sortedByDescending { it.level }

            if (unique.isEmpty()) {
                dynamicListContainer.addView(TextView(ctx).apply {
                    text = if (hasLocPerm) "No nearby networks scanned yet. Tap Refresh Scan above."
                           else "Location permission is required by Android TV to list nearby Wi-Fi networks."
                    textSize = 9.5f; setTextColor(Color.argb(190, 210, 230, 250))
                    setPadding(dp(ctx, 4), dp(ctx, 8), dp(ctx, 4), dp(ctx, 8))
                })
            } else {
                for (scan in unique.take(8)) {
                    val isCurrent = scan.SSID == curSsid
                    val netRow = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9)); isFocusable = true
                        background = cardBg(ctx, lp.accentColor, isCurrent)
                        setOnFocusChangeListener { v, f ->
                            MotionEngine.animateFocus(v, f, lp.motionStyle, 1.02f, 4f)
                            v.background = cardBg(ctx, lp.accentColor, f || isCurrent)
                        }
                        setOnClickListener {
                            showGlossyWifiConnectDialog(ctx, lp, wm, scan.SSID)
                        }
                    }
                    netRow.addView(TextView(ctx).apply {
                        text = if (isCurrent) "✓  ${scan.SSID}" else "📶  ${scan.SSID}"
                        textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD
                        setTextColor(if (isCurrent) Color.rgb(120, 245, 175) else Color.WHITE)
                    }, LinearLayout.LayoutParams(0, -2, 1f))
                    netRow.addView(TextView(ctx).apply {
                        text = "›"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.LTGRAY)
                    })
                    dynamicListContainer.addView(netRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(ctx, 6) })
                }
            }
        }

        statusCard.setOnClickListener {
            try {
                wm?.isWifiEnabled = !isWifiOn
                renderDynamicWifiContent()
            } catch (_: Exception) {
                Toast.makeText(ctx, "Wi-Fi radio controlled by TV OS", Toast.LENGTH_SHORT).show()
            }
        }

        val refreshBtn = makeGlassBtn(ctx, lp, "🔄  Refresh Nearby Networks") {
            try { wm?.startScan() } catch (_: Exception) {}
            renderDynamicWifiContent()
            Toast.makeText(ctx, "Wi-Fi list refreshed", Toast.LENGTH_SHORT).show()
        }
        panel.addView(refreshBtn, LinearLayout.LayoutParams(-1, dp(ctx, 38)).apply { bottomMargin = dp(ctx, 10) })
        panel.addView(dynamicListContainer, LinearLayout.LayoutParams(-1, -2))
        renderDynamicWifiContent()
    }

    @Suppress("DEPRECATION")
    private fun showGlossyWifiConnectDialog(ctx: Context, lp: LauncherPreferences, wm: WifiManager?, ssid: String) {
        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 22), dp(ctx, 18), dp(ctx, 22), dp(ctx, 18))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.argb(242, 24, 36, 78), Color.argb(248, 12, 18, 44))
            ).apply {
                cornerRadius = LauncherUtils.dpF(ctx, 22f)
                setStroke(dp(ctx, 1), Color.argb(140, 255, 255, 255))
            }
        }

        box.addView(TextView(ctx).apply {
            text = "📶  Connect to $ssid"
            textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(ctx, 12))
        })

        val passInput = EditText(ctx).apply {
            isSingleLine = true
            hint = "Enter Wi-Fi Password"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.argb(150, 200, 220, 245))
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            background = GradientDrawable().apply {
                setColor(Color.argb(55, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(ctx, 12f)
                setStroke(dp(ctx, 1), lp.accentColor)
            }
        }
        box.addView(passInput, LinearLayout.LayoutParams(dp(ctx, 300), -2))

        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        btnRow.addView(makeGlassBtn(ctx, lp, "Cancel") { dialog.dismiss() },
            LinearLayout.LayoutParams(0, dp(ctx, 38), 1f).apply { rightMargin = dp(ctx, 8) })

        btnRow.addView(makeGlassBtn(ctx, lp, "✓  Connect") {
            val pwd = passInput.text.toString()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val spec = WifiNetworkSpecifier.Builder().setSsid(ssid).apply {
                    if (pwd.isNotEmpty()) setWpa2Passphrase(pwd)
                }.build()
                val req = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(spec)
                    .build()
                val cm = ctx.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                cm?.requestNetwork(req, ConnectivityManager.NetworkCallback())
            } else if (wm != null) {
                val conf = WifiConfiguration().apply {
                    SSID = "\"$ssid\""
                    if (pwd.isNotEmpty()) preSharedKey = "\"$pwd\"" else allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                }
                val netId = wm.addNetwork(conf)
                if (netId != -1) {
                    wm.disconnect()
                    wm.enableNetwork(netId, true)
                    wm.reconnect()
                }
            }
            Toast.makeText(ctx, "Connecting to $ssid...", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }, LinearLayout.LayoutParams(0, dp(ctx, 38), 1.2f))

        box.addView(btnRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 14) })
        dialog.setContentView(box)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun makeGlassBtn(ctx: Context, lp: LauncherPreferences, label: String, onClick: () -> Unit) = TextView(ctx).apply {
        text = label; textSize = 9.5f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
        gravity = Gravity.CENTER; isFocusable = true; isClickable = true
        background = cardBg(ctx, lp.accentColor, false)
        setOnFocusChangeListener { v, f ->
            MotionEngine.animateFocus(v, f, lp.motionStyle, 1.03f, 4f)
            v.background = cardBg(ctx, lp.accentColor, f)
        }
        setOnClickListener { onClick() }
    }

    private fun cardBg(ctx: Context, accent: Int, active: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (active) intArrayOf(LauncherUtils.withAlpha(accent, 115), Color.argb(65, 255, 255, 255))
        else intArrayOf(Color.argb(52, 255, 255, 255), Color.argb(26, 255, 255, 255))
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 14f)
        setStroke(dp(ctx, if (active) 2 else 1), if (active) accent else Color.argb(65, 255, 255, 255))
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
