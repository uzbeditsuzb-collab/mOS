package com.mos.launcher.data.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.mos.launcher.data.icons.IconLoader
import com.mos.launcher.domain.model.AppInfo
import com.mos.launcher.domain.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.text.Collator

/**
 * App discovery through the platform [LauncherApps] service — the API intended
 * for Home apps. It covers work profiles / Secure Folder and delivers
 * install/remove/update callbacks without any background service.
 */
class LauncherAppsRepository(
    private val context: Context,
    private val iconLoader: IconLoader,
) : AppRepository {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    override fun observeApps(): Flow<List<AppInfo>> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                iconLoader.evictPackage(packageName)
                trySend(Unit)
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                iconLoader.evictPackage(packageName)
                trySend(Unit)
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String>, user: UserHandle, replacing: Boolean,
            ) {
                trySend(Unit)
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String>, user: UserHandle, replacing: Boolean,
            ) {
                packageNames.forEach(iconLoader::evictPackage)
                trySend(Unit)
            }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        trySend(Unit) // initial load
        awaitClose { launcherApps.unregisterCallback(callback) }
    }
        .conflate() // bursts of package events collapse into one reload
        .map { queryApps() }
        .flowOn(Dispatchers.IO)

    override fun launch(app: AppInfo, sourceBounds: Rect?, options: Bundle?): Boolean = try {
        launcherApps.startMainActivity(app.component, app.user, sourceBounds, options)
        true
    } catch (e: Exception) {
        // ActivityNotFoundException, SecurityException (disabled/quiet profile), etc.
        Log.w(TAG, "Failed to launch ${app.component}", e)
        false
    }

    private fun queryApps(): List<AppInfo> {
        val me = Process.myUserHandle()
        val collator = Collator.getInstance()
        return userManager.userProfiles
            .flatMap { user ->
                val serial = userManager.getSerialNumberForUser(user)
                val activities = try {
                    launcherApps.getActivityList(null, user)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not list apps for $user", e)
                    emptyList()
                }
                activities.map { info ->
                    AppInfo(
                        key = "${info.componentName.flattenToShortString()}#$serial",
                        label = info.label?.toString()?.trim().orEmpty()
                            .ifEmpty { info.componentName.packageName },
                        packageName = info.componentName.packageName,
                        component = info.componentName,
                        user = user,
                        isWorkProfile = user != me,
                    )
                }
            }
            .filterNot { it.packageName == context.packageName } // hide mOS itself
            .distinctBy { it.key }
            .sortedWith(compareBy(collator) { it.label })
    }

    private companion object {
        const val TAG = "mOS/Apps"
    }
}
