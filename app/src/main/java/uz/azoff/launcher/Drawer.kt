@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package uz.azoff.launcher

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val CAT_ALL = -100

private class DrawerEntry(val header: String?, val app: AppInfo?)

private fun letterOf(label: String): String {
    val ch = label.trim().firstOrNull() ?: return "#"
    return if (ch.isLetter()) ch.uppercaseChar().toString() else "#"
}

@Composable
fun AppDrawer(c: Controller, st: LauncherSettings) {
    val s = LocalStrings.current
    val ctx = LocalContext.current
    val apps by c.vm.apps.collectAsStateWithLifecycle()
    val badges by NotificationBadgeService.counts.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableIntStateOf(CAT_ALL) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val visible = remember(apps, st.hidden, st.kidsMode, st.kidsApps) {
        apps.filter { if (st.kidsMode) it.key in st.kidsApps else it.key !in st.hidden }
    }

    LaunchedEffect(c.focusSearch) {
        if (c.focusSearch) {
            delay(200)
            try { focus.requestFocus(); keyboard?.show() } catch (_: Exception) {}
            c.focusSearch = false
        }
    }

    val close = rememberUpdatedState { keyboard?.hide(); c.drawerOpen = false; query = "" }
    val nested = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 60f) close.value()
                return Offset.Zero
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (st.turbo) 1f else st.drawerAlpha))
            .systemBarsPadding()
            .imePadding()
    ) {
        // Qidiruv qatori (pastga sudrab yopish ham mumkin)
        Row(
            Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total > 80f) close.value() },
                    ) { _, d -> total += d }
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).focusRequester(focus),
                placeholder = { Text(s.searchHint) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, null) }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    val q = query.trim()
                    val first = visible.firstOrNull { it.label.contains(q, ignoreCase = true) }
                    if (first != null) c.launch(first)
                    else if (!st.kidsMode && q.isNotEmpty()) SearchProvider.webSearch(ctx, q)
                }),
            )
            if (!st.kidsMode) {
                var more by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { more = true }) { Icon(Icons.Filled.MoreVert, null) }
                    DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                        DropdownMenuItem(
                            text = { Text(s.hiddenApps) },
                            onClick = { more = false; c.requireAuth { c.hiddenOpen = true } },
                        )
                        DropdownMenuItem(
                            text = { Text(s.launcherSettings) },
                            onClick = { more = false; c.openSettings() },
                        )
                    }
                }
            }
        }

        if (query.isBlank()) {
            val cats = remember(visible) { visible.map { it.category }.distinct().sorted() }
            if (cats.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(selected = category == CAT_ALL, onClick = { category = CAT_ALL }, label = { Text(s.all) })
                    }
                    items(cats) { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(s.category(cat)) })
                    }
                }
            }
            val list = if (category == CAT_ALL) visible else visible.filter { it.category == category }
            val frequent = if (category == CAT_ALL && st.frequentOnTop && !st.kidsMode) {
                visible.filter { (st.usage[it.key] ?: 0) > 0 }
                    .sortedByDescending { st.usage[it.key] ?: 0 }
                    .take(st.drawerColumns)
            } else emptyList()
            AlphabetGrid(c, st, list, frequent, badges, nested)
        } else {
            SearchResults(c, st, query.trim(), visible, badges)
        }
    }
}

@Composable
private fun AlphabetGrid(
    c: Controller,
    st: LauncherSettings,
    list: List<AppInfo>,
    frequent: List<AppInfo>,
    badges: Map<String, Int>,
    nested: NestedScrollConnection,
) {
    val s = LocalStrings.current
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val entries = remember(list, frequent, s) {
        buildList<DrawerEntry> {
            if (frequent.isNotEmpty()) {
                add(DrawerEntry("★ " + s.frequent, null))
                frequent.forEach { add(DrawerEntry(null, it)) }
            }
            list.groupBy { letterOf(it.label) }.forEach { (letter, group) ->
                add(DrawerEntry(letter, null))
                group.forEach { add(DrawerEntry(null, it)) }
            }
        }
    }
    val letterIndex = remember(entries) {
        val m = LinkedHashMap<String, Int>()
        entries.forEachIndexed { i, e -> val h = e.header; if (h != null && h.length == 1) m[h] = i }
        m
    }
    val textColor = MaterialTheme.colorScheme.onSurface

    Row(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(st.drawerColumns),
            state = gridState,
            modifier = Modifier.weight(1f).fillMaxHeight().nestedScroll(nested),
            contentPadding = PaddingValues(start = 8.dp, end = 4.dp, bottom = 24.dp),
        ) {
            items(
                count = entries.size,
                span = { i -> if (entries[i].header != null) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
            ) { i ->
                val e = entries[i]
                val header = e.header
                val app = e.app
                if (header != null) {
                    Text(
                        header,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
                    )
                } else if (app != null) {
                    AppIconView(
                        app = app,
                        iconSize = st.iconSize.dp,
                        showLabel = true,
                        labelStyle = TextStyle(color = textColor),
                        badge = if (st.badgesOn) badges[app.packageName] ?: 0 else 0,
                        locked = app.key in st.locked,
                        onClick = { c.launch(app) },
                        onLongClick = if (st.kidsMode) null else ({ c.menu = app to MenuSource() }),
                    )
                }
            }
        }
        val letters = letterIndex.keys.toList()
        if (letters.size > 3) {
            FastScroller(letters) { l -> scope.launch { gridState.scrollToItem(letterIndex[l] ?: 0) } }
        }
    }
}

@Composable
private fun FastScroller(letters: List<String>, onLetter: (String) -> Unit) {
    var active by remember { mutableStateOf<String?>(null) }
    BoxWithConstraints(Modifier.fillMaxHeight().width(28.dp).padding(vertical = 8.dp)) {
        val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val pick: (Float) -> Unit = { y ->
            val i = ((y / hPx) * letters.size).toInt().coerceIn(0, letters.size - 1)
            val l = letters[i]
            if (l != active) {
                active = l
                onLetter(l)
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .pointerInput(letters) { detectTapGestures { pick(it.y); active = null } }
                .pointerInput(letters) {
                    detectVerticalDragGestures(
                        onDragStart = { pick(it.y) },
                        onDragEnd = { active = null },
                        onDragCancel = { active = null },
                    ) { change, _ ->
                        change.consume()
                        pick(change.position.y)
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEach { l ->
                Text(
                    l,
                    fontSize = if (l == active) 14.sp else 10.sp,
                    fontWeight = if (l == active) FontWeight.Bold else FontWeight.Normal,
                    color = if (l == active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchResults(c: Controller, st: LauncherSettings, query: String, visible: List<AppInfo>, badges: Map<String, Int>) {
    val s = LocalStrings.current
    val ctx = LocalContext.current
    val matches = remember(query, visible) {
        visible.filter { it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
            .sortedBy { if (it.label.startsWith(query, ignoreCase = true)) 0 else 1 }
            .take(st.drawerColumns * 3)
    }
    var hasPerm by remember { mutableStateOf(SearchProvider.hasContactsPermission(ctx)) }
    var contacts by remember { mutableStateOf(emptyList<ContactResult>()) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPerm = it }
    LaunchedEffect(query, hasPerm) {
        delay(200)
        contacts = if (st.kidsMode) emptyList() else SearchProvider.searchContacts(ctx, query)
    }
    val settingsHits = remember(query) { SearchProvider.searchSettings(query) }
    val textColor = MaterialTheme.colorScheme.onSurface
    val cols = st.drawerColumns

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp)) {
        if (matches.isNotEmpty()) {
            item { SectionLabel(s.apps) }
            matches.chunked(cols).forEach { row ->
                item {
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { app ->
                            AppIconView(
                                app = app,
                                iconSize = st.iconSize.dp,
                                showLabel = true,
                                labelStyle = TextStyle(color = textColor),
                                badge = if (st.badgesOn) badges[app.packageName] ?: 0 else 0,
                                locked = app.key in st.locked,
                                onClick = { c.launch(app) },
                                onLongClick = if (st.kidsMode) null else ({ c.menu = app to MenuSource() }),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        } else {
            item {
                Text(
                    s.nothingFound,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        if (!st.kidsMode) {
            item { SectionLabel(s.contacts) }
            if (!hasPerm) {
                item {
                    TextButton(onClick = { permLauncher.launch(Manifest.permission.READ_CONTACTS) }) { Text(s.allowContacts) }
                }
            } else {
                items(contacts) { ct ->
                    ResultRow(Icons.Filled.Person, ct.name) { SearchProvider.openContact(ctx, ct); c.drawerOpen = false }
                }
            }
            if (settingsHits.isNotEmpty()) {
                item { SectionLabel(s.settingsSection) }
                items(settingsHits) { e ->
                    ResultRow(Icons.Filled.Settings, e.name(st.language)) { safeStart(ctx, Intent(e.action)); c.drawerOpen = false }
                }
            }
            item { SectionLabel(s.web) }
            item { ResultRow(Icons.Filled.Search, s.searchWeb(query)) { SearchProvider.webSearch(ctx, query); c.drawerOpen = false } }
            item { ResultRow(Icons.Filled.ShoppingCart, s.searchPlay(query)) { SearchProvider.playSearch(ctx, query); c.drawerOpen = false } }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun ResultRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface)
    }
}
