package uz.azoff.launcher

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

sealed class HomeItem {
    data class App(val key: String) : HomeItem()
    data class Folder(val id: String, val name: String, val keys: List<String>) : HomeItem()
}

data class LauncherSettings(
    val language: String = "uz",
    val theme: Int = 0, // 0 tizim, 1 yorug', 2 qorong'i
    val dynamicColor: Boolean = true,
    val accent: Int = 0,
    val iconSize: Int = 54,
    val homeColumns: Int = 4,
    val drawerColumns: Int = 4,
    val showLabels: Boolean = true,
    val drawerAlpha: Float = 0.88f,
    val animations: Boolean = true,
    val iconPack: String = "",
    val pages: List<List<HomeItem>> = listOf(emptyList()),
    val dock: List<String> = emptyList(),
    val widgetIds: List<Int> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val locked: Set<String> = emptySet(),
    val usage: Map<String, Int> = emptyMap(),
    val pinHash: String = "",
    val kidsMode: Boolean = false,
    val kidsApps: Set<String> = emptySet(),
    val swipeUp: String = "drawer",
    val swipeDown: String = "notifications",
    val doubleTap: String = "lock",
    val showClock: Boolean = true,
    val showWeather: Boolean = true,
    val weatherCity: Int = 0,
    val badges: Boolean = true,
    val frequentOnTop: Boolean = true,
    val firstRun: Boolean = true,
    // Turbo (batareya tejash) rejimi
    val turbo: Boolean = false,
    val autoTurbo: Boolean = false,
    val autoTurboLevel: Int = 20,
    val followPowerSave: Boolean = true,
    val turboByAuto: Boolean = false,
    // Menyusiz rejim: barcha ilovalar bosh ekranda
    val drawerless: Boolean = false,
    val homeRows: Int = 5,
) {
    /** Turbo rejimni hisobga olgan holdagi haqiqiy qiymatlar */
    val anim: Boolean get() = animations && !turbo
    val weatherOn: Boolean get() = showWeather && !turbo
    val badgesOn: Boolean get() = badges && !turbo
    val widgetsOn: Boolean get() = !turbo
    val capacity: Int get() = (homeColumns * homeRows).coerceAtLeast(4)
}

object SettingsJson {
    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
    private fun Collection<String>.toJsonArray(): JSONArray = JSONArray().also { a -> forEach { a.put(it) } }

    fun toJson(s: LauncherSettings): String {
        val o = JSONObject()
        o.put("language", s.language)
        o.put("theme", s.theme)
        o.put("dynamicColor", s.dynamicColor)
        o.put("accent", s.accent)
        o.put("iconSize", s.iconSize)
        o.put("homeColumns", s.homeColumns)
        o.put("drawerColumns", s.drawerColumns)
        o.put("showLabels", s.showLabels)
        o.put("drawerAlpha", s.drawerAlpha.toDouble())
        o.put("animations", s.animations)
        o.put("iconPack", s.iconPack)
        val pages = JSONArray()
        s.pages.forEach { page ->
            val arr = JSONArray()
            page.forEach { item ->
                val io = JSONObject()
                when (item) {
                    is HomeItem.App -> {
                        io.put("t", "a"); io.put("k", item.key)
                    }
                    is HomeItem.Folder -> {
                        io.put("t", "f"); io.put("id", item.id); io.put("n", item.name)
                        io.put("ks", item.keys.toJsonArray())
                    }
                }
                arr.put(io)
            }
            pages.put(arr)
        }
        o.put("pages", pages)
        o.put("dock", s.dock.toJsonArray())
        o.put("widgetIds", JSONArray().also { a -> s.widgetIds.forEach { a.put(it) } })
        o.put("hidden", s.hidden.toJsonArray())
        o.put("locked", s.locked.toJsonArray())
        o.put("usage", JSONObject().also { u -> s.usage.forEach { (k, v) -> u.put(k, v) } })
        o.put("pinHash", s.pinHash)
        o.put("kidsMode", s.kidsMode)
        o.put("kidsApps", s.kidsApps.toJsonArray())
        o.put("swipeUp", s.swipeUp)
        o.put("swipeDown", s.swipeDown)
        o.put("doubleTap", s.doubleTap)
        o.put("showClock", s.showClock)
        o.put("showWeather", s.showWeather)
        o.put("weatherCity", s.weatherCity)
        o.put("badges", s.badges)
        o.put("frequentOnTop", s.frequentOnTop)
        o.put("firstRun", s.firstRun)
        o.put("turbo", s.turbo)
        o.put("autoTurbo", s.autoTurbo)
        o.put("autoTurboLevel", s.autoTurboLevel)
        o.put("followPowerSave", s.followPowerSave)
        o.put("turboByAuto", s.turboByAuto)
        o.put("drawerless", s.drawerless)
        o.put("homeRows", s.homeRows)
        o.put("format", "azoff-launcher-1")
        return o.toString(2)
    }

    fun fromJson(str: String): LauncherSettings {
        val o = JSONObject(str)
        val d = LauncherSettings()
        val pages = mutableListOf<List<HomeItem>>()
        o.optJSONArray("pages")?.let { pa ->
            for (i in 0 until pa.length()) {
                val arr = pa.optJSONArray(i) ?: continue
                val items = mutableListOf<HomeItem>()
                for (j in 0 until arr.length()) {
                    val io = arr.optJSONObject(j) ?: continue
                    if (io.optString("t") == "f") {
                        items.add(
                            HomeItem.Folder(
                                io.optString("id", UUID.randomUUID().toString()),
                                io.optString("n", "Folder"),
                                io.optJSONArray("ks")?.strings() ?: emptyList()
                            )
                        )
                    } else {
                        val k = io.optString("k")
                        if (k.isNotEmpty()) items.add(HomeItem.App(k))
                    }
                }
                pages.add(items)
            }
        }
        val usage = mutableMapOf<String, Int>()
        o.optJSONObject("usage")?.let { u -> u.keys().forEach { k -> usage[k] = u.optInt(k) } }
        val widgetIds = mutableListOf<Int>()
        o.optJSONArray("widgetIds")?.let { a -> for (i in 0 until a.length()) widgetIds.add(a.optInt(i)) }
        return LauncherSettings(
            language = o.optString("language", d.language),
            theme = o.optInt("theme", d.theme),
            dynamicColor = o.optBoolean("dynamicColor", d.dynamicColor),
            accent = o.optInt("accent", d.accent),
            iconSize = o.optInt("iconSize", d.iconSize),
            homeColumns = o.optInt("homeColumns", d.homeColumns),
            drawerColumns = o.optInt("drawerColumns", d.drawerColumns),
            showLabels = o.optBoolean("showLabels", d.showLabels),
            drawerAlpha = o.optDouble("drawerAlpha", d.drawerAlpha.toDouble()).toFloat(),
            animations = o.optBoolean("animations", d.animations),
            iconPack = o.optString("iconPack", d.iconPack),
            pages = if (pages.isEmpty()) d.pages else pages,
            dock = o.optJSONArray("dock")?.strings() ?: d.dock,
            widgetIds = widgetIds,
            hidden = o.optJSONArray("hidden")?.strings()?.toSet() ?: d.hidden,
            locked = o.optJSONArray("locked")?.strings()?.toSet() ?: d.locked,
            usage = usage,
            pinHash = o.optString("pinHash", d.pinHash),
            kidsMode = o.optBoolean("kidsMode", d.kidsMode),
            kidsApps = o.optJSONArray("kidsApps")?.strings()?.toSet() ?: d.kidsApps,
            swipeUp = o.optString("swipeUp", d.swipeUp),
            swipeDown = o.optString("swipeDown", d.swipeDown),
            doubleTap = o.optString("doubleTap", d.doubleTap),
            showClock = o.optBoolean("showClock", d.showClock),
            showWeather = o.optBoolean("showWeather", d.showWeather),
            weatherCity = o.optInt("weatherCity", d.weatherCity),
            badges = o.optBoolean("badges", d.badges),
            frequentOnTop = o.optBoolean("frequentOnTop", d.frequentOnTop),
            firstRun = o.optBoolean("firstRun", false),
            turbo = o.optBoolean("turbo", d.turbo),
            autoTurbo = o.optBoolean("autoTurbo", d.autoTurbo),
            autoTurboLevel = o.optInt("autoTurboLevel", d.autoTurboLevel),
            followPowerSave = o.optBoolean("followPowerSave", d.followPowerSave),
            turboByAuto = o.optBoolean("turboByAuto", d.turboByAuto),
            drawerless = o.optBoolean("drawerless", d.drawerless),
            homeRows = o.optInt("homeRows", d.homeRows),
        )
    }
}

fun hashPin(pin: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(("azoff:" + pin).toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

// ---------- Bosh ekran bilan ishlash uchun yordamchi funksiyalar ----------

fun LauncherSettings.homeKeys(): Set<String> {
    val set = mutableSetOf<String>()
    pages.forEach { p ->
        p.forEach { item ->
            when (item) {
                is HomeItem.App -> set.add(item.key)
                is HomeItem.Folder -> set.addAll(item.keys)
            }
        }
    }
    return set
}

fun LauncherSettings.folders(): List<HomeItem.Folder> =
    pages.flatten().filterIsInstance<HomeItem.Folder>()

fun LauncherSettings.addToPage(key: String, page: Int): LauncherSettings {
    if (homeKeys().contains(key)) return this
    val p = page.coerceIn(0, pages.size - 1)
    // sahifa to'la bo'lsa, keyingi bo'sh joyga yoki yangi sahifaga qo'yamiz
    val order = listOf(p) + pages.indices.filter { it != p }
    val target = order.firstOrNull { pages[it].size < capacity }
    return if (target != null) {
        copy(pages = pages.mapIndexed { i, items -> if (i == target) items + HomeItem.App(key) else items })
    } else {
        copy(pages = pages + listOf(listOf(HomeItem.App(key))))
    }
}

/** Menyusiz rejim: bosh ekranda yo'q barcha ilovalarni oxiriga qo'shadi */
fun LauncherSettings.syncAllApps(keys: List<String>): LauncherSettings {
    val present = homeKeys() + dock
    var s = this
    keys.filter { it !in present && it !in hidden }.forEach { k ->
        val last = s.pages.size - 1
        s = if (s.pages[last].size < s.capacity) {
            s.copy(pages = s.pages.mapIndexed { i, items -> if (i == last) items + HomeItem.App(k) else items })
        } else {
            s.copy(pages = s.pages + listOf(listOf(HomeItem.App(k))))
        }
    }
    return s
}

/** Element boshqa sahifaga ko'chiriladi (kerak bo'lsa yangi sahifa yaratiladi). false — sahifa to'la */
fun LauncherSettings.moveToPage(page: Int, index: Int, target: Int): LauncherSettings? {
    if (page !in pages.indices || target < 0) return null
    val item = pages[page].getOrNull(index) ?: return null
    val base = if (target >= pages.size) copy(pages = pages + listOf(emptyList())) else this
    if (base.pages[target].size >= capacity) return null
    return base.copy(pages = base.pages.mapIndexed { i, items ->
        when (i) {
            page -> items.filterIndexed { j, _ -> j != index }
            target -> items + item
            else -> items
        }
    })
}

/** Bir elementni ikkinchisining ustiga tashlash: papka yaratish yoki papkaga qo'shish */
fun LauncherSettings.mergeItems(page: Int, from: Int, to: Int, defaultName: String): LauncherSettings {
    val items = pages.getOrNull(page) ?: return this
    val a = items.getOrNull(from) ?: return this
    val b = items.getOrNull(to) ?: return this
    if (a !is HomeItem.App) return moveItem(page, from, to)
    val merged: HomeItem = when (b) {
        is HomeItem.Folder -> b.copy(keys = b.keys + a.key)
        is HomeItem.App -> HomeItem.Folder(UUID.randomUUID().toString(), defaultName, listOf(b.key, a.key))
    }
    val list = items.toMutableList()
    list[to] = merged
    list.removeAt(from)
    return copy(pages = pages.mapIndexed { i, l -> if (i == page) list else l })
}

fun LauncherSettings.pageToDock(page: Int, index: Int): LauncherSettings? {
    val item = pages.getOrNull(page)?.getOrNull(index) as? HomeItem.App ?: return null
    if (dock.size >= 5 || item.key in dock) return null
    return copy(
        pages = pages.mapIndexed { i, l -> if (i == page) l.filterIndexed { j, _ -> j != index } else l },
        dock = dock + item.key,
    )
}

fun LauncherSettings.dockToPage(dockIndex: Int, page: Int): LauncherSettings {
    val key = dock.getOrNull(dockIndex) ?: return this
    return copy(dock = dock - key).addToPage(key, page)
}

fun LauncherSettings.removeFromHome(key: String): LauncherSettings =
    copy(pages = pages.map { items ->
        items.mapNotNull { item ->
            when (item) {
                is HomeItem.App -> if (item.key == key) null else item
                is HomeItem.Folder -> {
                    val ks = item.keys - key
                    if (ks.isEmpty()) null else item.copy(keys = ks)
                }
            }
        }
    })

fun LauncherSettings.moveItem(page: Int, from: Int, to: Int): LauncherSettings =
    copy(pages = pages.mapIndexed { i, items ->
        if (i != page || from !in items.indices || to !in items.indices) items
        else items.toMutableList().apply { add(to, removeAt(from)) }
    })

fun LauncherSettings.moveItemToPage(page: Int, index: Int, target: Int): LauncherSettings {
    if (page !in pages.indices || target !in pages.indices || page == target) return this
    val item = pages[page].getOrNull(index) ?: return this
    return copy(pages = pages.mapIndexed { i, items ->
        when (i) {
            page -> items.filterIndexed { j, _ -> j != index }
            target -> items + item
            else -> items
        }
    })
}

fun LauncherSettings.addPage(): LauncherSettings = copy(pages = pages + listOf(emptyList()))

fun LauncherSettings.removePage(page: Int): LauncherSettings {
    if (pages.size <= 1 || page !in pages.indices) return this
    val moved = pages[page]
    val rest = pages.filterIndexed { i, _ -> i != page }.toMutableList()
    val target = (page - 1).coerceAtLeast(0)
    rest[target] = rest[target] + moved
    return copy(pages = rest)
}

fun LauncherSettings.toggleDock(key: String): LauncherSettings =
    if (dock.contains(key)) copy(dock = dock - key)
    else if (dock.size >= 5) this
    else copy(dock = dock + key)

fun LauncherSettings.moveDock(from: Int, to: Int): LauncherSettings {
    if (from !in dock.indices || to !in dock.indices) return this
    return copy(dock = dock.toMutableList().apply { add(to, removeAt(from)) })
}

/** Ilovani papkaga qo'shadi. folderId null bo'lsa yangi papka yaratiladi. */
fun LauncherSettings.addToFolder(key: String, folderId: String?, newName: String, page: Int): LauncherSettings {
    val cleaned = removeFromHome(key)
    return if (folderId == null) {
        val p = page.coerceIn(0, cleaned.pages.size - 1)
        val folder = HomeItem.Folder(UUID.randomUUID().toString(), newName.ifBlank { "Papka" }, listOf(key))
        cleaned.copy(pages = cleaned.pages.mapIndexed { i, items -> if (i == p) items + folder else items })
    } else {
        cleaned.copy(pages = cleaned.pages.map { items ->
            items.map { if (it is HomeItem.Folder && it.id == folderId) it.copy(keys = it.keys + key) else it }
        })
    }
}

fun LauncherSettings.removeFromFolder(folderId: String, key: String): LauncherSettings =
    copy(pages = pages.map { items ->
        val idx = items.indexOfFirst { it is HomeItem.Folder && it.id == folderId }
        if (idx < 0) items
        else {
            val f = items[idx] as HomeItem.Folder
            val ks = f.keys - key
            val list = items.toMutableList()
            if (ks.isEmpty()) list.removeAt(idx) else list[idx] = f.copy(keys = ks)
            list.add(HomeItem.App(key))
            list
        }
    })

fun LauncherSettings.renameFolder(folderId: String, name: String): LauncherSettings =
    copy(pages = pages.map { items ->
        items.map { if (it is HomeItem.Folder && it.id == folderId) it.copy(name = name) else it }
    })

fun LauncherSettings.deleteFolder(folderId: String): LauncherSettings =
    copy(pages = pages.map { items ->
        val f = items.firstOrNull { it is HomeItem.Folder && it.id == folderId } as HomeItem.Folder?
        if (f == null) items
        else items.filter { it !== f } + f.keys.map { HomeItem.App(it) }
    })

fun LauncherSettings.cleanup(existing: Set<String>): LauncherSettings {
    if (existing.isEmpty()) return this
    return copy(
        pages = pages.map { items ->
            items.mapNotNull { item ->
                when (item) {
                    is HomeItem.App -> if (item.key in existing) item else null
                    is HomeItem.Folder -> {
                        val ks = item.keys.filter { it in existing }
                        if (ks.isEmpty()) null else item.copy(keys = ks)
                    }
                }
            }
        },
        dock = dock.filter { it in existing },
    )
}
