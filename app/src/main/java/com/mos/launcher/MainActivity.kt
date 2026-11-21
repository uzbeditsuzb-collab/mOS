package com.mos.launcher

import android.app.ActivityOptions
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.webkit.WebViewAssetLoader
import com.mos.launcher.data.apps.RoleAppsResolver
import com.mos.launcher.di.AppContainer
import com.mos.launcher.domain.model.AppInfo
import com.mos.launcher.launcher.DefaultHomeHelper
import com.mos.launcher.launcher.SystemActions
import com.mos.launcher.launcher.SystemControls
import com.mos.launcher.webapp.BridgeHost
import com.mos.launcher.webapp.IconPathHandler
import com.mos.launcher.webapp.MosWeb
import com.mos.launcher.webapp.NativeBridge
import com.mos.launcher.webapp.createMosWebView
import com.mos.launcher.webapp.iconToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * mOS Home activity. Renders the bundled mOS interface (assets/web) in a
 * WebView and supplies everything native: real apps and icons, launching,
 * system bars, Back/Home, default-Home request and Control Center switches.
 *
 * Registered with ACTION_MAIN + CATEGORY_HOME + CATEGORY_DEFAULT.
 */
class MainActivity : ComponentActivity(), BridgeHost {

    private lateinit var container: AppContainer
    private lateinit var controls: SystemControls
    private lateinit var iconHandler: IconPathHandler
    private val roles by lazy { RoleAppsResolver(this) }
    private val launcherApps by lazy { getSystemService(LauncherApps::class.java) }

    private var webView: WebView? = null
    private var bridge: NativeBridge? = null

    @Volatile private var appsByKey: Map<String, AppInfo> = emptyMap()
    @Volatile private var appsByToken: Map<String, AppInfo> = emptyMap()
    private var appsPayload: JSONObject? = null
    private var insetsPayload = JSONObject().put("top", 24).put("bottom", 48)

    private val density get() = resources.displayMetrics.density

    private val defaultHomeRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { pushState() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true) // chrome://inspect
        container = (application as MosApplication).container
        controls = SystemControls(this) { pushState() }
        // Largest icon size the interface offers is 64 dp.
        iconHandler = IconPathHandler(container.iconLoader, { appsByToken }, (64 * density).roundToInt())

        if (!buildWebView()) return

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            // A launcher never exits on Back: the interface closes its top layer.
            override fun handleOnBackPressed() {
                bridge?.command("back")
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.appRepository.observeApps().collect { onApps(it) }
            }
        }
    }

    /** Creates the WebView. Returns false if we had to fall back to the Compose launcher. */
    private fun buildWebView(): Boolean {
        val loader = WebViewAssetLoader.Builder()
            .setDomain(MosWeb.HOST)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/icons/", iconHandler)
            .build()
        val view = createMosWebView(this, loader, onRendererGone = ::recreateWebView)
        val newBridge = NativeBridge(view, this)
        if (!newBridge.attach()) {
            Log.w(TAG, "WebView too old for the mOS bridge; opening the native fallback")
            view.destroy()
            startActivity(Intent(this, NativeHomeActivity::class.java))
            finish()
            return false
        }

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            insetsPayload = JSONObject()
                .put("top", (bars.top / density).roundToInt())
                .put("bottom", (max(bars.bottom, ime.bottom) / density).roundToInt())
            bridge?.send("insets", JSONObject(insetsPayload.toString()))
            insets
        }

        setContentView(view)
        webView = view
        bridge = newBridge
        view.loadUrl(MosWeb.START_URL)
        return true
    }

    private fun recreateWebView() {
        webView?.let { old ->
            (old.parent as? ViewGroup)?.removeView(old)
            old.destroy()
        }
        webView = null
        bridge = null
        buildWebView()
    }

    // ---------------------------------------------------------------- apps

    private suspend fun onApps(apps: List<AppInfo>) {
        appsByKey = apps.associateBy { it.key }
        appsByToken = apps.associateBy { iconToken(it.key) }
        appsPayload = withContext(Dispatchers.IO) {
            val versions = HashMap<String, Long>()
            val list = JSONArray()
            for (app in apps) {
                val v = versions.getOrPut(app.packageName) { lastUpdate(app.packageName) }
                list.put(
                    JSONObject()
                        .put("id", app.key)
                        .put("n", app.label)
                        .put("ic", iconToken(app.key))
                        .put("v", v)
                        .put("w", app.isWorkProfile)
                )
            }
            JSONObject().put("apps", list).put("defaults", roles.defaults(apps))
        }
        sendApps()
    }

    private fun sendApps() {
        appsPayload?.let { bridge?.send("apps", JSONObject(it.toString())) }
    }

    @Suppress("DEPRECATION")
    private fun lastUpdate(pkg: String): Long = try {
        packageManager.getPackageInfo(pkg, 0).lastUpdateTime / 1000
    } catch (e: PackageManager.NameNotFoundException) {
        0L // e.g. work-profile-only package
    }

    // ---------------------------------------------------------------- state

    private fun pushState() {
        val state = controls.state()
            .put("isDefault", DefaultHomeHelper.isDefaultHome(this))
            .put("dark", isNightMode())
            .put("is24", DateFormat.is24HourFormat(this))
        bridge?.send("state", state)
    }

    private fun isNightMode(): Boolean =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    // ---------------------------------------------------------------- lifecycle

    override fun onStart() {
        super.onStart()
        webView?.onResume()
        webView?.resumeTimers()
        controls.start()
        pushState()
        bridge?.command("onShown")
    }

    override fun onStop() {
        // After launching an app, come back to a clean home screen.
        bridge?.command("onHidden")
        controls.stop()
        webView?.onPause()
        webView?.pauseTimers()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            bridge?.command("home")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        pushState() // dark mode / 24-hour changes, without reloading the page
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            iconHandler.clear()
            container.iconLoader.clear()
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    // ---------------------------------------------------------------- BridgeHost

    override fun onWebReady() {
        bridge?.send("insets", JSONObject(insetsPayload.toString()))
        pushState()
        sendApps()
    }

    override fun launch(key: String, rectCssPx: FloatArray?) {
        val app = appsByKey[key] ?: return
        val view = webView
        var bounds: Rect? = null
        var options: Bundle? = null
        if (rectCssPx != null && view != null) {
            val d = density
            val loc = IntArray(2).also(view::getLocationOnScreen)
            val left = (rectCssPx[0] * d).roundToInt()
            val top = (rectCssPx[1] * d).roundToInt()
            val w = (rectCssPx[2] * d).roundToInt().coerceAtLeast(1)
            val h = (rectCssPx[3] * d).roundToInt().coerceAtLeast(1)
            bounds = Rect(loc[0] + left, loc[1] + top, loc[0] + left + w, loc[1] + top + h)
            options = ActivityOptions.makeScaleUpAnimation(view, left, top, w, h).toBundle()
        }
        if (!container.appRepository.launch(app, bounds, options)) {
            bridge?.toast(getString(R.string.error_launch_failed))
        }
    }

    override fun openAppInfo(key: String) {
        val app = appsByKey[key] ?: return
        try {
            launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
            bridge?.toast(getString(R.string.error_no_handler))
        }
    }

    override fun openSystem(target: String) {
        when (target) {
            "settings" -> SystemActions.openSystemSettings(this)
            "wallpaper" -> SystemActions.openWallpaperPicker(this)
            "alarms" -> SystemActions.openAlarms(this)
            "calendar" -> SystemActions.openCalendar(this)
            "homeSettings" -> SystemActions.openHomeSettings(this)
            "wifi" -> SystemActions.start(this, controls.wifiPanelIntent())
            "bluetooth" -> SystemActions.start(this, controls.bluetoothIntent())
        }
    }

    override fun requestDefaultHome() {
        try {
            defaultHomeRequest.launch(DefaultHomeHelper.requestDefaultHomeIntent(this))
        } catch (e: Exception) {
            SystemActions.openHomeSettings(this)
        }
    }

    override fun setSystemBarsDark(dark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = dark
            isAppearanceLightNavigationBars = dark
        }
    }

    override fun setShowSystemWallpaper(show: Boolean) {
        // Only composite the phone's wallpaper when the interface actually shows it.
        if (show) window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
    }

    override fun haptic() {
        webView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    override fun clearIconCache() {
        iconHandler.clear()
        container.iconLoader.clear()
        webView?.clearCache(false)
    }

    override fun control(name: String, on: Boolean?, value: Int?, final: Boolean) {
        when (name) {
            "torch" -> if (!controls.setTorch(on == true)) bridge?.toast(getString(R.string.cc_torch_unavailable))
            "dnd" -> if (!controls.setDnd(on == true)) askFor(controls.dndAccessIntent(), R.string.cc_allow_dnd)
            "rot" -> if (!controls.setAutoRotate(on == true)) askFor(controls.writeSettingsIntent(), R.string.cc_allow_settings)
            "bright" -> if (value != null && !controls.setBrightness(value) && final) {
                askFor(controls.writeSettingsIntent(), R.string.cc_allow_settings)
            }
            "vol" -> if (value != null) controls.setVolume(value)
            else -> return
        }
        if (final) pushState() // show the real result (e.g. permission still missing)
    }

    private fun askFor(intent: Intent, message: Int) {
        bridge?.toast(getString(message))
        SystemActions.start(this, intent)
    }

    private companion object {
        const val TAG = "mOS/Main"
    }
}
