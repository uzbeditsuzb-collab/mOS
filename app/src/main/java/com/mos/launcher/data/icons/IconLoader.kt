package com.mos.launcher.data.icons

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.mos.launcher.domain.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads app icons on demand (only for items actually on screen) and keeps them
 * in a memory-bounded LRU cache. Never throws: missing icons fall back to the
 * system default activity icon, and as a last resort return null so the UI
 * can draw a placeholder.
 */
class IconLoader(context: Context) {

    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val densityDpi = appContext.resources.displayMetrics.densityDpi

    // 1/16 of the app heap, measured in KB. ~16 MB on a 256 MB heap.
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 16).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun cached(app: AppInfo, sizePx: Int): Bitmap? = cache.get(cacheKey(app, sizePx))

    suspend fun load(app: AppInfo, sizePx: Int): Bitmap? {
        cached(app, sizePx)?.let { return it }
        return withContext(Dispatchers.IO) {
            val drawable = loadDrawable(app) ?: return@withContext null
            try {
                drawable.toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                    .also { cache.put(cacheKey(app, sizePx), it) }
            } catch (e: Exception) {
                Log.w(TAG, "Icon render failed for ${app.component}", e)
                null
            }
        }
    }

    /** Drops one rendered size (used when the web UI keeps its own PNG copy). */
    fun forget(app: AppInfo, sizePx: Int) {
        cache.remove(cacheKey(app, sizePx))
    }

    fun clear() = cache.evictAll()

    fun evictPackage(packageName: String) {
        cache.snapshot().keys
            .filter { it.startsWith("$packageName/") }
            .forEach(cache::remove)
    }

    private fun loadDrawable(app: AppInfo): Drawable? = try {
        val info = launcherApps.resolveActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component),
            app.user,
        )
        // Badged icon adds the work-profile briefcase where applicable.
        info?.getBadgedIcon(densityDpi) ?: appContext.packageManager.defaultActivityIcon
    } catch (e: Exception) {
        Log.w(TAG, "Icon unavailable for ${app.component}", e)
        runCatching { appContext.packageManager.defaultActivityIcon }.getOrNull()
    }

    // flattenToShortString() starts with "package/", which evictPackage relies on.
    private fun cacheKey(app: AppInfo, sizePx: Int) = "${app.key}@$sizePx"

    private companion object {
        const val TAG = "mOS/Icons"
    }
}
