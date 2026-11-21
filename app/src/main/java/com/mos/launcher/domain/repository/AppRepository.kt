package com.mos.launcher.domain.repository

import android.graphics.Rect
import android.os.Bundle
import com.mos.launcher.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface AppRepository {

    /**
     * Emits the alphabetically sorted list of launchable apps across all user
     * profiles, and re-emits whenever apps are installed, removed or updated.
     */
    fun observeApps(): Flow<List<AppInfo>>

    /**
     * Starts the app's main activity. Returns false if it could not be opened.
     * [sourceBounds] (screen px) and [options] drive Android's open animation.
     */
    fun launch(app: AppInfo, sourceBounds: Rect? = null, options: Bundle? = null): Boolean
}
