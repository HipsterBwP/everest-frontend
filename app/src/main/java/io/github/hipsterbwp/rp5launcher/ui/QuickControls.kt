package io.github.hipsterbwp.rp5launcher.ui

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import kotlin.math.roundToInt

/**
 * Device controls for the quick menu. Apps can set brightness system-wide only with the
 * "modify system settings" permission; without it brightness applies while Everest is open.
 * Wi-Fi and Bluetooth can't be switched by apps on Android 10+, so those open Android's panels.
 */
class QuickControls(private val activity: Activity) {
    private val audio = activity.getSystemService(AudioManager::class.java)

    val maxVolume: Int get() = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val volume: Int get() = audio.getStreamVolume(AudioManager.STREAM_MUSIC)

    fun setVolume(level: Int) = audio.setStreamVolume(AudioManager.STREAM_MUSIC, level.coerceIn(0, maxVolume), 0)

    val systemWide: Boolean get() = Settings.System.canWrite(activity)

    /** 0..1. */
    val brightness: Float
        get() {
            val window = activity.window.attributes.screenBrightness
            if (!systemWide && window >= 0f) return window
            return Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, MID) / MAX.toFloat()
        }

    fun setBrightness(level: Float) {
        val value = level.coerceIn(MIN_LEVEL, 1f)
        if (systemWide) {
            val resolver = activity.contentResolver
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, (value * MAX).roundToInt().coerceIn(1, MAX))
            // Drop any app-only override so the system value shows.
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = -1f }
        } else {
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = value }
        }
    }

    fun allowSystemWide() = open(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${activity.packageName}")))

    val wifiConnected: Boolean
        get() {
            val cm = activity.getSystemService(ConnectivityManager::class.java)
            return cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }

    /** Battery percent (or null) and whether it's charging. */
    val battery: Pair<Int?, Boolean>
        get() {
            val intent = activity.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            return (if (level >= 0 && scale > 0) level * 100 / scale else null) to charging
        }

    fun openWifi() = open(Intent(Settings.Panel.ACTION_WIFI))
    fun openBluetooth() = open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    fun openSettings() = open(Intent(Settings.ACTION_SETTINGS))

    private fun open(intent: Intent) {
        runCatching { activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private companion object {
        const val MAX = 255
        const val MID = 128
        const val MIN_LEVEL = 0.02f
    }
}
