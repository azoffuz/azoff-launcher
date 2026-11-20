package uz.azoff.launcher

import android.app.Application
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("azoff", Context.MODE_PRIVATE)
    val repo = AppRepository(app)

    val settings = MutableStateFlow(loadSettings())
    val apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val loaded = MutableStateFlow(false)
    val weather = MutableStateFlow<Weather.Result?>(null)

    private var loadJob: Job? = null
    private var lastWeather = 0L
    private val callback: LauncherApps.Callback

    init {
        reloadApps()
        callback = repo.registerCallback { reloadApps() }
    }

    override fun onCleared() {
        repo.unregister(callback)
        super.onCleared()
    }

    private fun loadSettings(): LauncherSettings {
        val raw = prefs.getString("settings", null) ?: return LauncherSettings()
        return try { SettingsJson.fromJson(raw) } catch (e: Exception) { LauncherSettings() }
    }

    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        val old = settings.value
        val new = transform(old)
        if (new == old) return
        settings.value = new
        prefs.edit().putString("settings", SettingsJson.toJson(new)).apply()
        if (old.iconPack != new.iconPack) reloadApps()
        if (old.weatherCity != new.weatherCity || (!old.showWeather && new.showWeather)) refreshWeather(force = true)
    }

    fun reloadApps() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val density = getApplication<Application>().resources.displayMetrics.density
            val list = repo.loadApps(settings.value.iconPack, (80 * density).toInt())
            apps.value = list
            loaded.value = true
            val keys = list.map { it.key }.toSet()
            if (settings.value.firstRun && list.isNotEmpty()) {
                val (dock, home) = repo.defaultApps(list)
                update { it.copy(dock = dock, pages = listOf(home.map { k -> HomeItem.App(k) }), firstRun = false) }
            } else {
                update { it.cleanup(keys) }
            }
        }
    }

    fun onResume() {
        refreshWeather(force = false)
    }

    fun refreshWeather(force: Boolean) {
        val s = settings.value
        if (!s.showWeather) return
        val now = System.currentTimeMillis()
        if (!force && now - lastWeather < 30 * 60 * 1000L && weather.value != null) return
        lastWeather = now
        val city = Weather.cities.getOrNull(s.weatherCity) ?: Weather.cities[0]
        viewModelScope.launch {
            Weather.fetch(city)?.let { weather.value = it }
        }
    }

    fun launch(app: AppInfo): Boolean {
        val ok = repo.start(app, settings.value.animations)
        if (ok) update { it.copy(usage = it.usage + (app.key to ((it.usage[app.key] ?: 0) + 1))) }
        return ok
    }

    fun shortcuts(app: AppInfo): List<ShortcutInfo> = repo.shortcuts(app)
    fun shortcutIcon(s: ShortcutInfo): ImageBitmap? {
        val density = getApplication<Application>().resources.displayMetrics.density
        return repo.shortcutIcon(s, (40 * density).toInt())
    }

    fun exportTo(uri: Uri): Boolean = try {
        getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
            it.write(SettingsJson.toJson(settings.value).toByteArray())
        }
        true
    } catch (e: Exception) {
        false
    }

    fun importFrom(uri: Uri): Boolean = try {
        val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: throw IllegalStateException("empty")
        val imported = SettingsJson.fromJson(text).copy(firstRun = false)
        // Vidjet ID'lari boshqa qurilmada yaroqsiz bo'lishi mumkin, joriylarini saqlaymiz
        update { imported.copy(widgetIds = it.widgetIds) }
        reloadApps()
        true
    } catch (e: Exception) {
        false
    }
}
