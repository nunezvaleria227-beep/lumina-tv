package com.luminatv.tv.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.luminatv.tv.R
import com.luminatv.tv.data.Kind
import com.luminatv.tv.data.Movie
import com.luminatv.tv.data.MovieInfo
import com.luminatv.tv.data.ResumeEntry
import com.luminatv.tv.data.SeriesItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Una portada del carrusel del inicio. */
private class Cover(
    val key: String,
    val title: String,
    val image: String?,
    val movie: Movie? = null,
    val series: SeriesItem? = null,
    val resume: ResumeEntry? = null
)

/** Una fila del inicio (Continuar viendo, Estrenos, Series nuevas…). */
private class Shelf(val key: String, val title: String, val covers: List<Cover>)

private fun Movie.toCover(prefix: String) = Cover(prefix + id, name, poster, movie = this)
private fun SeriesItem.toCover(prefix: String) = Cover(prefix + id, name, cover, series = this)

// Posición de las portadas según su distancia a la del centro (0, 1, 2, 3, 4).
private val COVER_X = floatArrayOf(0f, 150f, 268f, 366f, 446f)
private val COVER_SCALE = floatArrayOf(1f, 0.833f, 0.70f, 0.60f, 0.52f)
private val COVER_ROT = floatArrayOf(0f, 28f, 36f, 42f, 46f)
private val COVER_ALPHA = floatArrayOf(1f, 0.82f, 0.56f, 0.30f, 0f)

private fun lerpTable(table: FloatArray, distance: Float): Float {
    val d = distance.coerceIn(0f, (table.size - 1).toFloat())
    val i = d.toInt().coerceAtMost(table.size - 2)
    val t = d - i
    return table[i] + (table[i + 1] - table[i]) * t
}

private val CardShape = RoundedCornerShape(10.dp)
private val PinkButton = Brush.linearGradient(listOf(Color(0xFFFF4FA3), Color(0xFFC2167F)))
private val ShelfPink = Color(0xFFFF9ED0)
private val Muted = Color(0xFF8E7A8B)
private val Night = Color(0xFF0A0310)

/**
 * Inicio de Lumina TV: un carrusel de portadas en 3D al centro, el fondo toma los colores
 * de la portada elegida, abajo el título con sus botones y una barra de menú flotante.
 */
@Composable
fun HomeScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmExit by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var stageFocused by remember { mutableStateOf(false) }
    var headerFocused by remember { mutableStateOf(false) }
    val headerFR = remember { FocusRequester() }
    val stageFR = remember { FocusRequester() }
    val playFR = remember { FocusRequester() }
    val dockFR = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        vm.live.ensureLoaded()
        vm.movies.ensureLoaded()
        vm.series.ensureLoaded()
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }

    // ------------------------------------------------------------------ filas
    val movies = vm.movies.visibleItems
    val seriesItems = vm.series.visibleItems
    val resume = vm.resumeEntries
    val favMovies = vm.movies.favoriteList
    val favSeries = vm.series.favoriteList
    val newMovies = remember(movies) { movies.sortedByDescending { it.added }.take(30) }
    val topMovies = remember(movies) {
        movies.filter { it.rating10 >= 7f }.sortedByDescending { it.rating10 }.take(30)
    }
    val newSeries = remember(seriesItems) { seriesItems.sortedByDescending { it.added }.take(30) }
    val shelves = remember(resume, newMovies, newSeries, topMovies, favMovies, favSeries) {
        buildList {
            if (resume.isNotEmpty()) {
                add(Shelf("resume", "Continuar viendo", resume.map { Cover("r" + it.key, it.title, it.image, resume = it) }))
            }
            if (newMovies.isNotEmpty()) add(Shelf("new", "Estrenos", newMovies.map { it.toCover("n") }))
            if (newSeries.isNotEmpty()) add(Shelf("series", "Series nuevas", newSeries.map { it.toCover("s") }))
            if (topMovies.isNotEmpty()) add(Shelf("top", "Mejor calificadas", topMovies.map { it.toCover("t") }))
            val mine = favMovies.map { it.toCover("fm") } + favSeries.map { it.toCover("fs") }
            if (mine.isNotEmpty()) add(Shelf("mine", "Mi lista", mine))
        }
    }

    val shelfIdx = shelves.indexOfFirst { it.key == vm.homeShelfKey }.let { found ->
        if (found >= 0) found else shelves.indexOfFirst { it.key == "new" }.coerceAtLeast(0)
    }
    val shelf = shelves.getOrNull(shelfIdx)
    val count = shelf?.covers?.size ?: 0
    val index = vm.homeIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
    val current = shelf?.covers?.getOrNull(index)

    fun changeShelf(step: Int) {
        if (shelves.isEmpty()) return
        val next = (shelfIdx + step).coerceIn(0, shelves.size - 1)
        if (next == shelfIdx) return
        vm.homeShelfKey = shelves[next].key
        vm.homeIndex = 0
    }

    fun moveCover(step: Int) {
        if (count == 0) return
        vm.homeIndex = (vm.homeIndex.coerceIn(0, count - 1) + step).coerceIn(0, count - 1)
    }

    // Ficha de la película enfocada (fondo grande, año, género y duración); se pide al detenerse en ella.
    val infos = remember { mutableStateMapOf<String, MovieInfo?>() }
    val focusedMovie = current?.movie
    LaunchedEffect(focusedMovie?.id) {
        val movie = focusedMovie ?: return@LaunchedEffect
        if (infos.containsKey(movie.id)) return@LaunchedEffect
        delay(350)
        infos[movie.id] = try {
            vm.loadMovieInfo(movie)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }
    val info = focusedMovie?.let { infos[it.id] }

    // Movimiento suave del carrusel.
    val pos = remember { Animatable(index.toFloat()) }
    val stageAlpha = remember { Animatable(1f) }
    var lastShelf by remember { mutableStateOf(shelf?.key) }
    LaunchedEffect(index, shelf?.key) {
        if (shelf?.key != lastShelf) {
            lastShelf = shelf?.key
            pos.snapTo(index.toFloat())
            launch {
                stageAlpha.snapTo(0f)
                stageAlpha.animateTo(1f, tween(380))
            }
        } else {
            pos.animateTo(index.toFloat(), spring(dampingRatio = 0.86f, stiffness = 280f))
        }
    }

    // Al entrar, el foco va al carrusel.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        withFrameNanos { }
        stageFR.tryRequestFocus()
    }

    BackHandler {
        if (stageFocused) confirmExit = true else stageFR.tryRequestFocus()
    }

    fun primary(cover: Cover) {
        when {
            cover.resume != null -> vm.playResume(context, cover.resume)
            cover.movie != null -> vm.openMovie(cover.movie)
            cover.series != null -> vm.openSeries(cover.series)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Night)
    ) {
        // ------------------------------------------------ fondo con los colores de la portada
        val ambient = info?.backdrop?.takeIf { it.isNotBlank() } ?: current?.image?.takeIf { it.isNotBlank() }
        Crossfade(targetState = ambient, animationSpec = tween(700), label = "ambient") { url ->
            if (url == null) {
                Image(
                    bitmap = rememberArt(R.drawable.bg_home),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.7f }
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .memoryCacheKey("ambient:$url")
                        .size(72)
                        .precision(Precision.EXACT)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.62f }
                        .blur(48.dp)
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0.25f to Night.copy(alpha = 0f),
                        1f to Night.copy(alpha = 0.88f),
                        radius = 1500f
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(0f to Night.copy(alpha = 0f), 0.5f to Night))
        )

        // ------------------------------------------------ logo y reloj
        AppLogo(
            Modifier
                .padding(start = 36.dp, top = 22.dp)
                .width(104.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 36.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(formatClock(now), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(homeDate(now), color = Color(0xFFB9A2B5), fontSize = 8.sp, fontWeight = FontWeight.Medium)
        }

        // ------------------------------------------------ nombre de la fila (▲ para cambiarla)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp)
                .focusRequester(headerFR)
                .onFocusChanged { headerFocused = it.isFocused }
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionLeft -> { changeShelf(-1); true }
                        Key.DirectionRight -> { changeShelf(1); true }
                        Key.DirectionDown, Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> {
                            stageFR.tryRequestFocus(); true
                        }
                        Key.DirectionUp -> true
                        else -> false
                    }
                }
                .focusable(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val prev = shelves.getOrNull(shelfIdx - 1)?.title
            val next = shelves.getOrNull(shelfIdx + 1)?.title
            Text(
                text = when {
                    !headerFocused -> if (shelves.size > 1) "▲  Cambiar fila" else " "
                    else -> listOfNotNull(prev?.let { "◀  $it" }, next?.let { "$it  ▶" }).joinToString("     ·     ")
                },
                color = Muted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (headerFocused) AccentColor.copy(alpha = 0.22f) else Color.Transparent)
                    .border(1.dp, if (headerFocused) AccentLight else Color.Transparent, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Sparkle(9.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = (shelf?.title ?: "Inicio").uppercase(),
                    color = ShelfPink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }

        // ------------------------------------------------ carrusel de portadas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .offset(y = 93.dp)
                .focusRequester(stageFR)
                .onFocusChanged { stageFocused = it.isFocused }
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionLeft -> { moveCover(-1); true }
                        Key.DirectionRight -> { moveCover(1); true }
                        Key.DirectionUp -> { headerFR.tryRequestFocus(); true }
                        Key.DirectionDown -> {
                            if (current != null) playFR.tryRequestFocus() else dockFR.tryRequestFocus()
                            true
                        }
                        else -> false
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { current?.let { primary(it) } }
                .graphicsLayer { alpha = stageAlpha.value },
            contentAlignment = Alignment.Center
        ) {
            if (shelf == null) {
                EmptyHome(loading = vm.movies.loading || vm.series.loading)
            } else {
                val first = (index - 5).coerceAtLeast(0)
                val last = (index + 5).coerceAtMost(count - 1)
                for (i in first..last) {
                    val cover = shelf.covers[i]
                    key(cover.key) {
                        CoverCard(
                            cover = cover,
                            isCenter = i == index,
                            ringOn = stageFocused && i == index,
                            modifier = Modifier
                                .zIndex(10f - abs(i - index))
                                .graphicsLayer {
                                    val d = i - pos.value
                                    val a = abs(d)
                                    val s = lerpTable(COVER_SCALE, a)
                                    translationX = sign(d) * lerpTable(COVER_X, a) * density
                                    rotationY = -sign(d) * lerpTable(COVER_ROT, a)
                                    scaleX = s
                                    scaleY = s
                                    alpha = lerpTable(COVER_ALPHA, a)
                                    cameraDistance = 14f * density
                                }
                        )
                    }
                }
            }
        }

        // ------------------------------------------------ título, datos y botones
        if (current != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 340.dp)
                    .width(720.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = current.title,
                    color = TextPrimary,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                MetaLine(current, info)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val playLabel = when {
                        current.resume != null -> "Continuar"
                        current.series != null -> "Ver serie"
                        else -> "Ver ahora"
                    }
                    PillButton(
                        text = playLabel,
                        primary = true,
                        modifier = Modifier
                            .focusRequester(playFR)
                            .focusProperties { up = stageFR; down = dockFR }
                    ) {
                        when {
                            current.resume != null -> vm.playResume(context, current.resume)
                            current.movie != null -> vm.playMovie(context, current.movie, info = info)
                            current.series != null -> vm.openSeries(current.series)
                        }
                    }
                    val resumeMovie = current.resume?.takeIf { it.type == "movie" }?.let { vm.movies.catalog?.byId?.get(it.id) }
                    if (current.movie != null || current.series != null || resumeMovie != null) {
                        PillButton(
                            text = "Más información",
                            modifier = Modifier.focusProperties { up = stageFR; down = dockFR }
                        ) {
                            when {
                                current.movie != null -> vm.openMovie(current.movie)
                                current.series != null -> vm.openSeries(current.series)
                                resumeMovie != null -> vm.openMovie(resumeMovie)
                            }
                        }
                    }
                    val favKind = if (current.series != null) Kind.SERIES else Kind.MOVIES
                    val favId = current.movie?.id ?: current.series?.id
                    if (favId != null) {
                        val fav = vm.isFavorite(favKind, favId)
                        PillButton(
                            text = if (fav) "En mi lista" else "Mi lista",
                            icon = if (fav) Icons.Filled.Check else Icons.Filled.Add,
                            modifier = Modifier.focusProperties { up = stageFR; down = dockFR }
                        ) { vm.toggleFavorite(favKind, favId) }
                    }
                }
            }
        }

        // ------------------------------------------------ filas (abajo a la izquierda)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 36.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            shelves.forEachIndexed { i, s ->
                val on = i == shelfIdx
                Text(
                    text = if (on) "●  ${s.title}" else s.title,
                    color = if (on) ShelfPink else Muted,
                    fontSize = 8.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
        Text(
            text = "◀ ▶  recorrer   ·   ▲  cambiar fila",
            color = Muted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 36.dp, bottom = 28.dp)
        )

        // ------------------------------------------------ barra de menú flotante
        Dock(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            homeRequester = dockFR,
            upTarget = if (current != null) playFR else stageFR,
            items = listOf(
                DockItem("Buscar", { t -> Icon(Icons.Filled.Search, null, tint = t, modifier = Modifier.size(14.dp)) }) {
                    vm.open(Screen.Search(SearchScope.ALL))
                },
                DockItem("Inicio", { t -> Icon(Icons.Filled.Home, null, tint = t, modifier = Modifier.size(14.dp)) }, home = true) {
                    stageFR.tryRequestFocus()
                },
                DockItem("En vivo", { t -> GlyphIcon(Glyph.TV, Modifier.size(14.dp), t) }) { vm.open(Screen.Live) },
                DockItem("Películas", { t -> GlyphIcon(Glyph.CLAPPER, Modifier.size(14.dp), t) }) { vm.open(Screen.Movies) },
                DockItem("Series", { t -> GlyphIcon(Glyph.SERIES, Modifier.size(14.dp), t) }) { vm.open(Screen.SeriesList) },
                DockItem("Guía", { t -> GlyphIcon(Glyph.EPG, Modifier.size(14.dp), t) }) { vm.open(Screen.Epg) },
                DockItem("Más", { t -> GlyphIcon(Glyph.GRID, Modifier.size(14.dp), t) }) { showMore = true },
                DockItem("Ajustes", { t -> Icon(Icons.Filled.Settings, null, tint = t, modifier = Modifier.size(14.dp)) }) {
                    vm.open(Screen.Settings)
                }
            )
        )
    }

    UpdateDialogs(vm)

    if (showMore) {
        OptionsDialog(
            title = "Más opciones",
            options = listOf(
                Opt("Continuar viendo") { vm.open(Screen.Continue) },
                Opt("Mi lista") {
                    vm.homeShelfKey = "mine"
                    vm.homeIndex = 0
                    scope.launch {
                        withFrameNanos { }
                        stageFR.tryRequestFocus()
                    }
                },
                Opt("Pantalla múltiple") { vm.open(Screen.Multi) },
                Opt("Catch up (programas que ya pasaron)") { vm.open(Screen.CatchUp) },
                Opt("Radio") { vm.open(Screen.Radio) },
                Opt("Grabaciones") { vm.open(Screen.Recordings) },
                Opt("Avisos") { vm.open(Screen.Notifications) },
                Opt("Mi cuenta") { vm.open(Screen.Account) },
                Opt("Cancelar") { }
            ),
            onDismiss = { showMore = false }
        )
    }

    if (confirmExit) {
        OptionsDialog(
            title = "¿Qué quieres hacer?",
            options = listOf(
                Opt("Seguir viendo") { },
                Opt("Cerrar sesión") { vm.logout() },
                Opt("Salir de la aplicación") { context.findActivity()?.finish() }
            ),
            onDismiss = { confirmExit = false }
        )
    }
}

// ------------------------------------------------------------------ portada

@Composable
private fun CoverCard(cover: Cover, isCenter: Boolean, ringOn: Boolean, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(150.dp, 225.dp)
            .shadow(
                elevation = if (ringOn) 26.dp else 14.dp,
                shape = CardShape,
                ambientColor = if (ringOn) AccentColor else Color.Black,
                spotColor = if (ringOn) AccentColor else Color.Black
            )
            .clip(CardShape)
            .background(PanelColor)
            .border(if (ringOn) 2.dp else 0.5.dp, if (ringOn) Color.White else Color(0x22FFFFFF), CardShape)
    ) {
        if (cover.image.isNullOrBlank()) {
            Text(
                text = cover.title,
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(12.dp)
            )
        } else {
            AsyncImage(
                model = cover.image,
                contentDescription = cover.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (!isCenter) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Night.copy(alpha = 0f), Night.copy(alpha = 0.92f))))
            )
            Text(
                text = cover.title,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
        val resume = cover.resume
        if (resume != null && resume.durationMs > 0) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0x55FFFFFF))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(resume.fraction.coerceIn(0.02f, 1f))
                        .height(4.dp)
                        .background(AccentBrush)
                )
            }
        }
    }
}

/** Año, género, duración, calificación y "HD" de lo enfocado. */
@Composable
private fun MetaLine(cover: Cover, info: MovieInfo?) {
    val parts = mutableListOf<String>()
    var rating = 0f
    when {
        cover.resume != null -> {
            cover.resume.subtitle?.takeIf { it.isNotBlank() }?.let { parts += it }
            if (cover.resume.positionMs > 0) parts += "Te quedaste en " + formatDuration(cover.resume.positionMs / 1000)
        }
        cover.movie != null -> {
            info?.releaseDate?.take(4)?.takeIf { it.length == 4 }?.let { parts += it }
            info?.genre?.takeIf { it.isNotBlank() }?.let { g ->
                parts += g.split(',', '/').map { it.trim() }.filter { it.isNotEmpty() }.take(2).joinToString(", ")
            }
            info?.duration?.takeIf { it.isNotBlank() }?.let { parts += prettyRuntime(it) }
            rating = cover.movie.rating10
        }
        cover.series != null -> {
            parts += "Serie"
            rating = cover.series.rating10
        }
    }
    Row(
        modifier = Modifier.padding(top = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        parts.forEachIndexed { i, p ->
            if (i > 0) Text("  •  ", color = Color(0xFF6A4A6E), fontSize = 10.sp)
            Text(p, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        if (rating >= 0.5f) {
            if (parts.isNotEmpty()) Spacer(Modifier.width(10.dp))
            Icon(Icons.Filled.Star, contentDescription = null, tint = StarColor, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                String.format(java.util.Locale.US, "%.1f", rating),
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (cover.resume == null) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = "HD",
                color = TextSecondary,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(0.5.dp, Color(0xFF6A4A6E), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 0.dp)
            )
        }
    }
}

/** "martes 3 de noviembre" */
private fun homeDate(millis: Long): String =
    java.text.SimpleDateFormat("EEEE d 'de' MMMM", java.util.Locale.forLanguageTag("es-MX")).format(java.util.Date(millis))

/** "02:08:00" o "128 min" → "2 h 08 min". */
private fun prettyRuntime(raw: String): String {
    val t = raw.trim()
    val clock = Regex("""^(\d{1,2}):(\d{2})(?::(\d{2}))?$""").find(t)
    val minutes = when {
        clock != null -> clock.groupValues[1].toInt() * 60 + clock.groupValues[2].toInt()
        else -> Regex("""^(\d{2,3})\s*(min)?$""").find(t)?.groupValues?.get(1)?.toIntOrNull()
    } ?: return t
    if (minutes <= 0) return t
    return if (minutes >= 60) "${minutes / 60} h ${"%02d".format(minutes % 60)} min" else "$minutes min"
}

@Composable
private fun BoxScope.EmptyHome(loading: Boolean) {
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (loading) {
            CircularProgressIndicator(color = AccentLight, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(12.dp))
            Text("Cargando tu contenido…", color = TextSecondary, fontSize = 12.sp)
        } else {
            Text("Aún no hay películas ni series", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Entra a En vivo, Películas o Series desde el menú de abajo.", color = TextSecondary, fontSize = 11.sp)
        }
    }
}

// ------------------------------------------------------------------ botones

@Composable
private fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = if (primary) Icons.Filled.PlayArrow else null,
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
                    primary || focused -> Modifier.background(PinkButton)
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
            fontWeight = if (primary || focused) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private class DockItem(
    val label: String,
    val icon: @Composable (Color) -> Unit,
    val home: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun Dock(modifier: Modifier, homeRequester: FocusRequester, upTarget: FocusRequester, items: List<DockItem>) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0x14FFFFFF))
            .border(0.5.dp, Color(0x24FFFFFF), RoundedCornerShape(15.dp))
            .padding(horizontal = 7.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items.forEach { item ->
            var focused by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "dock")
            val shape = RoundedCornerShape(10.dp)
            val tint = if (focused) Color.White else if (item.home) ShelfPink else Color(0xFFB9A2B5)
            Column(
                modifier = Modifier
                    .then(if (item.home) Modifier.focusRequester(homeRequester) else Modifier)
                    .focusProperties { up = upTarget }
                    .onFocusChanged { focused = it.isFocused }
                    .scale(scale)
                    .shadow(if (focused) 10.dp else 0.dp, shape, ambientColor = AccentColor, spotColor = AccentColor)
                    .clip(shape)
                    .then(
                        when {
                            focused -> Modifier.background(Brush.linearGradient(listOf(Color(0xFFFF4FA3), AccentDeep)))
                            item.home -> Modifier.background(AccentColor.copy(alpha = 0.16f))
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
                    fontWeight = if (focused || item.home) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

/** Destello de cuatro puntas de la marca. */
@Composable
private fun Sparkle(size: Dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w / 2, 0f)
            lineTo(w * 0.61f, h * 0.39f)
            lineTo(w, h / 2)
            lineTo(w * 0.61f, h * 0.61f)
            lineTo(w / 2, h)
            lineTo(w * 0.39f, h * 0.61f)
            lineTo(0f, h / 2)
            lineTo(w * 0.39f, h * 0.39f)
            close()
        }
        drawPath(path, ShelfPink)
    }
}
