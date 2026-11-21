package com.mos.launcher.webapp

import android.graphics.Bitmap
import android.util.Base64
import android.util.LruCache
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import com.mos.launcher.data.icons.IconLoader
import com.mos.launcher.domain.model.AppInfo
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** URL-safe token for an app key, used in icon URLs: /icons/<token>_<version>.png */
fun iconToken(key: String): String =
    Base64.encodeToString(key.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

/**
 * Serves real app icons to the web interface on demand. The page uses
 * <img loading="lazy">, so only icons that are actually on screen are rendered.
 * Encoded PNGs are kept in a small LRU; the decoded bitmap is released
 * straight away because WebView keeps its own decoded copy.
 *
 * Called by WebView on a background thread, so blocking here is allowed.
 */
class IconPathHandler(
    private val iconLoader: IconLoader,
    private val appsByToken: () -> Map<String, AppInfo>,
    private val sizePx: Int,
) : WebViewAssetLoader.PathHandler {

    private val pngCache = object : LruCache<String, ByteArray>(MAX_BYTES) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    override fun handle(path: String): WebResourceResponse? {
        // path = "<token>_<version>.png"
        val file = path.substringAfterLast('/').removeSuffix(".png")
        val token = file.substringBeforeLast('_')
        val app = appsByToken()[token] ?: return null

        val bytes = pngCache.get(file) ?: render(app)?.also { pngCache.put(file, it) } ?: return null
        return WebResourceResponse(
            "image/png", null, 200, "OK",
            mapOf("Cache-Control" to "max-age=604800"), // version in URL busts it on app update
            ByteArrayInputStream(bytes),
        )
    }

    private fun render(app: AppInfo): ByteArray? {
        val bitmap = runBlocking { iconLoader.load(app, sizePx) } ?: return null
        val out = ByteArrayOutputStream(16 * 1024)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        iconLoader.forget(app, sizePx)
        return out.toByteArray()
    }

    fun clear() = pngCache.evictAll()

    private companion object {
        const val MAX_BYTES = 3 * 1024 * 1024 // ~150 icons at 128 px
    }
}
