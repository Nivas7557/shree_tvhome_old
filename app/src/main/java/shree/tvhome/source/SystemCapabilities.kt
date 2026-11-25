package shree.tvhome.source

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.tv.TvInputManager
import android.os.Build
import android.provider.Settings

object SystemCapabilities {

    data class DiscoveredSetting(
        val icon: String,
        val title: String,
        val subtitle: String,
        val intent: Intent
    )

    data class DiscoveredTvInput(
        val label: String,
        val id: String,
        val setupIntent: Intent?
    )

    data class DiscoveredAudioDevice(
        val typeName: String,
        val productName: String,
        val id: Int
    )

    private val audioTypeLabels = mapOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP to "Bluetooth Audio",
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO to "Bluetooth Audio",
        AudioDeviceInfo.TYPE_HDMI to "HDMI Audio",
        AudioDeviceInfo.TYPE_HDMI_ARC to "HDMI Audio",
        AudioDeviceInfo.TYPE_USB_DEVICE to "USB Audio",
        AudioDeviceInfo.TYPE_USB_HEADSET to "USB Audio",
        AudioDeviceInfo.TYPE_WIRED_HEADSET to "Wired Audio",
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES to "Wired Audio"
    )

    fun hasBluetoothHardware(ctx: Context): Boolean {
        val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bm?.adapter ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
        return adapter != null
    }

    fun isVolumeFixed(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return am.isVolumeFixed
    }

    fun resolveIntent(ctx: Context, action: String): Intent? {
        val intent = Intent(action)
        return if (intent.resolveActivity(ctx.packageManager) != null) intent else null
    }

    fun getAvailableSettings(ctx: Context, filterActions: Set<String>? = null): List<DiscoveredSetting> {
        val pm = ctx.packageManager
        val catalog = listOf(
            DiscoveredSetting("⚙", "TV Device Settings", "Full system & device preferences", Intent(Settings.ACTION_SETTINGS)),
            DiscoveredSetting("📺", "TV Picture & Display", "Hardware sharpness, contrast & resolution", Intent(Settings.ACTION_DISPLAY_SETTINGS)),
            DiscoveredSetting("🔊", "TV Sound & Dolby", "System audio & equalizer settings", Intent(Settings.ACTION_SOUND_SETTINGS)),
            DiscoveredSetting("📶", "Network Settings", "Advanced IP, proxy & ethernet", Intent(Settings.ACTION_WIFI_SETTINGS)),
            DiscoveredSetting("🔵", "Bluetooth Pairing", "Pair new remotes & gamepads", Intent(Settings.ACTION_BLUETOOTH_SETTINGS)),
            DiscoveredSetting("📦", "System App Manager", "All system & background apps", Intent(Settings.ACTION_APPLICATION_SETTINGS))
        )
        return catalog.filter { item ->
            val actionMatches = filterActions == null || item.intent.action in filterActions
            actionMatches && item.intent.resolveActivity(pm) != null
        }
    }

    fun getTvInputs(ctx: Context): List<DiscoveredTvInput> {
        val tvMgr = ctx.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()
        return try {
            tvMgr.tvInputList.map { info ->
                val lbl = try { info.loadLabel(ctx).toString() } catch (_: Exception) { info.id }
                DiscoveredTvInput(
                    label = lbl,
                    id = info.id,
                    setupIntent = try { info.createSetupIntent() } catch (_: Exception) { null }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getAudioOutputs(ctx: Context): List<DiscoveredAudioDevice> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return emptyList()
        return try {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .distinctBy { it.type }
                .map { dev ->
                    val typeLabel = mapAudioDeviceType(dev.type)
                    val prod = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: typeLabel
                    DiscoveredAudioDevice(typeLabel, prod, dev.id)
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun mapAudioDeviceType(type: Int): String = audioTypeLabels[type] ?: "Built-in Speaker"
}
