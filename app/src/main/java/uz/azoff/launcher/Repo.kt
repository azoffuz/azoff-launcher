package uz.azoff.launcher

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.provider.ContactsContract
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.HttpURLConnection
import java.net.URL

data class AppInfo(
    val key: String,
    val packageName: String,
    val className: String,
    val label: String,
    val icon: ImageBitmap,
    val category: Int,
    val user: UserHandle,
) {
    val component: ComponentName get() = ComponentName(packageName, className)
}

class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    val iconPacks = IconPackManager(context)

    suspend fun loadApps(iconPack: String, iconSizePx: Int): List<AppInfo> = withContext(Dispatchers.IO) {
        val pack = if (iconPack.isNotEmpty()) iconPacks.load(iconPack) else null
        val user = Process.myUserHandle()
        launcherApps.getActivityList(null, user)
            .filter { it.componentName.packageName != context.packageName }
            .mapNotNull { info ->
                try {
                    val cn = info.componentName
                    val drawable: Drawable = pack?.getIcon(cn) ?: info.getBadgedIcon(0)
                    AppInfo(
                        key = cn.flattenToString(),
                        packageName = cn.packageName,
                        className = cn.className,
                        label = info.label?.toString() ?: cn.packageName,
                        icon = drawable.toBitmap(iconSizePx, iconSizePx).asImageBitmap(),
                        category = info.applicationInfo.category,
                        user = user,
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
    }

    fun registerCallback(onChange: () -> Unit): LauncherApps.Callback {
        val cb = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = onChange()
            override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = onChange()
            override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = onChange()
        }
        launcherApps.registerCallback(cb, Handler(Looper.getMainLooper()))
        return cb
    }

    fun unregister(cb: LauncherApps.Callback) {
        try { launcherApps.unregisterCallback(cb) } catch (_: Exception) {}
    }

    fun start(app: AppInfo, animations: Boolean, bounds: Rect? = null): Boolean = try {
        val opts = if (animations) null else ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()
        launcherApps.startMainActivity(app.component, app.user, bounds, opts)
        true
    } catch (e: Exception) {
        false
    }

    fun canUseShortcuts(): Boolean = try { launcherApps.hasShortcutHostPermission() } catch (e: Exception) { false }

    fun shortcuts(app: AppInfo): List<ShortcutInfo> {
        if (!canUseShortcuts()) return emptyList()
        return try {
            val q = LauncherApps.ShortcutQuery()
                .setPackage(app.packageName)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
            (launcherApps.getShortcuts(q, app.user) ?: emptyList())
                .filter { it.isEnabled }
                .sortedBy { it.rank }
                .take(5)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun shortcutIcon(s: ShortcutInfo, sizePx: Int): ImageBitmap? = try {
        launcherApps.getShortcutIconDrawable(s, context.resources.displayMetrics.densityDpi)
            ?.toBitmap(sizePx, sizePx)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }

    fun startShortcut(s: ShortcutInfo): Boolean = try {
        launcherApps.startShortcut(s, null, null); true
    } catch (e: Exception) {
        false
    }

    fun openAppInfo(app: AppInfo) {
        try {
            launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
            safeStart(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
        }
    }

    fun uninstall(app: AppInfo) {
        safeStart(context, Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
    }

    /** Birinchi ishga tushirishda dock va bosh ekran uchun standart ilovalar */
    fun defaultApps(apps: List<AppInfo>): Pair<List<String>, List<String>> {
        val pm = context.packageManager
        fun find(intent: Intent): String? {
            val ri = try { pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) } catch (e: Exception) { null }
            val pkg = ri?.activityInfo?.packageName ?: return null
            return apps.firstOrNull { it.packageName == pkg }?.key
        }
        fun cat(c: String) = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, c)
        val dock = listOfNotNull(
            find(Intent(Intent.ACTION_DIAL)),
            find(cat(Intent.CATEGORY_APP_MESSAGING)),
            find(cat(Intent.CATEGORY_APP_BROWSER)),
            find(Intent("android.media.action.IMAGE_CAPTURE")),
        ).distinct()
        val home = listOfNotNull(
            find(cat(Intent.CATEGORY_APP_GALLERY)),
            find(cat(Intent.CATEGORY_APP_CALENDAR)),
            find(cat(Intent.CATEGORY_APP_MUSIC)),
            find(cat(Intent.CATEGORY_APP_MAPS)),
            find(cat(Intent.CATEGORY_APP_EMAIL)),
            find(cat(Intent.CATEGORY_APP_CONTACTS)),
            apps.firstOrNull { it.packageName == "com.android.vending" }?.key,
            find(Intent(Settings.ACTION_SETTINGS)),
        ).distinct().filter { it !in dock }
        return dock to home
    }
}

fun safeStart(context: Context, intent: Intent): Boolean = try {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    true
} catch (e: Exception) {
    false
}

// ---------------- Icon paketlar ----------------

class IconPackManager(private val context: Context) {
    data class Pack(val packageName: String, val label: String)

    fun installedPacks(): List<Pack> {
        val pm = context.packageManager
        val actions = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME", "com.gau.go.launcherex.theme")
        return actions.flatMap { a ->
            try { pm.queryIntentActivities(Intent(a), 0) } catch (e: Exception) { emptyList() }
        }
            .map { it.activityInfo.packageName }
            .distinct()
            .map { p ->
                val label = try { pm.getApplicationLabel(pm.getApplicationInfo(p, 0)).toString() } catch (e: Exception) { p }
                Pack(p, label)
            }
            .sortedBy { it.label.lowercase() }
    }

    @SuppressLint("DiscouragedApi")
    fun load(packageName: String): LoadedPack? = try {
        val res = context.packageManager.getResourcesForApplication(packageName)
        val map = HashMap<String, String>()
        val xmlId = res.getIdentifier("appfilter", "xml", packageName)
        val parser: XmlPullParser = if (xmlId != 0) {
            res.getXml(xmlId)
        } else {
            XmlPullParserFactory.newInstance().newPullParser().apply {
                setInput(res.assets.open("appfilter.xml"), "utf-8")
            }
        }
        var ev = parser.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG && parser.name == "item") {
                val comp = parser.getAttributeValue(null, "component")
                val dr = parser.getAttributeValue(null, "drawable")
                if (comp != null && dr != null) map[comp] = dr
            }
            ev = parser.next()
        }
        LoadedPack(res, packageName, map)
    } catch (e: Exception) {
        null
    }

    class LoadedPack(private val res: Resources, private val pkg: String, private val map: Map<String, String>) {
        @SuppressLint("DiscouragedApi")
        fun getIcon(cn: ComponentName): Drawable? {
            val name = map["ComponentInfo{${cn.packageName}/${cn.className}}"] ?: return null
            val id = res.getIdentifier(name, "drawable", pkg)
            if (id == 0) return null
            return try { ResourcesCompat.getDrawable(res, id, null) } catch (e: Exception) { null }
        }
    }
}

// ---------------- Ob-havo (Open-Meteo, kalitsiz) ----------------

object Weather {
    data class City(val name: String, val lat: Double, val lon: Double)
    data class Result(val temp: Double, val code: Int)

    val cities = listOf(
        City("Toshkent", 41.2995, 69.2401),
        City("Samarqand", 39.6542, 66.9597),
        City("Buxoro", 39.7747, 64.4286),
        City("Andijon", 40.7821, 72.3442),
        City("Namangan", 40.9983, 71.6726),
        City("Farg'ona", 40.3864, 71.7864),
        City("Qo'qon", 40.5286, 70.9425),
        City("Qarshi", 38.8606, 65.7891),
        City("Nukus", 42.4600, 59.6100),
        City("Urganch", 41.5500, 60.6333),
        City("Xiva", 41.3783, 60.3639),
        City("Jizzax", 40.1158, 67.8422),
        City("Navoiy", 40.0844, 65.3792),
        City("Termiz", 37.2242, 67.2783),
        City("Guliston", 40.4897, 68.7842),
        City("Nurafshon", 41.0444, 69.3500),
        City("Moskva", 55.7558, 37.6173),
        City("Istanbul", 41.0082, 28.9784),
        City("Dubay", 25.2048, 55.2708),
        City("Seul", 37.5665, 126.9780),
    )

    suspend fun fetch(city: City): Result? = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=${city.lat}&longitude=${city.lon}" +
                    "&current=temperature_2m,weather_code&timezone=auto"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val cur = JSONObject(text).getJSONObject("current")
            Result(cur.getDouble("temperature_2m"), cur.getInt("weather_code"))
        } catch (e: Exception) {
            null
        }
    }

    fun emoji(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..67 -> "🌧️"
        in 71..77 -> "❄️"
        in 80..82 -> "🌦️"
        in 85..86 -> "🌨️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }
}

// ---------------- Qidiruv: kontaktlar va sozlamalar ----------------

data class ContactResult(val id: Long, val name: String, val lookupKey: String)

data class SettingEntry(val uz: String, val ru: String, val en: String, val action: String) {
    fun name(lang: String) = when (lang) { "ru" -> ru; "en" -> en; else -> uz }
    fun matches(q: String) = listOf(uz, ru, en).any { it.contains(q, ignoreCase = true) }
}

object SearchProvider {
    val settings = listOf(
        SettingEntry("Wi-Fi", "Wi-Fi", "Wi-Fi", Settings.ACTION_WIFI_SETTINGS),
        SettingEntry("Bluetooth", "Bluetooth", "Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS),
        SettingEntry("Ekran", "Экран", "Display", Settings.ACTION_DISPLAY_SETTINGS),
        SettingEntry("Ovoz", "Звук", "Sound", Settings.ACTION_SOUND_SETTINGS),
        SettingEntry("Batareya", "Батарея", "Battery", Intent.ACTION_POWER_USAGE_SUMMARY),
        SettingEntry("Ilovalar", "Приложения", "Apps", Settings.ACTION_APPLICATION_SETTINGS),
        SettingEntry("Joylashuv", "Местоположение", "Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS),
        SettingEntry("Sana va vaqt", "Дата и время", "Date & time", Settings.ACTION_DATE_SETTINGS),
        SettingEntry("Til", "Язык", "Language", Settings.ACTION_LOCALE_SETTINGS),
        SettingEntry("Xotira", "Хранилище", "Storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        SettingEntry("Mobil tarmoq", "Мобильная сеть", "Mobile network", Settings.ACTION_DATA_ROAMING_SETTINGS),
        SettingEntry("Xavfsizlik", "Безопасность", "Security", Settings.ACTION_SECURITY_SETTINGS),
        SettingEntry("Maxsus imkoniyatlar", "Спец. возможности", "Accessibility", Settings.ACTION_ACCESSIBILITY_SETTINGS),
        SettingEntry("Sozlamalar", "Настройки", "Settings", Settings.ACTION_SETTINGS),
    )

    fun searchSettings(q: String): List<SettingEntry> = settings.filter { it.matches(q) }.take(4)

    fun hasContactsPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun searchContacts(context: Context, q: String): List<ContactResult> = withContext(Dispatchers.IO) {
        if (q.isBlank() || !hasContactsPermission(context)) return@withContext emptyList()
        val result = mutableListOf<ContactResult>()
        try {
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(q))
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.LOOKUP_KEY,
                ),
                null, null, null
            )?.use { c ->
                while (c.moveToNext() && result.size < 5) {
                    result.add(ContactResult(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: ""))
                }
            }
        } catch (_: Exception) {
        }
        result
    }

    fun openContact(context: Context, c: ContactResult) {
        safeStart(context, Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.getLookupUri(c.id, c.lookupKey)))
    }

    fun webSearch(context: Context, q: String) {
        val i = Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, q)
        if (!safeStart(context, i)) {
            safeStart(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))))
        }
    }

    fun playSearch(context: Context, q: String) {
        if (!safeStart(context, Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(q))))) {
            safeStart(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=" + Uri.encode(q))))
        }
    }
}
