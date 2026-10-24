package com.emberr.domain.util.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import com.emberr.core.android.requestAppDataEraseKeepingCloudAccountOnNextLaunch
import com.emberr.data.local.room.dao.CalendarTaskDao
import com.emberr.domain.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import kotlin.system.exitProcess

actual val isDesktopPlatform = false
actual val isDebugBuild: Boolean by lazy {
    val context = KoinPlatform.getKoin().get<Context>()
    (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
actual val appVersionName: String?
    get() {
        val context = KoinPlatform.getKoin().get<Context>()
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

actual fun showFeedback(message: String) {
    val context = KoinPlatform.getKoin().get<android.content.Context>()
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

actual fun triggerHapticFeedback() {
    val context = KoinPlatform.getKoin().get<Context>()
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
}

actual fun restartApplication() {
    val context = KoinPlatform.getKoin().get<Context>()
    val launchComponent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.component ?: return
    context.startActivity(Intent.makeRestartActivityTask(launchComponent))
    exitProcess(0)
}

actual suspend fun eraseAllAppData(keepCloudAccount: Boolean): Boolean {
    val context = KoinPlatform.getKoin().get<Context>()
    if (keepCloudAccount) {
        val wasRequested = withContext(Dispatchers.IO) {
            cancelEveryTaskReminder()
            requestAppDataEraseKeepingCloudAccountOnNextLaunch(context)
        }
        if (wasRequested) restartApplication()
        return wasRequested
    }
    val activityManager = context.getSystemService(ActivityManager::class.java) ?: return false
    return activityManager.clearApplicationUserData()
}

private suspend fun cancelEveryTaskReminder() {
    val koin = KoinPlatform.getKoin()
    runCatching {
        val reminderScheduler = koin.get<ReminderScheduler>()
        koin.get<CalendarTaskDao>().getAllTasksAcrossSpaces().forEach { task -> reminderScheduler.cancel(task.blockId) }
    }
}
