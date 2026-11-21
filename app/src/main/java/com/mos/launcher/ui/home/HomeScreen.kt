package com.mos.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mos.launcher.R
import com.mos.launcher.domain.model.AppInfo
import com.mos.launcher.ui.home.components.ClockHeader
import com.mos.launcher.ui.home.components.Dock
import com.mos.launcher.ui.theme.OnWallpaper
import com.mos.launcher.ui.theme.WallpaperTextShadow

/** Stateless home screen drawn over the system wallpaper. */
@Composable
fun HomeScreen(
    dock: List<AppInfo>,
    isDefaultHome: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit,
    onLongPress: () -> Unit,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    val currentOpenDrawer by rememberUpdatedState(onOpenDrawer)
    val currentLongPress by rememberUpdatedState(onLongPress)

    Box(
        modifier = modifier
            .fillMaxSize()
            // Soft top/bottom scrim keeps text and dock legible on any wallpaper.
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.22f),
                    0.28f to Color.Transparent,
                    0.72f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.28f),
                )
            )
            .pointerInput(swipeThresholdPx) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -swipeThresholdPx) currentOpenDrawer() },
                ) { change, dragAmount ->
                    change.consume()
                    total += dragAmount
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentLongPress()
                    },
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            ClockHeader(
                showDefaultBanner = !isDefaultHome,
                onClockClick = onClockClick,
                onDateClick = onDateClick,
                onSetDefault = onSetDefault,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 34.dp),
            )

            // Home grid area — user shortcuts and folders arrive in Milestone 3.
            Spacer(Modifier.weight(1f))

            SwipeHint(onClick = onOpenDrawer, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(6.dp))
            Dock(apps = dock, onAppClick = onAppClick, onAllAppsClick = onOpenDrawer)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SwipeHint(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowUp,
            contentDescription = null,
            tint = OnWallpaper.Tertiary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.home_swipe_hint),
            style = MaterialTheme.typography.labelMedium.copy(shadow = WallpaperTextShadow),
            color = OnWallpaper.Secondary,
        )
    }
}
