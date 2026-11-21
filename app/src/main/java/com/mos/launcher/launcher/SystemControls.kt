package com.mos.launcher.launcher

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * The Control Center's real switches, using only public APIs available to
 * normal apps. Where Android requires the user's approval (Do not disturb,
 * brightness, auto-rotate) the setter returns false and the caller opens the
 * matching Android permission screen. Wi-Fi and Bluetooth cannot be toggled by
 * apps on modern Android, so those open Android's own panel instead.
 */
class SystemControls(context: Context, private val onChanged: () -> Unit) {

    private val ctx = context.applicationContext
    private val resolver = ctx.contentResolver
    private val camera = ctx.getSystemService(CameraManager::class.java)
    private val audio = ctx.getSystemService(AudioManager::class.java)
    private val notifications = ctx.getSystemService(NotificationManager::class.java)
    private val wifi = ctx.getSystemService(WifiManager::class.java)
    private val bluetooth = ctx.getSystemService(BluetoothManager::class.java)?.adapter

    private val torchCameraId: String? by lazy {
        try {
            camera.cameraIdList.firstOrNull {
                camera.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }
    private var torchOn = false
    private var listening = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId && enabled != torchOn) {
                torchOn = enabled
                onChanged()
            }
        }
    }

    /** Listen only while mOS is visible (no background work). */
    fun start() {
        if (listening) return
        listening = true
        try {
            camera.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Log.w(TAG, "Torch callback unavailable", e)
        }
    }

    fun stop() {
        if (!listening) return
        listening = false
        runCatching { camera.unregisterTorchCallback(torchCallback) }
    }

    fun state(): JSONObject = JSONObject()
        .put("wifi", runCatching { wifi?.isWifiEnabled == true }.getOrDefault(false))
        .put("bt", runCatching { bluetooth?.isEnabled == true }.getOrDefault(false))
        .put("torch", torchOn)
        .put("hasTorch", torchCameraId != null)
        .put("dnd", isDndOn())
        .put("rot", Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1)
        .put("bright", brightnessPercent())
        .put("vol", volumePercent())

    fun setTorch(on: Boolean): Boolean {
        val id = torchCameraId ?: return false
        return try {
            camera.setTorchMode(id, on)
            true
        } catch (e: Exception) { // camera in use, etc.
            Log.w(TAG, "Torch failed", e)
            false
        }
    }

    fun hasDndAccess(): Boolean = notifications.isNotificationPolicyAccessGranted

    fun setDnd(on: Boolean): Boolean {
        if (!hasDndAccess()) return false
        notifications.setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL
        )
        return true
    }

    fun canWriteSettings(): Boolean = Settings.System.canWrite(ctx)

    fun setAutoRotate(on: Boolean): Boolean {
        if (!canWriteSettings()) return false
        return Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, if (on) 1 else 0)
    }

    fun setBrightness(percent: Int): Boolean {
        if (!canWriteSettings()) return false
        Settings.System.putInt(
            resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
        val raw = (percent.coerceIn(1, 100) * 255 / 100f).roundToInt().coerceIn(1, 255)
        return Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, raw)
    }

    fun setVolume(percent: Int): Boolean = try {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (percent.coerceIn(0, 100) * max / 100f).roundToInt(), 0)
        true
    } catch (e: SecurityException) {
        false
    }

    // ---- Android screens ----

    fun wifiPanelIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_WIFI)
        else Intent(Settings.ACTION_WIFI_SETTINGS)

    fun bluetoothIntent(): Intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)

    fun dndAccessIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun writeSettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))

    // ---- helpers ----

    private fun isDndOn(): Boolean {
        val f = notifications.currentInterruptionFilter
        return f != NotificationManager.INTERRUPTION_FILTER_ALL && f != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    private fun brightnessPercent(): Int {
        val raw = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        return (raw * 100 / 255f).roundToInt().coerceIn(1, 100)
    }

    private fun volumePercent(): Int = try {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        (audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / max).roundToInt()
    } catch (e: Exception) {
        0
    }

    private companion object {
        const val TAG = "mOS/Controls"
    }
}
