package com.mos.launcher.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.mos.launcher.R

/** Safe wrappers around public system intents used by the home screen. */
object SystemActions {

    fun openWallpaperPicker(context: Context) {
        val picker = Intent(Intent.ACTION_SET_WALLPAPER)
        start(context, Intent.createChooser(picker, context.getString(R.string.choose_wallpaper)))
    }

    fun openHomeSettings(context: Context) {
        if (!start(context, DefaultHomeHelper.homeSettingsIntent(), showError = false)) {
            start(context, Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun openSystemSettings(context: Context) {
        start(context, Intent(Settings.ACTION_SETTINGS))
    }

    fun openAlarms(context: Context) {
        start(context, Intent(AlarmClock.ACTION_SHOW_ALARMS), showError = false)
    }

    fun openCalendar(context: Context) {
        start(
            context,
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR),
            showError = false,
        )
    }

    /** Returns true if an activity was started. */
    fun start(context: Context, intent: Intent, showError: Boolean = true): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        Log.w("mOS/System", "No handler for $intent", e)
        if (showError) {
            Toast.makeText(context, R.string.error_no_handler, Toast.LENGTH_SHORT).show()
        }
        false
    } catch (e: SecurityException) {
        Log.w("mOS/System", "Not allowed: $intent", e)
        false
    }
}
