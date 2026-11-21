package com.mos.launcher.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mos.launcher.R
import com.mos.launcher.domain.model.AppInfo
import com.mos.launcher.ui.components.AppIcon
import com.mos.launcher.ui.components.AppsGlyph
import com.mos.launcher.ui.theme.OnWallpaper

private val DockShape = RoundedCornerShape(30.dp)
private val SlotShape = RoundedCornerShape(30)

/** Glass dock: two apps, the All-apps button, two apps — as in the prototype. */
@Composable
fun Dock(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAllAppsClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 52.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(DockShape)
            .background(OnWallpaper.Glass)
            .border(1.dp, OnWallpaper.GlassStroke, DockShape)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        apps.take(2).forEach { DockSlot(it, iconSize, onAppClick) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val label = stringResource(R.string.home_open_apps)
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(SlotShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(role = Role.Button, onClick = onAllAppsClick)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                AppsGlyph(color = Color.White)
            }
        }
        apps.drop(2).take(2).forEach { DockSlot(it, iconSize, onAppClick) }
    }
}

@Composable
private fun RowScope.DockSlot(app: AppInfo, iconSize: Dp, onAppClick: (AppInfo) -> Unit) {
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        AppIcon(
            app = app,
            size = iconSize,
            modifier = Modifier
                .clip(SlotShape)
                .clickable(role = Role.Button) { onAppClick(app) }
                .semantics { contentDescription = app.label },
        )
    }
}
