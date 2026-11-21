package com.mos.launcher.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.MediaStore
import com.mos.launcher.domain.model.AppInfo

/**
 * Picks the initial dock apps from the user's default apps for standard roles
 * (phone, messages, browser, camera). No package names are hard-coded, so it
 * works with Samsung apps, Google apps or anything else the user prefers.
 *
 * The dock becomes user-customizable in a later milestone.
 */
class DefaultDockResolver(context: Context) {

    private val pm = context.packageManager

    private val roleIntents = listOf(
        Intent(Intent.ACTION_DIAL),
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://")).addCategory(Intent.CATEGORY_BROWSABLE),
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
    )

    fun resolve(apps: List<AppInfo>): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        val me = Process.myUserHandle()
        val personal = apps.filter { it.user == me }
        return roleIntents
            .mapNotNull { intent ->
                packageFor(intent)?.let { pkg -> personal.firstOrNull { it.packageName == pkg } }
            }
            .distinctBy { it.key }
    }

    private fun packageFor(intent: Intent): String? = try {
        val default = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        if (default != null && default !in RESOLVER_PACKAGES) {
            default
        } else {
            // No default chosen yet: take the first capable app.
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .firstOrNull()?.activityInfo?.packageName
        }
    } catch (e: Exception) {
        null
    }

    private companion object {
        /** Packages hosting the "choose an app" dialog rather than a real handler. */
        val RESOLVER_PACKAGES = setOf("android", "com.android.intentresolver")
    }
}
