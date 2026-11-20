@file:OptIn(ExperimentalMaterial3Api::class)

package uz.azoff.launcher

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

private val gestureActions = listOf("none", "drawer", "search", "notifications", "quick_settings", "lock", "recents", "turbo", "settings", "wallpaper", "app")

@Composable
fun SettingsScreen(c: Controller, st: LauncherSettings) {
    val s = LocalStrings.current
    val ctx = LocalContext.current
    val apps by c.vm.apps.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<String?>(null) }
    val update: ((LauncherSettings) -> LauncherSettings) -> Unit = { c.vm.update(it) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) c.toast(if (c.vm.exportTo(uri)) s.backupDone else s.error)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) c.toast(if (c.vm.importFrom(uri)) s.restoreDone else s.error)
    }

    // Ruxsatlar holatini har safar qaytib kelganda yangilash
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val isDefault = remember(tick) { SystemActions.isDefaultLauncher(ctx) }
    val notifOk = remember(tick) { NotificationBadgeService.isEnabled(ctx) }
    val a11yOk = remember(tick) { SystemActions.accessibilityEnabled(ctx) }
    val packs = remember { c.vm.repo.iconPacks.installedPacks() }
    val version = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { c.settingsOpen = false }) { Icon(Icons.Filled.ArrowBack, s.back) }
                Text(s.settingsTitle, style = MaterialTheme.typography.titleLarge)
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
                if (!isDefault) {
                    item {
                        Card(
                            Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(s.notDefault, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.size(8.dp))
                                Button(onClick = { SystemActions.openHomeSettings(ctx) }) { Text(s.makeDefault) }
                            }
                        }
                    }
                }

                // ---------- Turbo ----------
                item { Section("⚡ " + s.turboSection) }
                item {
                    SwitchRow(s.turboMode, s.turboDesc, st.turbo) { v -> update { it.copy(turbo = v, turboByAuto = false) } }
                }
                item { SwitchRow(s.autoTurbo, s.autoTurboDesc(st.autoTurboLevel), st.autoTurbo) { v -> update { it.copy(autoTurbo = v) } } }
                if (st.autoTurbo) {
                    item { SliderRow(s.autoTurboLevel, st.autoTurboLevel, 5, 50, suffix = "%") { v -> update { it.copy(autoTurboLevel = v) } } }
                }
                item { SwitchRow(s.followPowerSave, s.followPowerSaveDesc, st.followPowerSave) { v -> update { it.copy(followPowerSave = v) } } }

                // ---------- Ko'rinish ----------
                item { Section(s.appearance) }
                item { ClickRow(s.language, languageName(st.language)) { dialog = "lang" } }
                item { ClickRow(s.theme, s.themeNames[st.theme.coerceIn(0, 2)]) { dialog = "theme" } }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    item { SwitchRow(s.materialYou, s.materialYouDesc, st.dynamicColor) { v -> update { it.copy(dynamicColor = v) } } }
                }
                if (!st.dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    item { AccentRow(s.accentColor, st.accent) { i -> update { it.copy(accent = i) } } }
                }
                item { SliderRow(s.iconSize, st.iconSize, 40, 72) { v -> update { it.copy(iconSize = v) } } }
                item { SwitchRow(s.showLabels, null, st.showLabels) { v -> update { it.copy(showLabels = v) } } }
                item {
                    val packName = packs.firstOrNull { it.packageName == st.iconPack }?.label ?: s.defaultIcons
                    ClickRow(s.iconPack, packName) { dialog = "iconPack" }
                }
                item { SwitchRow(s.animations, s.animationsDesc, st.animations) { v -> update { it.copy(animations = v) } } }

                // ---------- Bosh ekran ----------
                item { Section(s.homeScreen) }
                item { SwitchRow(s.showClock, null, st.showClock) { v -> update { it.copy(showClock = v) } } }
                item { SwitchRow(s.showWeather, s.weatherDesc, st.showWeather) { v -> update { it.copy(showWeather = v) } } }
                if (st.showWeather) {
                    item {
                        ClickRow(s.city, (Weather.cities.getOrNull(st.weatherCity) ?: Weather.cities[0]).name) { dialog = "city" }
                    }
                }
                item { SwitchRow(s.drawerless, s.drawerlessDesc, st.drawerless) { v -> update { it.copy(drawerless = v) } } }
                item { SliderRow(s.homeColumns, st.homeColumns, 3, 6) { v -> update { it.copy(homeColumns = v) } } }
                item { SliderRow(s.homeRows, st.homeRows, 3, 8) { v -> update { it.copy(homeRows = v) } } }
                item { SwitchRow(s.badges, if (notifOk) s.badgesDesc else s.badgesNeedAccess, st.badges) { v -> update { it.copy(badges = v) } } }
                if (st.badges && !notifOk) {
                    item { ClickRow(s.grantAccess, s.restrictedHint) { SystemActions.openNotificationAccess(ctx) } }
                }
                item { ClickRow(s.editHome, null) { c.settingsOpen = false; c.editMode = true } }
                item { ClickRow(s.addWidget, null) { c.widgetPicker = true } }
                item { ClickRow(s.wallpaper, null) { SystemActions.setWallpaper(ctx) } }

                // ---------- Ilovalar menyusi ----------
                item { Section(s.drawer) }
                item { SliderRow(s.drawerColumns, st.drawerColumns, 3, 6) { v -> update { it.copy(drawerColumns = v) } } }
                item {
                    SliderRow(s.transparency, (st.drawerAlpha * 100).roundToInt(), 40, 100, suffix = "%") { v ->
                        update { it.copy(drawerAlpha = v / 100f) }
                    }
                }
                item { SwitchRow(s.frequentOnTop, null, st.frequentOnTop) { v -> update { it.copy(frequentOnTop = v) } } }

                // ---------- Gesture'lar ----------
                item { Section(s.gestures) }
                item { ClickRow(s.swipeUp, s.actionName(st.swipeUp, apps)) { dialog = "swipeUp" } }
                item { ClickRow(s.swipeDown, s.actionName(st.swipeDown, apps)) { dialog = "swipeDown" } }
                item { ClickRow(s.doubleTap, s.actionName(st.doubleTap, apps)) { dialog = "doubleTap" } }
                if (!a11yOk) {
                    item { ClickRow(s.accessibilityService, s.accessibilityDesc) { SystemActions.openAccessibilitySettings(ctx) } }
                }

                // ---------- Xavfsizlik ----------
                item { Section(s.security) }
                item {
                    ClickRow(if (st.pinHash.isEmpty()) s.setPin else s.changePin, null) {
                        if (st.pinHash.isEmpty()) dialog = "pin" else c.requireAuth { dialog = "pin" }
                    }
                }
                if (st.pinHash.isNotEmpty()) {
                    item { ClickRow(s.removePin, null) { c.requireAuth { update { it.copy(pinHash = "") } } } }
                }
                item { ClickRow(s.hiddenApps, s.countApps(st.hidden.size)) { c.requireAuth { c.hiddenOpen = true } } }
                item { ClickRow(s.lockedApps, s.countApps(st.locked.size)) { c.requireAuth { dialog = "locked" } } }

                // ---------- Bolalar rejimi ----------
                item { Section(s.kidsMode) }
                item { ClickRow(s.kidsApps, s.countApps(st.kidsApps.size)) { dialog = "kids" } }
                item {
                    SwitchRow(s.kidsMode, s.kidsDesc, st.kidsMode) { v ->
                        if (v) {
                            when {
                                st.pinHash.isEmpty() -> c.toast(s.setPinFirst)
                                st.kidsApps.isEmpty() -> c.toast(s.chooseKidsApps)
                                else -> {
                                    update { it.copy(kidsMode = true) }
                                    c.settingsOpen = false
                                }
                            }
                        } else {
                            c.requireAuth { update { it.copy(kidsMode = false) } }
                        }
                    }
                }

                // ---------- Zaxira ----------
                item { Section(s.backup) }
                item { ClickRow(s.exportSettings, s.exportDesc) { exportLauncher.launch("azoff-launcher-backup.json") } }
                item {
                    ClickRow(s.importSettings, s.importDesc) {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"))
                    }
                }

                // ---------- Haqida ----------
                item { Section(s.about) }
                item { ClickRow(s.makeDefault, null) { SystemActions.openHomeSettings(ctx) } }
                item { ClickRow("Azoff Launcher", "v$version · Redmi Note 13 / HyperOS") {} }
                item {
                    Text(
                        s.restrictedHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    // ---------- Dialoglar ----------
    val dismiss = { dialog = null }
    when (dialog) {
        "lang" -> ChoiceDialog(
            s.language,
            listOf("uz" to "O'zbekcha", "ru" to "Русский", "en" to "English"),
            st.language,
            dismiss,
        ) { v -> update { it.copy(language = v) }; dismiss() }

        "theme" -> ChoiceDialog(
            s.theme,
            s.themeNames.mapIndexed { i, n -> i.toString() to n },
            st.theme.toString(),
            dismiss,
        ) { v -> update { it.copy(theme = v.toInt()) }; dismiss() }

        "city" -> ChoiceDialog(
            s.city,
            Weather.cities.mapIndexed { i, ct -> i.toString() to ct.name },
            st.weatherCity.toString(),
            dismiss,
        ) { v -> update { it.copy(weatherCity = v.toInt()) }; dismiss() }

        "iconPack" -> ChoiceDialog(
            s.iconPack,
            listOf("" to s.defaultIcons) + packs.map { it.packageName to it.label },
            st.iconPack,
            dismiss,
            footer = if (packs.isEmpty()) s.noIconPacks else null,
        ) { v -> update { it.copy(iconPack = v) }; dismiss() }

        "swipeUp", "swipeDown", "doubleTap" -> {
            val key = dialog ?: ""
            val current = when (key) { "swipeUp" -> st.swipeUp; "swipeDown" -> st.swipeDown; else -> st.doubleTap }
            var pickApp by remember { mutableStateOf(false) }
            val apply: (String) -> Unit = { code ->
                update {
                    when (key) {
                        "swipeUp" -> it.copy(swipeUp = code)
                        "swipeDown" -> it.copy(swipeDown = code)
                        else -> it.copy(doubleTap = code)
                    }
                }
            }
            if (pickApp) {
                AppPickerDialog(
                    title = s.chooseApp,
                    apps = apps,
                    multi = false,
                    initial = emptySet(),
                    onDismiss = { pickApp = false; dismiss() },
                ) { keys ->
                    keys.firstOrNull()?.let { apply("app:$it") }
                    pickApp = false; dismiss()
                }
            } else {
                ChoiceDialog(
                    s.chooseAction,
                    gestureActions.map { it to s.actionName(it, emptyList()) },
                    if (current.startsWith("app:")) "app" else current,
                    dismiss,
                ) { v ->
                    if (v == "app") pickApp = true else { apply(v); dismiss() }
                }
            }
        }

        "pin" -> PinSetupDialog(onDismiss = dismiss) { pin ->
            update { it.copy(pinHash = hashPin(pin)) }
            c.toast(s.pinSaved)
            dismiss()
        }

        "kids" -> AppPickerDialog(
            title = s.kidsApps,
            apps = apps,
            multi = true,
            initial = st.kidsApps,
            onDismiss = dismiss,
        ) { keys -> update { it.copy(kidsApps = keys) }; dismiss() }

        "locked" -> AppPickerDialog(
            title = s.lockedApps,
            apps = apps,
            multi = true,
            initial = st.locked,
            onDismiss = dismiss,
        ) { keys -> update { it.copy(locked = keys) }; dismiss() }
    }
}

fun languageName(code: String) = when (code) {
    "ru" -> "Русский"
    "en" -> "English"
    else -> "O'zbekcha"
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 6.dp),
    )
}

@Composable
private fun ClickRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (!subtitle.isNullOrEmpty()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(title: String, value: Int, min: Int, max: Int, suffix: String = "", onChange: (Int) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("${local.roundToInt()}$suffix", color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local.roundToInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = if (max - min <= 10) (max - min - 1).coerceAtLeast(0) else 0,
        )
    }
}

@Composable
private fun AccentRow(title: String, selected: Int, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.size(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(accentColors) { i, color ->
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (i == selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { onPick(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (i == selected) Icon(Icons.Filled.Check, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onDismiss: () -> Unit,
    footer: String? = null,
    onPick: (String) -> Unit,
) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    itemsIndexed(options) { _, (value, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPick(value) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = value == selected, onClick = { onPick(value) })
                            Text(label)
                        }
                    }
                }
                if (footer != null) {
                    Text(footer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.close) } },
    )
}
