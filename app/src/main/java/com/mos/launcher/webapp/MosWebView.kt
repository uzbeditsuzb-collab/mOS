package com.mos.launcher.webapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature
import java.io.ByteArrayInputStream

/** Where the bundled mOS interface is served from. Never a remote server. */
object MosWeb {
    const val HOST = WebViewAssetLoader.DEFAULT_DOMAIN // appassets.androidplatform.net
    const val ORIGIN = "https://$HOST"
    const val START_URL = "$ORIGIN/assets/web/index.html"
}

/**
 * Builds the WebView that renders the mOS interface:
 * local content only, no file/content access, no zoom, fixed text scale,
 * transparent so the phone's wallpaper can show through when chosen.
 */
@SuppressLint("SetJavaScriptEnabled")
fun createMosWebView(
    context: Context,
    assetLoader: WebViewAssetLoader,
    onRendererGone: () -> Unit,
): WebView = WebView(context).apply {
    setBackgroundColor(Color.TRANSPARENT)
    overScrollMode = View.OVER_SCROLL_NEVER
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    // Long-press is handled by the interface itself; no text selection / native menu.
    isLongClickable = false
    setOnLongClickListener { true }
    isHapticFeedbackEnabled = true

    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true            // settings, layout and folders (localStorage)
        allowFileAccess = false
        allowContentAccess = false
        setSupportZoom(false)
        builtInZoomControls = false
        displayZoomControls = false
        textZoom = 100                      // keep the designed type scale
        useWideViewPort = true
        loadWithOverviewMode = false
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(false)
        mediaPlaybackRequiresUserGesture = true
        cacheMode = WebSettings.LOAD_DEFAULT
    }
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        // mOS has its own dark theme; never let WebView recolour it.
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false)
    }
    webViewClient = MosWebViewClient(assetLoader, onRendererGone)
}

private class MosWebViewClient(
    private val assetLoader: WebViewAssetLoader,
    private val onRendererGone: () -> Unit,
) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val url = request.url
        if (url.host == MosWeb.HOST) {
            return assetLoader.shouldInterceptRequest(url) ?: empty(404, "Not Found")
        }
        // Offline by design: nothing outside the APK is ever fetched.
        return empty(403, "Blocked")
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        request.url.host != MosWeb.HOST

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        // Low-memory kill or crash of the renderer: rebuild instead of crashing the launcher.
        Log.w("mOS/Web", "Renderer gone (crash=${detail.didCrash()}), recreating WebView")
        onRendererGone()
        return true
    }

    private fun empty(code: Int, reason: String) = WebResourceResponse(
        "text/plain", "utf-8", code, reason, emptyMap(), ByteArrayInputStream(ByteArray(0)),
    )
}
