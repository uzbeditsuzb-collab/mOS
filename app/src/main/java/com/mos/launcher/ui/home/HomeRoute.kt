package com.mos.launcher.ui.home

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mos.launcher.R
import com.mos.launcher.domain.model.AppInfo
import com.mos.launcher.launcher.DefaultHomeHelper
import com.mos.launcher.launcher.SystemActions
import com.mos.launcher.ui.drawer.AppDrawer
import com.mos.launcher.ui.home.components.HomeMenuSheet
import com.mos.launcher.ui.theme.MosMotion
import com.mos.launcher.ui.theme.SystemBarIcons

/** Wires state, system intents and navigation for the home screen + drawer. */
@Composable
fun HomeRoute(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val dock by viewModel.dock.collectAsStateWithLifecycle()
    val drawerOpen by viewModel.drawerOpen.collectAsStateWithLifecycle()
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    // Re-checked every time mOS returns to the foreground.
    var isDefaultHome by remember { mutableStateOf(DefaultHomeHelper.isDefaultHome(context)) }
    LifecycleResumeEffect(Unit) {
        isDefaultHome = DefaultHomeHelper.isDefaultHome(context)
        onPauseOrDispose { }
    }
    val defaultHomeRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { isDefaultHome = DefaultHomeHelper.isDefaultHome(context) }

    val requestDefaultHome: () -> Unit = {
        try {
            defaultHomeRequest.launch(DefaultHomeHelper.requestDefaultHomeIntent(context))
        } catch (e: ActivityNotFoundException) {
            SystemActions.openHomeSettings(context)
        }
    }
    val launchApp: (AppInfo) -> Unit = { app ->
        if (!viewModel.launch(app)) {
            Toast.makeText(context, R.string.error_launch_failed, Toast.LENGTH_SHORT).show()
        }
    }

    // A launcher never "exits" on Back; Back only closes the drawer.
    BackHandler(enabled = true) {
        if (drawerOpen) viewModel.closeDrawer()
    }

    SystemBarIcons(darkIcons = drawerOpen && !isSystemInDarkTheme())

    val homeAlpha by animateFloatAsState(
        targetValue = if (drawerOpen) 0f else 1f,
        animationSpec = tween(MosMotion.ExitMillis, easing = MosMotion.Easing),
        label = "homeAlpha",
    )
    val homeScale by animateFloatAsState(
        targetValue = if (drawerOpen) 0.94f else 1f,
        animationSpec = tween(MosMotion.EnterMillis, easing = MosMotion.Easing),
        label = "homeScale",
    )

    Box(Modifier.fillMaxSize()) {
        HomeScreen(
            dock = dock,
            isDefaultHome = isDefaultHome,
            onAppClick = launchApp,
            onOpenDrawer = viewModel::openDrawer,
            onLongPress = { menuOpen = true },
            onClockClick = { SystemActions.openAlarms(context) },
            onDateClick = { SystemActions.openCalendar(context) },
            onSetDefault = requestDefaultHome,
            modifier = Modifier.graphicsLayer {
                alpha = homeAlpha
                scaleX = homeScale
                scaleY = homeScale
            },
        )

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(tween(MosMotion.EnterMillis, easing = MosMotion.Easing)) { it / 10 } +
                fadeIn(tween(MosMotion.ExitMillis)),
            exit = slideOutVertically(tween(MosMotion.ExitMillis, easing = MosMotion.Easing)) { it / 10 } +
                fadeOut(tween(200)),
        ) {
            AppDrawer(
                apps = apps,
                onAppClick = launchApp,
                onClose = viewModel::closeDrawer,
            )
        }
    }

    if (menuOpen) {
        HomeMenuSheet(
            onDismiss = { menuOpen = false },
            onWallpaper = {
                menuOpen = false
                SystemActions.openWallpaperPicker(context)
            },
            onDefaultHome = {
                menuOpen = false
                SystemActions.openHomeSettings(context)
            },
            onSystemSettings = {
                menuOpen = false
                SystemActions.openSystemSettings(context)
            },
        )
    }
}
