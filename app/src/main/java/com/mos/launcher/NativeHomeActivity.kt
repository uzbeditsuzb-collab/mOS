package com.mos.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.mos.launcher.ui.components.LocalIconLoader
import com.mos.launcher.ui.home.HomeRoute
import com.mos.launcher.ui.home.HomeViewModel
import com.mos.launcher.ui.theme.MosTheme

/**
 * Milestone 1 Jetpack Compose home screen, kept intact. Since v1.0 the Home
 * activity is the WebView-based [MainActivity]; this screen is the fallback it
 * opens when the phone's Android System WebView is too old for the bridge.
 */
class NativeHomeActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels { HomeViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent bars with light icons: home content sits on the wallpaper.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val container = (application as MosApplication).container
        setContent {
            MosTheme {
                CompositionLocalProvider(LocalIconLoader provides container.iconLoader) {
                    HomeRoute(viewModel)
                }
            }
        }
    }

    /** Home pressed while mOS is already the foreground task. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.onHomePressed()
        }
    }

    /** After launching an app, come back to a clean home screen. */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) viewModel.closeDrawer()
    }
}
