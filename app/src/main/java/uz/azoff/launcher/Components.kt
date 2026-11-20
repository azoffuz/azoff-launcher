@file:OptIn(ExperimentalFoundationApi::class)

package uz.azoff.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import android.os.SystemClock
import kotlin.math.hypot
import kotlin.math.roundToInt

fun cellHeight(iconSize: Int, showLabels: Boolean): Dp = (iconSize + if (showLabels) 34 else 14).dp

@Composable
fun AppIconView(
    app: AppInfo,
    iconSize: Dp,
    showLabel: Boolean,
    labelStyle: TextStyle,
    badge: Int,
    locked: Boolean,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick) else Modifier)
            .padding(vertical = 5.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(iconSize))
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = (-5).dp)
                        .size(19.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (badge > 9) "9+" else badge.toString(), color = Color.White, fontSize = 10.sp)
                }
            }
            if (locked) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(11.dp))
                }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(
                app.label,
                style = labelStyle,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun FolderIconView(
    name: String,
    apps: List<AppInfo>,
    iconSize: Dp,
    showLabel: Boolean,
    labelStyle: TextStyle,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick) else Modifier)
            .padding(vertical = 5.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(iconSize * 0.28f))
                .background(Color.White.copy(alpha = 0.28f))
                .padding(iconSize * 0.1f),
        ) {
            val mini = (iconSize * 0.8f) / 2
            Column {
                for (r in 0 until 2) {
                    Row {
                        for (c in 0 until 2) {
                            val a = apps.getOrNull(r * 2 + c)
                            Box(Modifier.size(mini).padding(2.dp)) {
                                if (a != null) Image(bitmap = a.icon, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(name, style = labelStyle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

enum class DropSide { LEFT, RIGHT, TOP, BOTTOM }

/** Ikonka ustida uzoq bosilganini bosh ekranning "uzoq bosish" menyusidan ajratish uchun */
object DragGuard {
    @Volatile
    var lastIconLongPress = 0L
}

/**
 * Ikonlar to'ri. Uzoq bosib (yoki tahrirlash rejimida darhol) ikonni istalgan joyga surish mumkin:
 *  - boshqa katakka — joy almashadi
 *  - boshqa ikonning ustiga — papka yaratiladi (onMerge)
 *  - ekran chetiga yoki pastga/tepaga — onOutside (boshqa sahifa yoki dock)
 *  - surmasdan qo'yib yuborilsa — onLongPress (menyu)
 */
@Composable
fun <T> ReorderGrid(
    items: List<T>,
    columns: Int,
    cellHeight: Dp,
    editMode: Boolean,
    onMove: (Int, Int) -> Unit,
    onRemove: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    dragEnabled: Boolean = true,
    minRows: Int = 1,
    onTap: ((Int) -> Unit)? = null,
    onLongPress: ((Int) -> Unit)? = null,
    onMerge: ((Int, Int) -> Unit)? = null,
    onOutside: ((Int, DropSide) -> Unit)? = null,
    content: @Composable (Int, T) -> Unit,
) {
    val tapCb by rememberUpdatedState(onTap)
    val longCb by rememberUpdatedState(onLongPress)
    val moveCb by rememberUpdatedState(onMove)
    val mergeCb by rememberUpdatedState(onMerge)
    val outsideCb by rememberUpdatedState(onOutside)
    val haptic = LocalHapticFeedback.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cols = columns.coerceAtLeast(1)
        val cellW = maxWidth / cols
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val cellWpx = with(density) { cellW.toPx() }
        val cellHpx = with(density) { cellHeight.toPx() }
        val stillPx = with(density) { 14.dp.toPx() }
        var dragIndex by remember { mutableIntStateOf(-1) }
        var dx by remember { mutableFloatStateOf(0f) }
        var dy by remember { mutableFloatStateOf(0f) }
        var hover by remember { mutableIntStateOf(-1) }
        val rows = ((items.size + cols - 1) / cols).coerceAtLeast(minRows).coerceAtLeast(1)
        val gridH = cellHpx * rows

        fun targetOf(index: Int): Int {
            val col = index % cols
            val row = index / cols
            val cx = col * cellWpx + dx + cellWpx / 2
            val cy = row * cellHpx + dy + cellHpx / 2
            val tc = (cx / cellWpx).toInt().coerceIn(0, cols - 1)
            val tr = (cy / cellHpx).toInt().coerceAtLeast(0)
            return tr * cols + tc
        }

        fun finish(index: Int) {
            if (index < 0 || index >= items.size) { dragIndex = -1; hover = -1; dx = 0f; dy = 0f; return }
            val col = index % cols
            val row = index / cols
            val dist = hypot(dx, dy)
            val cx = col * cellWpx + dx + cellWpx / 2
            val cy = row * cellHpx + dy + cellHpx / 2
            val out = outsideCb
            when {
                dist < stillPx -> if (!editMode) longCb?.invoke(index)
                out != null && cx < cellWpx * 0.3f -> out(index, DropSide.LEFT)
                out != null && cx > widthPx - cellWpx * 0.3f -> out(index, DropSide.RIGHT)
                out != null && cy < -cellHpx * 0.35f -> out(index, DropSide.TOP)
                out != null && cy > gridH + cellHpx * 0.15f -> out(index, DropSide.BOTTOM)
                else -> {
                    val target = targetOf(index)
                    if (target >= items.size) {
                        if (index != items.size - 1) moveCb(index, items.size - 1)
                    } else if (target != index) {
                        val tcx = (target % cols) * cellWpx + cellWpx / 2
                        val tcy = (target / cols) * cellHpx + cellHpx / 2
                        val merge = mergeCb
                        if (merge != null && hypot(cx - tcx, cy - tcy) < minOf(cellWpx, cellHpx) * 0.28f) merge(index, target)
                        else moveCb(index, target)
                    }
                }
            }
            dragIndex = -1; hover = -1; dx = 0f; dy = 0f
        }

        Box(Modifier.fillMaxWidth().height(cellHeight * rows)) {
            items.forEachIndexed { index, item ->
                val col = index % cols
                val row = index / cols
                val dragging = index == dragIndex
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (col * cellWpx + if (dragging) dx else 0f).roundToInt(),
                                (row * cellHpx + if (dragging) dy else 0f).roundToInt(),
                            )
                        }
                        .width(cellW)
                        .height(cellHeight)
                        .zIndex(if (dragging) 2f else 0f)
                        .graphicsLayer {
                            val sc = if (dragging) 1.15f else if (index == hover) 0.88f else 1f
                            scaleX = sc; scaleY = sc
                            alpha = if (dragging) 0.9f else 1f
                        }
                        .then(
                            if (!dragEnabled && !editMode) Modifier
                            else Modifier.pointerInput(index, items.size, cols, editMode, cellWpx) {
                                val slop = viewConfiguration.touchSlop
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (!editMode) {
                                        // 0 = bosish, 1 = boshqa harakat (sahifa surish), null = uzoq bosish
                                        val r = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                            var res = 1
                                            while (true) {
                                                val ev = awaitPointerEvent()
                                                val ch = ev.changes.firstOrNull { it.id == down.id }
                                                if (ch == null) { res = 1; break }
                                                if (!ch.pressed) { ch.consume(); res = 0; break }
                                                if (ch.isConsumed || (ch.position - down.position).getDistance() > slop) { res = 1; break }
                                            }
                                            res
                                        }
                                        if (r == 0) { tapCb?.invoke(index); return@awaitEachGesture }
                                        if (r == 1) return@awaitEachGesture
                                        DragGuard.lastIconLongPress = SystemClock.uptimeMillis()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else {
                                        // tahrirlash rejimida: ozgina surilgach darhol ko'chirish boshlanadi
                                        var started = false
                                        while (!started) {
                                            val ev = awaitPointerEvent()
                                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                                            if (!ch.pressed) return@awaitEachGesture
                                            if ((ch.position - down.position).getDistance() > slop) started = true
                                        }
                                    }
                                    dragIndex = index; dx = 0f; dy = 0f
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!ch.pressed) { ch.consume(); break }
                                        val d = ch.positionChange()
                                        dx += d.x; dy += d.y
                                        ch.consume()
                                        val t = targetOf(index)
                                        hover = if (t != index && t < items.size) t else -1
                                    }
                                    finish(index)
                                }
                            }
                        ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    content(index, item)
                    if (editMode && onRemove != null) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 6.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                                .clickable { onRemove(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Pastdan chiqadigan oyna ko'rinishidagi dialog */
@Composable
fun SheetDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.BottomCenter) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp),
            ) {
                Column(Modifier.padding(vertical = 12.dp)) { content() }
            }
        }
    }
}

@Composable
fun MenuRow(text: String, icon: (@Composable () -> Unit)? = null, color: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color,
        )
    }
}

@Composable
fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}
