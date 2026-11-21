package com.mos.launcher.di

import android.content.Context
import com.mos.launcher.data.apps.DefaultDockResolver
import com.mos.launcher.data.apps.LauncherAppsRepository
import com.mos.launcher.data.icons.IconLoader
import com.mos.launcher.domain.repository.AppRepository

/**
 * Minimal manual dependency container (no DI framework needed yet).
 * Created once per process in [com.mos.launcher.MosApplication].
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val iconLoader: IconLoader by lazy { IconLoader(appContext) }
    val appRepository: AppRepository by lazy { LauncherAppsRepository(appContext, iconLoader) }
    val dockResolver: DefaultDockResolver by lazy { DefaultDockResolver(appContext) }
}
