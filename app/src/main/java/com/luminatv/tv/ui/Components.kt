package com.luminatv.tv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.luminatv.tv.R

/**
 * Tarjeta enfocable con el control remoto: al enfocarla crece un poco y muestra un borde de color.
 * OK / Enter del control (o un toque) dispara onClick. Un toque largo, o la tecla Menú del control,
 * dispara onLongClick.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    scaleOnFocus: Float = 1.06f,
    selected: Boolean = false,
    background: Color = PanelColor,
    focusedBackground: Color = PanelFocusedColor,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) scaleOnFocus else 1f, label = "focusScale")
    val borderColor = when {
        focused -> FocusRingColor
        selected -> AccentColor.copy(alpha = 0.75f)
        else -> Color.Transparent
    }
    val borderWidth = when {
        focused -> 3.dp
        selected -> 2.dp
        else -> 0.dp
    }
    Box(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .scale(scale)
            // brillo rosa alrededor de lo que está enfocado
            .shadow(if (focused) 14.dp else 0.dp, shape, ambientColor = AccentColor, spotColor = AccentColor)
            .clip(shape)
            .background(if (focused) focusedBackground else background, shape)
            .border(borderWidth, borderColor, shape)
            .onKeyEvent { event ->
                if (onLongClick != null && event.type == KeyEventType.KeyDown && event.key == Key.Menu) {
                    onLongClick()
                    true
                } else {
                    false
                }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        content(focused)
    }
}

/** Ícono que acompaña al texto de un botón (se reconoce por el símbolo con que empieza el texto). */
private enum class BtnGlyph { PLAY, RESTART, TRAILER, FAV_ON, FAV_OFF, REC, STOP }

private val glyphPrefixes = listOf(
    "▶" to BtnGlyph.PLAY,
    "⟲" to BtnGlyph.RESTART,
    "🎬" to BtnGlyph.TRAILER,
    "★" to BtnGlyph.FAV_ON,
    "☆" to BtnGlyph.FAV_OFF,
    "●" to BtnGlyph.REC,
    "■" to BtnGlyph.STOP
)

/** "▶  Reproducir" -> (PLAY, "Reproducir"): el símbolo se cambia por un ícono de verdad. */
private fun splitGlyph(text: String): Pair<BtnGlyph?, String> {
    val t = text.trimStart()
    for ((prefix, glyph) in glyphPrefixes) {
        if (t.startsWith(prefix)) return glyph to t.removePrefix(prefix).trim()
    }
    return null to text
}

@Composable
private fun ButtonGlyph(glyph: BtnGlyph, tint: Color, size: Dp, focused: Boolean) {
    when (glyph) {
        BtnGlyph.PLAY -> Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = tint, modifier = Modifier.size(size + 4.dp))
        BtnGlyph.RESTART -> Icon(Icons.Filled.Refresh, contentDescription = null, tint = tint, modifier = Modifier.size(size))
        BtnGlyph.TRAILER -> GlyphIcon(Glyph.CLAPPER, Modifier.size(size), tint)
        BtnGlyph.FAV_ON -> Icon(Icons.Filled.Star, contentDescription = null, tint = if (focused) Color.White else StarColor, modifier = Modifier.size(size))
        BtnGlyph.FAV_OFF -> Icon(Icons.Filled.Add, contentDescription = null, tint = tint, modifier = Modifier.size(size + 2.dp))
        BtnGlyph.REC -> Box(
            Modifier
                .size(size * 0.62f)
                .clip(CircleShape)
                .background(if (focused) Color.White else Color(0xFFFF4D5E))
        )
        BtnGlyph.STOP -> Box(
            Modifier
                .size(size * 0.56f)
                .clip(RoundedCornerShape(2.dp))
                .background(tint)
        )
    }
}

/**
 * Botón de Lumina: "de vidrio" sin foco y rosa degradado con borde blanco al enfocarlo.
 * Si el texto empieza con ▶ ⟲ 🎬 ★ ☆ ● ■, se dibuja el ícono correspondiente en lugar del símbolo.
 */
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    val parts = remember(text) { splitGlyph(text) }
    val shape = RoundedCornerShape(if (compact) 8.dp else 10.dp)
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "btnScale")
    val fill: Modifier = when {
        focused -> Modifier.background(AccentBrush, shape)
        selected -> Modifier.background(AccentColor.copy(alpha = 0.22f), shape)
        else -> Modifier.background(GlassColor, shape)
    }
    val borderColor = when {
        focused -> Color.White.copy(alpha = 0.92f)
        selected -> AccentColor
        else -> GlassBorder
    }
    Row(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .scale(scale)
            .shadow(if (focused) 12.dp else 0.dp, shape, ambientColor = AccentColor, spotColor = AccentColor)
            .clip(shape)
            .then(fill)
            .border(if (focused) 2.dp else 1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (compact) 12.dp else 22.dp, vertical = if (compact) 8.dp else 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = when {
            !enabled -> TextTertiary
            focused -> Color.White
            else -> TextPrimary
        }
        val glyph = parts.first
        if (glyph != null) {
            ButtonGlyph(glyph, tint, if (compact) 14.dp else 18.dp, focused)
            Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
        }
        Text(
            text = parts.second,
            color = tint,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 12.sp else 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Botón redondo con un ícono (barras superiores). */
@Composable
fun IconAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    content: @Composable BoxScope.() -> Unit
) {
    FocusCard(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        scaleOnFocus = 1.1f,
        background = Color.Transparent
    ) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center, content = content)
    }
}

/** Proporción del logo de Lumina TV (res/drawable-nodpi/logo_lumina.png). */
const val LOGO_RATIO = 1600f / 363f

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Image(
        bitmap = rememberArt(R.drawable.logo_lumina),
        contentDescription = stringResource(R.string.app_name),
        contentScale = ContentScale.Fit,
        modifier = modifier.aspectRatio(LOGO_RATIO)
    )
}

class MenuEntry(val label: String, val checked: Boolean = false, val onClick: () -> Unit)

/** Botón de tres puntos con su menú desplegable. */
@Composable
fun MoreMenu(entries: List<MenuEntry>) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconAction(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones", tint = TextPrimary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(if (entry.checked) "✓  ${entry.label}" else entry.label) },
                    onClick = {
                        open = false
                        entry.onClick()
                    }
                )
            }
        }
    }
}

/** Barra superior: atrás, logo, título al centro, búsqueda y menú de tres puntos. */
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    showLogo: Boolean = true,
    centerTitle: Boolean = false,
    onSearch: (() -> Unit)? = null,
    menu: List<MenuEntry> = emptyList(),
    leading: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconAction(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = TextPrimary)
            }
            Spacer(Modifier.width(6.dp))
        }
        leading()
        if (showLogo) {
            AppLogo(Modifier.height(34.dp))
            Spacer(Modifier.width(12.dp))
            if (!centerTitle && title.isNotBlank()) {
                // separador fino entre el logo y el nombre de la sección
                Box(
                    Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(Color.White.copy(alpha = 0.22f))
                )
                Spacer(Modifier.width(12.dp))
            }
        }
        Box(
            Modifier.weight(1f),
            contentAlignment = if (centerTitle) Alignment.Center else Alignment.CenterStart
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing()
        if (menu.isNotEmpty()) {
            MoreMenu(menu)
            Spacer(Modifier.width(4.dp))
        }
        // La lupa va siempre en la esquina superior derecha.
        if (onSearch != null) {
            IconAction(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = TextPrimary)
            }
        }
    }
}

/** Campo de texto de una línea con lupa, como "Buscar en categorías". */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    requester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Color(0x33FFFFFF), shape)
            .border(2.dp, if (focused) AccentColor else Color.Transparent, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = TextSecondary, fontSize = 15.sp, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(AccentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
                    .onFocusChanged { focused = it.isFocused }
            )
        }
    }
}

class Opt(val label: String, val action: () -> Unit)

/** Ventana con una lista de opciones (confirmaciones, elegir duración, etc.). */
@Composable
fun OptionsDialog(
    title: String,
    options: List<Opt>,
    onDismiss: () -> Unit,
    message: String? = null
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        withFrameNanos { }
        first.tryRequestFocus()
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1C0B22), Color(0xFF0E0513))))
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            if (message != null) {
                Text(message, color = TextSecondary)
            }
            options.forEachIndexed { index, option ->
                TvButton(
                    text = option.label,
                    onClick = {
                        onDismiss()
                        option.action()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (index == 0) Modifier.focusRequester(first) else Modifier)
                )
            }
        }
    }
}

// ---------------------------------------------------------------- imágenes

@Composable
fun PosterImage(url: String?, title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(Color(0xFF2B1433), Color(0xFF140818)))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title.take(1).uppercase(),
            style = MaterialTheme.typography.displaySmall,
            color = TextSecondary
        )
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Calificación sobre el póster: estrella dorada y número, en una pastilla oscura. */
@Composable
fun RatingBadge(rating10: Float, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xB30A0310))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = StarColor, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(2.dp))
        Text(
            text = String.format(Locale.US, "%.1f", rating10),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Póster limpio con la calificación y la estrella de favorito; el título aparece al enfocarlo. */
@Composable
fun PosterCard(
    title: String,
    imageUrl: String?,
    rating10: Float,
    favorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    FocusCard(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        scaleOnFocus = 1.05f
    ) { focused ->
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        ) {
            PosterImage(url = imageUrl, title = title, modifier = Modifier.fillMaxSize())
            // Las portadas ya traen su título: el nombre se muestra solo al enfocarla (o si no hay imagen).
            if (focused || imageUrl.isNullOrBlank()) Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xD9000000))))
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (rating10 >= 0.5f) {
                RatingBadge(rating10, Modifier.align(Alignment.TopStart).padding(5.dp))
            }
            if (favorite) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Favorito",
                    tint = StarColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ChannelLogo(icon: String?, name: String, size: Dp = 48.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF22102A)),
        contentAlignment = Alignment.Center
    ) {
        if (icon == null) {
            Text(text = name.take(1).uppercase(), color = TextSecondary)
        } else {
            AsyncImage(
                model = icon,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        }
    }
}

// ----------------------------------------------------------------- estados

@Composable
fun CenterLoading(modifier: Modifier = Modifier, text: String? = null) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = AccentColor, modifier = Modifier.size(40.dp))
            if (text != null) {
                Text(text, color = TextSecondary, textAlign = TextAlign.Center, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun CenterMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
fun ErrorWithRetry(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = message, color = ErrorColor, textAlign = TextAlign.Center)
            TvButton(text = "Reintentar", onClick = onRetry)
        }
    }
}

/** Pide el foco sin romper la app si el elemento todavía no está en pantalla. */
fun FocusRequester.tryRequestFocus() {
    try {
        requestFocus()
    } catch (e: Exception) {
        // El elemento aún no existe en la composición: se ignora.
    }
}

fun Modifier.focusIf(condition: Boolean, requester: FocusRequester): Modifier =
    if (condition) this.focusRequester(requester) else this
