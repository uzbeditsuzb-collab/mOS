package com.mos.launcher.domain.model

import android.content.ComponentName
import android.os.UserHandle

/**
 * A launchable activity visible to mOS.
 *
 * @property key Stable unique id (component + user serial). Used for list keys,
 * icon cache keys and, in later milestones, persisted shortcuts/folders.
 */
data class AppInfo(
    val key: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val isWorkProfile: Boolean,
)
