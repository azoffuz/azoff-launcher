@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package uz.azoff.launcher

import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.roundToInt

class MenuSource(val page: Int = -1, val folderId: String? = null)

/** Butun interfeys holati va amallar */
@Stable
class Controller(val activity: MainActivity, val vm: LauncherViewModel) {
    var s: Strings = Strings("uz")

    var drawerOpen by mutableStateOf(false)
    var focusSearch by mutableStateOf(false)
    var settingsOpen by mutableStateOf(false)
    var hiddenOpen by mutableStateOf(false)
    var editMode by mutableStateOf(false)
    var homeMenu by mutableStateOf(false)
    var widgetPicker by mutableStateOf(false)
    var menu by mutableStateOf<Pair<AppInfo, MenuSource>?>(null)
    var openFolderId by mutableStateOf<String?>(null)
    var folderChooser by mutableStateOf<Pair<AppInfo, Int>?>(null)
    var addAppPage by mutableIntStateOf(-1)
    var authAction by mutableStateOf<(() -> Unit)?>(null)
    var currentPage by mutableIntStateOf(0)

    val settings: LauncherSettings get() = vm.settings.value

    fun toast(msg: String) = Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()

    fun closeAll() {
        drawerOpen = false; settingsOpen = false; hiddenOpen = false; editMode = false
        homeMenu = false; widgetPicker = false; menu = null; openFolderId = null
        folderChooser = null; addAppPage = -1
    }

    fun requireAuth(action: () -> Unit) {
        if (settings.pinHash.isEmpty() && !activity.biometricAvailable()) {
            toast(s.setPinFirst)
            action()
            return
        }
        authAction = action
    }

    fun launch(app: AppInfo) {
        val go = {
            if (!vm.launch(app)) toast(s.cannotOpen)
            drawerOpen = false
            hiddenOpen = false
            openFolderId = null
        }
        if (app.key in settings.locked) requireAuth(go) else go()
    }

    fun openSettings() {
        if (settings.kidsMode) requireAuth { settingsOpen = true } else settingsOpen = true
    }

    fun runAction(code: String) {
        if (settings.kidsMode && code != "drawer") return
        when {
            code == "drawer" -> drawerOpen = true
            code == "search" -> { focusSearch = true; drawerOpen = true }
            code == "notifications" -> SystemActions.expandNotifications(activity)
            code == "quick_settings" -> SystemActions.expandQuickSettings(activity)
            code == "lock" -> if (!SystemActions.lockScreen()) {
                toast(s.needAccessibility); SystemActions.openAccessibilitySettings(activity)
            }
            code == "recents" -> if (!SystemActions.recents()) {
                toast(s.needAccessibility); SystemActions.openAccessibilitySettings(activity)
            }
            code == "settings" -> openSettings()
            code == "wallpaper" -> SystemActions.setWallpaper(activity)
            code.startsWith("app:") -> vm.apps.value.firstOrNull { it.key == code.removePrefix("app:") }?.let { launch(it) }
        }
    }
}

@Composable
fun LauncherRoot(activity: MainActivity) {
    val vm = activity.vm
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = remember(settings.language) { Strings(settings.language) }
    val c = remember { Controller(activity, vm) }
    c.s = s

    LaunchedEffect(Unit) {
        activity.homePressed.collect { if (it > 0) c.closeAll() }
    }

    CompositionLocalProvider(LocalStrings provides s) {
        AzoffTheme(settings) {
            BackHandler(enabled = true) {
                when {
                    c.settingsOpen -> c.settingsOpen = false
                    c.drawerOpen -> c.drawerOpen = false
                    c.editMode -> c.editMode = false
                }
            }
            Box(Modifier.fillMaxSize()) {
                if (settings.kidsMode) KidsHome(c, settings) else HomeScreen(c, settings)
                AnimatedVisibility(
                    visible = c.drawerOpen,
                    enter = if (settings.animations) slideInVertically { it / 4 } + fadeIn() else EnterTransition.None,
                    exit = if (settings.animations) slideOutVertically { it / 4 } + fadeOut() else ExitTransition.None,
                ) {
                    AppDrawer(c, settings)
                }
                if (c.settingsOpen) SettingsScreen(c, settings)
                LauncherDialogs(c, settings)
            }
        }
    }
}

// ======================= BOSH EKRAN =======================

@Composable
fun HomeScreen(c: Controller, st: LauncherSettings) {
    val apps by c.vm.apps.collectAsStateWithLifecycle()
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val badges by NotificationBadgeService.counts.collectAsStateWithLifecycle()
    val pageCount = st.pages.size + 1 // 0-sahifa: vidjetlar
    val pager = rememberPagerState(initialPage = 1) { pageCount }
    val home by c.activity.homePressed.collectAsStateWithLifecycle()
    val stState = rememberUpdatedState(st)

    LaunchedEffect(pager.currentPage) { c.currentPage = (pager.currentPage - 1).coerceAtLeast(0) }
    LaunchedEffect(home) {
        if (home > 0 && pager.currentPage != 1) {
            if (stState.value.animations) pager.animateScrollToPage(1) else pager.scrollToPage(1)
        }
    }
    LaunchedEffect(pageCount) {
        if (pager.currentPage >= pageCount) pager.scrollToPage(pageCount - 1)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.10f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { c.runAction(stState.value.doubleTap) },
                    onLongPress = { c.homeMenu = true },
                )
            }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragCancel = { total = 0f },
                    onDragEnd = {
                        if (!c.editMode) {
                            if (total < -120f) c.runAction(stState.value.swipeUp)
                            else if (total > 120f) c.runAction(stState.value.swipeDown)
                        }
                        total = 0f
                    },
                ) { _, amount -> total += amount }
            }
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            if (c.editMode) EditBar(c, st) else ClockHeader(c, st)
            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                beyondViewportPageCount = 1,
            ) { page ->
                if (page == 0) WidgetsPage(c, st)
                else HomePage(c, st, page - 1, byKey, badges)
            }
            PageDots(pager.currentPage, pageCount)
            Dock(c, st, byKey, badges)
        }
    }
}

@Composable
fun ClockHeader(c: Controller, st: LauncherSettings) {
    if (!st.showClock && !st.showWeather) {
        Spacer(Modifier.height(16.dp)); return
    }
    val s = LocalStrings.current
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L - now % 1000L)
        }
    }
    val weather by c.vm.weather.collectAsStateWithLifecycle()
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val time = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    val date = s.formatDate(cal.get(Calendar.DAY_OF_WEEK), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH))

    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        if (st.showClock) {
            Text(
                time,
                style = wallpaperText,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.clickable { safeStart(ctx, Intent(AlarmClock.ACTION_SHOW_ALARMS)) },
            )
            Text(
                date,
                style = wallpaperText,
                fontSize = 18.sp,
                modifier = Modifier.clickable {
                    safeStart(ctx, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR))
                },
            )
        }
        if (st.showWeather) {
            val city = Weather.cities.getOrNull(st.weatherCity) ?: Weather.cities[0]
            val w = weather
            Text(
                if (w != null) "${Weather.emoji(w.code)}  ${w.temp.roundToInt()}°C · ${city.name}" else "🌡️  — · ${city.name}",
                style = wallpaperText,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp).clickable { c.vm.refreshWeather(force = true) },
            )
        }
    }
}

@Composable
fun EditBar(c: Controller, st: LauncherSettings) {
    val s = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .horizontalScroll(rememberScrollState())
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { c.addAppPage = c.currentPage }) { Text(s.addApp) }
        TextButton(onClick = { c.vm.update { it.addPage() } }) { Text(s.addPage) }
        if (st.pages.size > 1) TextButton(onClick = { c.vm.update { it.removePage(c.currentPage) } }) { Text(s.removePage) }
        TextButton(onClick = { c.widgetPicker = true }) { Text(s.addWidget) }
        Button(onClick = { c.editMode = false }) { Text(s.done) }
    }
}

@Composable
fun HomePage(c: Controller, st: LauncherSettings, pageIndex: Int, byKey: Map<String, AppInfo>, badges: Map<String, Int>) {
    val s = LocalStrings.current
    val items = st.pages.getOrNull(pageIndex) ?: emptyList()
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty() && c.editMode) {
            Text(s.emptyPageHint, style = wallpaperText, modifier = Modifier.padding(16.dp))
        }
        ReorderGrid(
            items = items,
            columns = st.homeColumns,
            cellHeight = cellHeight(st.iconSize, st.showLabels),
            editMode = c.editMode,
            onMove = { f, t -> c.vm.update { it.moveItem(pageIndex, f, t) } },
            onRemove = { i ->
                c.vm.update { x ->
                    x.copy(pages = x.pages.mapIndexed { p, l -> if (p == pageIndex) l.filterIndexed { j, _ -> j != i } else l })
                }
            },
        ) { _, item ->
            when (item) {
                is HomeItem.App -> byKey[item.key]?.let { app ->
                    AppIconView(
                        app = app,
                        iconSize = st.iconSize.dp,
                        showLabel = st.showLabels,
                        labelStyle = wallpaperText,
                        badge = if (st.badges) badges[app.packageName] ?: 0 else 0,
                        locked = app.key in st.locked,
                        onClick = { if (!c.editMode) c.launch(app) },
                        onLongClick = { if (!c.editMode) c.menu = app to MenuSource(page = pageIndex) },
                    )
                }
                is HomeItem.Folder -> FolderIconView(
                    name = item.name,
                    apps = item.keys.mapNotNull { byKey[it] },
                    iconSize = st.iconSize.dp,
                    showLabel = st.showLabels,
                    labelStyle = wallpaperText,
                    onClick = { if (!c.editMode) c.openFolderId = item.id },
                    onLongClick = { if (!c.editMode) c.openFolderId = item.id },
                )
            }
        }
    }
}

@Composable
fun WidgetsPage(c: Controller, st: LauncherSettings) {
    val s = LocalStrings.current
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.widgets, style = wallpaperText, fontSize = 22.sp, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = { c.widgetPicker = true }) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text(s.add)
                }
                Spacer(Modifier.width(6.dp))
                TextButton(onClick = { c.editMode = !c.editMode }) {
                    Text(if (c.editMode) s.done else s.edit, style = wallpaperText)
                }
            }
        }
        if (st.widgetIds.isEmpty()) {
            item { Text(s.noWidgets, style = wallpaperText, modifier = Modifier.padding(8.dp)) }
        }
        items(st.widgetIds, key = { it }) { id -> HostedWidget(c, id) }
    }
}

@Composable
fun HostedWidget(c: Controller, id: Int) {
    val info = remember(id) { c.activity.widgetManager.getAppWidgetInfo(id) } ?: return
    val density = LocalDensity.current
    val h = with(density) { info.minHeight.toDp() }.coerceIn(72.dp, 420.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    ) {
        AndroidView(
            factory = { ctx -> c.activity.widgetHost.createView(ctx, id, info) },
            modifier = Modifier.fillMaxWidth().height(h),
        )
        if (c.editMode) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .clickable { c.activity.removeWidget(id) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun PageDots(current: Int, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        for (i in 0 until count) {
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (i == current) 8.dp else 6.dp)
                    .clip(if (i == 0) RoundedCornerShape(2.dp) else CircleShape)
                    .background(Color.White.copy(alpha = if (i == current) 0.95f else 0.45f))
            )
        }
    }
}

@Composable
fun Dock(c: Controller, st: LauncherSettings, byKey: Map<String, AppInfo>, badges: Map<String, Int>) {
    val dockApps = st.dock.mapNotNull { byKey[it] }
    if (dockApps.isEmpty() && !c.editMode) {
        Spacer(Modifier.height(16.dp)); return
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(vertical = 4.dp)
    ) {
        if (dockApps.isEmpty()) {
            Text(LocalStrings.current.dockHint, style = wallpaperText, modifier = Modifier.padding(16.dp))
        } else {
            ReorderGrid(
                items = dockApps,
                columns = dockApps.size,
                cellHeight = (st.iconSize + 14).dp,
                editMode = c.editMode,
                onMove = { f, t -> c.vm.update { it.moveDock(f, t) } },
                onRemove = { i -> c.vm.update { it.toggleDock(dockApps[i].key) } },
            ) { _, app ->
                AppIconView(
                    app = app,
                    iconSize = st.iconSize.dp,
                    showLabel = false,
                    labelStyle = wallpaperText,
                    badge = if (st.badges) badges[app.packageName] ?: 0 else 0,
                    locked = app.key in st.locked,
                    onClick = { if (!c.editMode) c.launch(app) },
                    onLongClick = { if (!c.editMode) c.menu = app to MenuSource() },
                )
            }
        }
    }
}

// ======================= BOLALAR REJIMI =======================

@Composable
fun KidsHome(c: Controller, st: LauncherSettings) {
    val s = LocalStrings.current
    val apps by c.vm.apps.collectAsStateWithLifecycle()
    val kidsApps = apps.filter { it.key in st.kidsApps }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.10f))
            .systemBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { ClockHeader(c, st) }
            IconButton(onClick = { c.requireAuth { c.vm.update { it.copy(kidsMode = false) } } }) {
                Icon(Icons.Filled.Lock, s.exitKidsMode, tint = Color.White)
            }
        }
        Text(
            "🧸 " + s.kidsMode,
            style = wallpaperText,
            fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(st.homeColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(kidsApps, key = { it.key }) { app ->
                AppIconView(
                    app = app,
                    iconSize = (st.iconSize + 8).dp,
                    showLabel = true,
                    labelStyle = wallpaperText,
                    badge = 0,
                    locked = false,
                    onClick = { c.launch(app) },
                    onLongClick = null,
                )
            }
        }
    }
}

// ======================= DIALOGLAR =======================

@Composable
fun LauncherDialogs(c: Controller, st: LauncherSettings) {
    val apps by c.vm.apps.collectAsStateWithLifecycle()
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val s = LocalStrings.current

    c.menu?.let { (app, src) -> AppMenuDialog(c, st, app, src) }
    c.openFolderId?.let { id -> FolderDialog(c, st, id, byKey) }
    c.folderChooser?.let { (app, page) -> FolderChooserDialog(c, st, app, page) }
    if (c.addAppPage >= 0) {
        val home = st.homeKeys()
        AppPickerDialog(
            title = s.addApp,
            apps = apps.filter { it.key !in home && it.key !in st.hidden },
            multi = false,
            initial = emptySet(),
            onDismiss = { c.addAppPage = -1 },
        ) { keys ->
            val page = c.addAppPage
            keys.firstOrNull()?.let { k -> c.vm.update { it.addToPage(k, page) } }
            c.addAppPage = -1
        }
    }
    if (c.widgetPicker) WidgetPickerDialog(c)
    if (c.homeMenu) HomeMenuDialog(c, st)
    if (c.hiddenOpen) HiddenAppsDialog(c, st, apps)
    if (c.authAction != null) AuthDialog(c, st)
}

@Composable
fun AppMenuDialog(c: Controller, st: LauncherSettings, app: AppInfo, src: MenuSource) {
    val s = LocalStrings.current
    val dismiss = { c.menu = null }
    val shortcuts = remember(app.key) { c.vm.shortcuts(app) }
    val red = Color(0xFFE53935)
    SheetDialog(onDismiss = dismiss) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(app.icon, null, Modifier.size(44.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(app.label, style = MaterialTheme.typography.titleMedium)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
            shortcuts.forEach { sc ->
                val ic = remember(sc.id) { c.vm.shortcutIcon(sc) }
                MenuRow(
                    text = (sc.shortLabel ?: sc.longLabel ?: "").toString(),
                    icon = { if (ic != null) Image(ic, null, Modifier.size(24.dp)) else Icon(Icons.Filled.KeyboardArrowRight, null) },
                ) {
                    if (!c.vm.repo.startShortcut(sc)) c.toast(s.cannotOpen)
                    dismiss(); c.drawerOpen = false
                }
            }
            if (shortcuts.isNotEmpty()) HorizontalDivider(Modifier.padding(vertical = 4.dp))

            val onHome = app.key in st.homeKeys()
            val fid = src.folderId
            if (fid != null) {
                MenuRow(s.removeFromFolder, icon = { Icon(Icons.Filled.Close, null) }) {
                    c.vm.update { it.removeFromFolder(fid, app.key) }; dismiss()
                }
            }
            if (!onHome) {
                MenuRow(s.addToHome, icon = { Icon(Icons.Filled.Home, null) }) {
                    c.vm.update { it.addToPage(app.key, c.currentPage) }; c.toast(s.added); dismiss()
                }
            } else if (src.page >= 0) {
                MenuRow(s.removeFromHome, icon = { Icon(Icons.Filled.Close, null) }) {
                    c.vm.update { it.removeFromHome(app.key) }; dismiss()
                }
                if (st.pages.size > 1) {
                    MenuRow(s.moveToNextPage, icon = { Icon(Icons.Filled.KeyboardArrowRight, null) }) {
                        c.vm.update { x ->
                            val idx = x.pages.getOrNull(src.page)?.indexOfFirst { it is HomeItem.App && it.key == app.key } ?: -1
                            if (idx < 0) x else x.moveItemToPage(src.page, idx, (src.page + 1) % x.pages.size)
                        }
                        dismiss()
                    }
                }
            }
            val inDock = app.key in st.dock
            MenuRow(if (inDock) s.removeFromDock else s.addToDock, icon = { Icon(Icons.Filled.Star, null) }) {
                if (!inDock && st.dock.size >= 5) c.toast(s.dockFull) else c.vm.update { it.toggleDock(app.key) }
                dismiss()
            }
            MenuRow(s.addToFolder, icon = { Icon(Icons.Filled.List, null) }) {
                c.folderChooser = app to (if (src.page >= 0) src.page else c.currentPage)
                dismiss()
            }
            val hidden = app.key in st.hidden
            MenuRow(if (hidden) s.unhide else s.hide, icon = { Icon(Icons.Filled.Face, null) }) {
                if (hidden) c.vm.update { it.copy(hidden = it.hidden - app.key) }
                else c.vm.update { it.removeFromHome(app.key).copy(hidden = it.hidden + app.key, dock = it.dock - app.key) }
                dismiss()
            }
            val locked = app.key in st.locked
            MenuRow(if (locked) s.unlockApp else s.lockApp, icon = { Icon(Icons.Filled.Lock, null) }) {
                dismiss()
                if (locked) c.requireAuth { c.vm.update { it.copy(locked = it.locked - app.key) } }
                else {
                    if (st.pinHash.isEmpty() && !c.activity.biometricAvailable()) c.toast(s.setPinFirst)
                    c.vm.update { it.copy(locked = it.locked + app.key) }
                }
            }
            MenuRow(s.appInfo, icon = { Icon(Icons.Filled.Info, null) }) { c.vm.repo.openAppInfo(app); dismiss() }
            MenuRow(s.uninstall, icon = { Icon(Icons.Filled.Delete, null, tint = red) }, color = red) {
                c.vm.repo.uninstall(app); dismiss()
            }
        }
    }
}

@Composable
fun FolderDialog(c: Controller, st: LauncherSettings, id: String, byKey: Map<String, AppInfo>) {
    val s = LocalStrings.current
    val folder = st.folders().firstOrNull { it.id == id }
    if (folder == null) {
        LaunchedEffect(id) { c.openFolderId = null }
        return
    }
    val badges by NotificationBadgeService.counts.collectAsStateWithLifecycle()
    var name by remember(id) { mutableStateOf(folder.name) }
    SheetDialog(onDismiss = { c.openFolderId = null }) {
        OutlinedTextField(
            value = name,
            onValueChange = { v -> name = v; c.vm.update { it.renameFolder(id, v) } },
            singleLine = true,
            label = { Text(s.folderName) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(folder.keys.mapNotNull { byKey[it] }, key = { it.key }) { app ->
                AppIconView(
                    app = app,
                    iconSize = 52.dp,
                    showLabel = true,
                    labelStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    badge = if (st.badges) badges[app.packageName] ?: 0 else 0,
                    locked = app.key in st.locked,
                    onClick = { c.launch(app) },
                    onLongClick = { c.menu = app to MenuSource(folderId = id) },
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { c.vm.update { it.deleteFolder(id) }; c.openFolderId = null }) { Text(s.ungroupFolder) }
            TextButton(onClick = { c.openFolderId = null }) { Text(s.close) }
        }
    }
}

@Composable
fun FolderChooserDialog(c: Controller, st: LauncherSettings, app: AppInfo, page: Int) {
    val s = LocalStrings.current
    val close = { c.folderChooser = null }
    var newName by remember { mutableStateOf("") }
    SheetDialog(onDismiss = close) {
        SheetTitle(s.chooseFolder)
        st.folders().forEach { f ->
            MenuRow(f.name, icon = { Icon(Icons.Filled.List, null) }) {
                c.vm.update { it.addToFolder(app.key, f.id, "", page) }; close()
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                label = { Text(s.newFolder) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                c.vm.update { it.addToFolder(app.key, null, newName.ifBlank { s.folder }, page) }; close()
            }) { Text(s.create) }
        }
    }
}

@Composable
fun AppPickerDialog(
    title: String,
    apps: List<AppInfo>,
    multi: Boolean,
    initial: Set<String>,
    onDismiss: () -> Unit,
    onDone: (Set<String>) -> Unit,
) {
    val s = LocalStrings.current
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initial) }
    val shown = apps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
    SheetDialog(onDismiss = onDismiss) {
        SheetTitle(title)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(s.searchHint) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
            items(shown, key = { it.key }) { app ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (multi) selected = if (app.key in selected) selected - app.key else selected + app.key
                            else onDone(setOf(app.key))
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(app.icon, null, Modifier.size(36.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(app.label, modifier = Modifier.weight(1f))
                    if (multi) Checkbox(checked = app.key in selected, onCheckedChange = null)
                }
            }
        }
        if (multi) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(s.cancel) }
                Button(onClick = { onDone(selected) }) { Text(s.save + " (${selected.size})") }
            }
        }
    }
}

class WidgetEntry(val info: android.appwidget.AppWidgetProviderInfo, val label: String, val app: String)

@Composable
fun WidgetPickerDialog(c: Controller) {
    val s = LocalStrings.current
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val list = remember {
        c.activity.widgetManager.installedProviders.mapNotNull { p ->
            try {
                val app = pm.getApplicationLabel(pm.getApplicationInfo(p.provider.packageName, 0)).toString()
                WidgetEntry(p, p.loadLabel(pm) ?: app, app)
            } catch (e: Exception) {
                null
            }
        }.sortedWith(compareBy({ it.app.lowercase() }, { it.label.lowercase() }))
    }
    val dpi = ctx.resources.displayMetrics.densityDpi
    SheetDialog(onDismiss = { c.widgetPicker = false }) {
        SheetTitle(s.chooseWidget)
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            items(list) { w ->
                val icon = remember(w.info.provider) {
                    try { w.info.loadIcon(ctx, dpi)?.toBitmap(96, 96)?.asImageBitmap() } catch (e: Exception) { null }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { c.widgetPicker = false; c.activity.addWidget(w.info) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (icon != null) Image(icon, null, Modifier.size(40.dp)) else Spacer(Modifier.size(40.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(w.label)
                        Text(w.app, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeMenuDialog(c: Controller, st: LauncherSettings) {
    val s = LocalStrings.current
    val close = { c.homeMenu = false }
    SheetDialog(onDismiss = close) {
        if (st.kidsMode) {
            MenuRow(s.launcherSettings, icon = { Icon(Icons.Filled.Settings, null) }) { close(); c.openSettings() }
            return@SheetDialog
        }
        MenuRow(s.editHome, icon = { Icon(Icons.Filled.Edit, null) }) { close(); c.editMode = true }
        MenuRow(s.addWidget, icon = { Icon(Icons.Filled.Add, null) }) { close(); c.widgetPicker = true }
        MenuRow(s.wallpaper, icon = { Icon(Icons.Filled.Face, null) }) { close(); SystemActions.setWallpaper(c.activity) }
        MenuRow(s.hiddenApps, icon = { Icon(Icons.Filled.Lock, null) }) { close(); c.requireAuth { c.hiddenOpen = true } }
        MenuRow(s.launcherSettings, icon = { Icon(Icons.Filled.Settings, null) }) { close(); c.openSettings() }
    }
}

@Composable
fun HiddenAppsDialog(c: Controller, st: LauncherSettings, apps: List<AppInfo>) {
    val s = LocalStrings.current
    val hidden = apps.filter { it.key in st.hidden }
    SheetDialog(onDismiss = { c.hiddenOpen = false }) {
        SheetTitle(s.hiddenApps)
        if (hidden.isEmpty()) {
            Text(s.noHiddenApps, modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(hidden, key = { it.key }) { app ->
                AppIconView(
                    app = app,
                    iconSize = 52.dp,
                    showLabel = true,
                    labelStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    badge = 0,
                    locked = app.key in st.locked,
                    onClick = { c.launch(app) },
                    onLongClick = { c.menu = app to MenuSource() },
                )
            }
        }
    }
}

@Composable
fun AuthDialog(c: Controller, st: LauncherSettings) {
    val s = LocalStrings.current
    val action = c.authAction ?: return
    val done: () -> Unit = { c.authAction = null; action() }
    val cancel: () -> Unit = { c.authAction = null }
    if (st.pinHash.isEmpty()) {
        LaunchedEffect(action) { c.activity.biometric(s.authTitle, done, cancel) }
    } else {
        PinDialog(
            title = s.enterPin,
            onDismiss = cancel,
            biometric = if (c.activity.biometricAvailable()) ({ c.activity.biometric(s.authTitle, done) {} }) else null,
        ) { pin ->
            if (hashPin(pin) == st.pinHash) done() else c.toast(s.wrongPin)
        }
    }
}

@Composable
fun PinDialog(title: String, onDismiss: () -> Unit, biometric: (() -> Unit)?, onPin: (String) -> Unit) {
    val s = LocalStrings.current
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { v -> if (v.length <= 12 && v.all { it.isDigit() }) pin = v },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    label = { Text(s.pin) },
                )
                if (biometric != null) {
                    TextButton(onClick = biometric) {
                        Icon(Icons.Filled.Check, null); Spacer(Modifier.width(6.dp)); Text(s.useBiometric)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onPin(pin) }, enabled = pin.length >= 4) { Text(s.ok) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

@Composable
fun PinSetupDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val s = LocalStrings.current
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    val ok = a.length >= 4 && a == b
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.setPin) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.pinRules, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = a,
                    onValueChange = { v -> if (v.length <= 12 && v.all { it.isDigit() }) a = v },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    label = { Text(s.newPin) },
                )
                OutlinedTextField(
                    value = b,
                    onValueChange = { v -> if (v.length <= 12 && v.all { it.isDigit() }) b = v },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    label = { Text(s.repeatPin) },
                    isError = b.isNotEmpty() && a != b,
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(a) }, enabled = ok) { Text(s.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
