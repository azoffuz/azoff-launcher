package uz.azoff.launcher

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow

/** Bildirishnoma nuqtalari (badge) uchun xizmat */
class NotificationBadgeService : NotificationListenerService() {
    companion object {
        val counts = MutableStateFlow<Map<String, Int>>(emptyMap())

        fun isEnabled(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    override fun onListenerConnected() = update()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = update()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = update()
    override fun onListenerDisconnected() {
        counts.value = emptyMap()
    }

    private fun update() {
        try {
            val list = activeNotifications ?: return
            counts.value = list
                .filter { !it.isOngoing && it.isClearable }
                .groupingBy { it.packageName }
                .eachCount()
        } catch (_: Exception) {
        }
    }
}

/** Ekranni qulflash va "so'nggi ilovalar" uchun maxsus imkoniyat xizmati */
class LockAccessibilityService : AccessibilityService() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        var instance: LockAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}

object SystemActions {
    @SuppressLint("WrongConstant")
    private fun statusBar(context: Context, method: String) {
        try {
            val sb = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod(method).invoke(sb)
        } catch (_: Exception) {
        }
    }

    fun expandNotifications(context: Context) = statusBar(context, "expandNotificationsPanel")
    fun expandQuickSettings(context: Context) = statusBar(context, "expandSettingsPanel")

    fun accessibilityEnabled(context: Context): Boolean {
        if (LockAccessibilityService.instance != null) return true
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = ComponentName(context, LockAccessibilityService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(me, ignoreCase = true) }
    }

    fun lockScreen(): Boolean {
        val s = LockAccessibilityService.instance ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
    }

    fun recents(): Boolean =
        LockAccessibilityService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS) ?: false

    fun openAccessibilitySettings(context: Context) =
        safeStart(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun openNotificationAccess(context: Context) =
        safeStart(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    fun openHomeSettings(context: Context) {
        if (!safeStart(context, Intent(Settings.ACTION_HOME_SETTINGS))) {
            safeStart(context, Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun isDefaultLauncher(context: Context): Boolean {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val ri = context.packageManager.resolveActivity(i, 0)
        return ri?.activityInfo?.packageName == context.packageName
    }

    fun setWallpaper(context: Context) {
        val i = Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), null)
        safeStart(context, i)
    }
}
