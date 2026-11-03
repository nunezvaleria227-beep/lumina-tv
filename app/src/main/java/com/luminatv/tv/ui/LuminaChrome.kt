package com.luminatv.tv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Colores comunes del diseño nuevo. */
val LuminaPink = Color(0xFFFF9ED0)
val LuminaMuted = Color(0xFF8E7A8B)
val LuminaNight = Color(0xFF0A0310)
val PinkGradient = Brush.linearGradient(listOf(Color(0xFFFF4FA3), Color(0xFFC2167F)))

/** Pestañas de la barra de menú flotante. */
enum class DockTab { SEARCH, HOME, LIVE, MOVIES, SERIES, GUIDE, MORE, SETTINGS }

/** Espacio que hay que dejar libre abajo para que la barra no tape nada. */
val DockSpace = 64.dp

/**
 * Encabezado de las secciones: logo y reloj arriba, y debajo el nombre de la sección con su ícono
 * y, a la derecha, sus botones (buscar, ordenar…).
 */
@Composable
fun LuminaHeader(
    title: String,
    icon: (@Composable (Color) -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 36.dp, end = 36.dp, top = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            AppLogo(
                Modifier
                    .padding(top = 4.dp)
                    .width(92.dp)
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(formatClock(now), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(longDate(now), color = Color(0xFFB9A2B5), fontSize = 7.5.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                icon(TextPrimary)
                Spacer(Modifier.width(7.dp))
            }
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), content = actions)
        }
    }
}

/** "martes 3 de noviembre" */
fun longDate(millis: Long): String =
    java.text.SimpleDateFormat("EEEE d 'de' MMMM", java.util.Locale.forLanguageTag("es-MX")).format(java.util.Date(millis))

/** Pastilla de texto (categorías, temporadas, filtros y botones pequeños). */
@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.07f else 1f, label = "chip")
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .scale(scale)
            .shadow(if (focused || selected) 8.dp else 0.dp, shape, ambientColor = AccentColor, spotColor = AccentColor)
            .clip(shape)
            .then(
                when {
                    selected -> Modifier.background(PinkGradient)
                    focused -> Modifier.background(Color(0x33FFFFFF))
                    else -> Modifier.background(Color(0x14FFFFFF))
                }
            )
            .border(
                if (focused) 1.5.dp else 0.5.dp,
                if (focused) Color.White else Color(0x24FFFFFF),
                shape
            )
            .clickable(onClick = onClick)
            .height(if (small) 22.dp else 26.dp)
            .padding(horizontal = if (small) 11.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (selected || focused) Color.White else TextSecondary,
            fontSize = if (small) 8.5.sp else 9.5.sp,
            fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/**
 * Fila de categorías como pastillas. La elegida va en rosa; al entrar, el foco va a ella.
 */
@Composable
fun CategoryChips(
    rows: List<CategoryRow>,
    selectedId: String,
    onSelect: (String) -> Unit,
    selectedRequester: FocusRequester,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onChipFocus: (Boolean) -> Unit = {}
) {
    val selectedIndex = rows.indexOfFirst { it.id == selectedId }
    LaunchedEffect(rows.size) {
        if (selectedIndex > 2) listState.scrollToItem((selectedIndex - 2).coerceAtLeast(0))
    }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 36.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
            val label = when (row.id) {
                ROW_FAV -> "★  ${row.name}"
                else -> row.name
            }
            Chip(
                text = label,
                selected = row.id == selectedId,
                onClick = { onSelect(row.id) },
                modifier = Modifier
                    .then(if (index == selectedIndex) Modifier.focusRequester(selectedRequester) else Modifier)
                    .onFocusChanged { onChipFocus(it.isFocused) }
            )
        }
    }
}

/** Una opción de la barra de menú flotante. */
class DockItem(
    val label: String,
    val icon: @Composable (Color) -> Unit,
    val active: Boolean = false,
    val requester: FocusRequester? = null,
    val onClick: () -> Unit
)

/**
 * Barra de menú flotante de Lumina (abajo al centro): Buscar, Inicio, En vivo, Películas,
 * Series, Guía, Más y Ajustes. La sección actual va en rosa.
 */
@Composable
fun LuminaDock(
    vm: AppViewModel,
    active: DockTab,
    modifier: Modifier = Modifier,
    activeRequester: FocusRequester? = null,
    upTarget: FocusRequester? = null,
    onActive: () -> Unit = {}
) {
    var showMore by remember { mutableStateOf(false) }
    fun go(tab: DockTab, action: () -> Unit) {
        if (tab == active) onActive() else action()
    }
    val items = listOf(
        DockTab.SEARCH to DockItem("Buscar", { t -> Icon(Icons.Filled.Search, null, tint = t, modifier = Modifier.size(14.dp)) }) {
            go(DockTab.SEARCH) { vm.switchTo(Screen.Search(SearchScope.ALL)) }
        },
        DockTab.HOME to DockItem("Inicio", { t -> Icon(Icons.Filled.Home, null, tint = t, modifier = Modifier.size(14.dp)) }) {
            go(DockTab.HOME) { vm.switchTo(null) }
        },
        DockTab.LIVE to DockItem("En vivo", { t -> GlyphIcon(Glyph.TV, Modifier.size(14.dp), t) }) {
            go(DockTab.LIVE) { vm.switchTo(Screen.Live) }
        },
        DockTab.MOVIES to DockItem("Películas", { t -> GlyphIcon(Glyph.CLAPPER, Modifier.size(14.dp), t) }) {
            go(DockTab.MOVIES) { vm.switchTo(Screen.Movies) }
        },
        DockTab.SERIES to DockItem("Series", { t -> GlyphIcon(Glyph.SERIES, Modifier.size(14.dp), t) }) {
            go(DockTab.SERIES) { vm.switchTo(Screen.SeriesList) }
        },
        DockTab.GUIDE to DockItem("Guía", { t -> GlyphIcon(Glyph.EPG, Modifier.size(14.dp), t) }) {
            go(DockTab.GUIDE) { vm.switchTo(Screen.Epg) }
        },
        DockTab.MORE to DockItem("Más", { t -> GlyphIcon(Glyph.GRID, Modifier.size(14.dp), t) }) {
            showMore = true
        },
        DockTab.SETTINGS to DockItem("Ajustes", { t -> Icon(Icons.Filled.Settings, null, tint = t, modifier = Modifier.size(14.dp)) }) {
            go(DockTab.SETTINGS) { vm.switchTo(Screen.Settings) }
        }
    ).map { (tab, item) ->
        DockItem(
            label = item.label,
            icon = item.icon,
            active = tab == active,
            requester = if (tab == active) activeRequester else null,
            onClick = item.onClick
        )
    }

    DockBar(items = items, upTarget = upTarget, modifier = modifier)

    if (showMore) {
        OptionsDialog(
            title = "Más opciones",
            options = listOf(
                Opt("Continuar viendo") { vm.switchTo(Screen.Continue) },
                Opt("Mi lista") {
                    vm.homeShelfKey = "mine"
                    vm.homeIndex = 0
                    vm.switchTo(null)
                },
                Opt("Pantalla múltiple") { vm.switchTo(Screen.Multi) },
                Opt("Catch up (programas que ya pasaron)") { vm.switchTo(Screen.CatchUp) },
                Opt("Radio") { vm.switchTo(Screen.Radio) },
                Opt("Grabaciones") { vm.switchTo(Screen.Recordings) },
                Opt("Avisos") { vm.switchTo(Screen.Notifications) },
                Opt("Mi cuenta") { vm.switchTo(Screen.Account) },
                Opt("Cancelar") { }
            ),
            onDismiss = { showMore = false }
        )
    }
}

@Composable
private fun DockBar(items: List<DockItem>, upTarget: FocusRequester?, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xCC140818))
            .border(0.5.dp, Color(0x24FFFFFF), RoundedCornerShape(15.dp))
            .padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items.forEach { item ->
            var focused by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "dock")
            val shape = RoundedCornerShape(10.dp)
            val tint = if (focused) Color.White else if (item.active) LuminaPink else Color(0xFFB9A2B5)
            Column(
                modifier = Modifier
                    .then(if (item.requester != null) Modifier.focusRequester(item.requester) else Modifier)
                    .then(if (upTarget != null) Modifier.focusProperties { up = upTarget } else Modifier)
                    .onFocusChanged { focused = it.isFocused }
                    .scale(scale)
                    .shadow(if (focused) 10.dp else 0.dp, shape, ambientColor = AccentColor, spotColor = AccentColor)
                    .clip(shape)
                    .then(
                        when {
                            focused -> Modifier.background(Brush.linearGradient(listOf(Color(0xFFFF4FA3), AccentDeep)))
                            item.active -> Modifier.background(AccentColor.copy(alpha = 0.18f))
                            else -> Modifier
                        }
                    )
                    .border(if (focused) 1.5.dp else 0.dp, if (focused) Color.White else Color.Transparent, shape)
                    .clickable(onClick = item.onClick)
                    .size(48.dp, 39.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                item.icon(tint)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.label,
                    color = tint,
                    fontSize = 6.5.sp,
                    fontWeight = if (focused || item.active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

/** Botón en forma de píldora (Ver ahora, Más información, Mi lista…). */
@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.07f else 1f, label = "pill")
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .scale(scale)
            .shadow(if (primary || focused) 12.dp else 0.dp, shape, ambientColor = AccentColor, spotColor = AccentColor)
            .clip(shape)
            .then(
                when {
                    primary || focused -> Modifier.background(PinkGradient)
                    else -> Modifier.background(Color(0x1AFFFFFF))
                }
            )
            .border(
                if (focused) 2.dp else 0.5.dp,
                if (focused) Color.White else Color(0x2EFFFFFF),
                shape
            )
            .clickable(onClick = onClick)
            .height(30.dp)
            .padding(start = if (icon != null) 13.dp else 16.dp, end = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = if (primary) 10.5.sp else 9.5.sp,
            fontWeight = if (primary || focused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
