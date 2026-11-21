package com.mos.launcher.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.mos.launcher.domain.model.AppInfo
import org.json.JSONArray
import org.json.JSONObject

/**
 * Finds the user's apps for standard roles (phone, camera, gallery, clock…)
 * so the first-run Home layout of the interface is filled with real apps.
 * No package names are hard-coded.
 */
class RoleAppsResolver(context: Context) {

    private val pm = context.packageManager

    private fun selector(category: String) = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category)

    private val roles: Map<String, Intent> = linkedMapOf(
        "phone" to Intent(Intent.ACTION_DIAL),
        "messages" to Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
        "browser" to Intent(Intent.ACTION_VIEW, Uri.parse("https://")).addCategory(Intent.CATEGORY_BROWSABLE),
        "camera" to Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
        "gallery" to selector(Intent.CATEGORY_APP_GALLERY),
        "calendar" to selector(Intent.CATEGORY_APP_CALENDAR),
        "email" to selector(Intent.CATEGORY_APP_EMAIL),
        "maps" to selector(Intent.CATEGORY_APP_MAPS),
        "music" to selector(Intent.CATEGORY_APP_MUSIC),
        "contacts" to selector(Intent.CATEGORY_APP_CONTACTS),
        "calculator" to selector(Intent.CATEGORY_APP_CALCULATOR),
        "clock" to Intent(AlarmClock.ACTION_SHOW_ALARMS),
        "settings" to Intent(Settings.ACTION_SETTINGS),
    )

    /** {dock, home, folder, fav, cc}: arrays of app keys. */
    fun defaults(apps: List<AppInfo>): JSONObject {
        val me = Process.myUserHandle()
        val personal = apps.filter { it.user == me }
        val byRole: Map<String, String?> = roles.mapValues { (_, intent) ->
            packageFor(intent)?.let { pkg -> personal.firstOrNull { it.packageName == pkg }?.key }
        }
        fun pick(vararg names: String): List<String> = names.mapNotNull { byRole[it] }.distinct()

        val dock = pick("phone", "messages", "browser", "camera")
        val home = pick("gallery", "calendar", "email", "maps", "music", "contacts").filter { it !in dock }
        val folder = pick("calculator", "clock", "settings").filter { it !in dock && it !in home }
        return JSONObject()
            .put("dock", JSONArray(dock))
            .put("home", JSONArray(home))
            .put("folder", JSONArray(folder))
            .put("fav", JSONArray(pick("camera", "gallery", "maps", "music", "contacts")))
            .put("cc", JSONArray(pick("calculator", "camera", "clock", "gallery")))
    }

    private fun packageFor(intent: Intent): String? = try {
        val default = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        if (default != null && default !in RESOLVER_PACKAGES) default
        else pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).firstOrNull()?.activityInfo?.packageName
    } catch (e: Exception) {
        null
    }

    private companion object {
        val RESOLVER_PACKAGES = setOf("android", "com.android.intentresolver")
    }
}
