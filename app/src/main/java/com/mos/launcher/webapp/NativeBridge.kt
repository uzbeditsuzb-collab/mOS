package com.mos.launcher.webapp

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

/** Everything the web interface is allowed to ask the phone to do. */
interface BridgeHost {
    fun onWebReady()
    fun launch(key: String, rectCssPx: FloatArray?)
    fun openAppInfo(key: String)
    fun openSystem(target: String)
    fun requestDefaultHome()
    fun setSystemBarsDark(dark: Boolean)
    fun setShowSystemWallpaper(show: Boolean)
    fun haptic()
    fun clearIconCache()
    fun control(name: String, on: Boolean?, value: Int?, final: Boolean)
}

/**
 * Restricted two-way channel between the bundled page and Kotlin.
 *
 * JS → Kotlin: `mOSNative.postMessage(json)`, injected by WebViewCompat.addWebMessageListener
 * and ONLY into frames whose origin is the local asset origin; non-main frames are ignored.
 * Only the message types below are understood; arguments are validated by [BridgeHost].
 * No addJavascriptInterface is used, so no Java object is exposed to the page.
 *
 * Kotlin → JS: `window.mOS.receive(json)` via evaluateJavascript, with JSON-quoted payloads.
 */
class NativeBridge(private val webView: WebView, private val host: BridgeHost) {

    var isReady = false
        private set

    /** Returns false if this phone's WebView is too old for origin-restricted messaging. */
    @SuppressLint("RequiresFeature")
    fun attach(): Boolean {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return false
        WebViewCompat.addWebMessageListener(webView, JS_OBJECT, setOf(MosWeb.ORIGIN)) { _, message, sourceOrigin, isMainFrame, _ ->
            if (!isMainFrame || sourceOrigin.toString().trimEnd('/') != MosWeb.ORIGIN) return@addWebMessageListener
            message.data?.let(::handle)
        }
        return true
    }

    private fun handle(raw: String) {
        if (raw.length > MAX_MESSAGE) return
        val m = try { JSONObject(raw) } catch (e: Exception) { return }
        try {
            when (m.optString("t")) {
                "ready" -> { isReady = true; host.onWebReady() }
                "launch" -> host.launch(m.getString("key"), m.optJSONObject("rect")?.let {
                    floatArrayOf(
                        it.optDouble("x").toFloat(), it.optDouble("y").toFloat(),
                        it.optDouble("w").toFloat(), it.optDouble("h").toFloat(),
                    )
                })
                "appInfo" -> host.openAppInfo(m.getString("key"))
                "system" -> host.openSystem(m.getString("what"))
                "requestDefault" -> host.requestDefaultHome()
                "bars" -> host.setSystemBarsDark(m.optBoolean("dark"))
                "wallpaper" -> host.setShowSystemWallpaper(m.optBoolean("system"))
                "haptic" -> host.haptic()
                "clearIcons" -> host.clearIconCache()
                "control" -> host.control(
                    name = m.getString("name"),
                    on = if (m.has("on")) m.getBoolean("on") else null,
                    value = if (m.has("v")) m.getInt("v") else null,
                    final = m.optBoolean("final", true),
                )
                else -> Log.w(TAG, "Ignored message type")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bad bridge message", e)
        }
    }

    /** Sends a message to the page. Dropped until the page has said it is ready. */
    fun send(type: String, payload: JSONObject = JSONObject()) {
        if (!isReady) return
        payload.put("t", type)
        webView.evaluateJavascript("window.mOS&&window.mOS.receive(${JSONObject.quote(payload.toString())})", null)
    }

    fun command(name: String) = send("cmd", JSONObject().put("name", name))

    fun toast(text: String) = send("toast", JSONObject().put("m", text))

    private companion object {
        const val TAG = "mOS/Bridge"
        const val JS_OBJECT = "mOSNative"
        const val MAX_MESSAGE = 4096
    }
}
