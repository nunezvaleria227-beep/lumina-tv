package com.luminatv.tv.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.runtime.remember
import com.luminatv.tv.R

// Paleta Lumina TV: morado muy oscuro con rosa magenta (como el logo y el banner).
val BackgroundColor = Color(0xFF0C0410)
val BackgroundGlow = Color(0xFF1A0820)
val SidebarColor = Color(0xFF0A0310)
val PanelColor = Color(0xFF170A1C)
val PanelFocusedColor = Color(0xFF2B1433)
val RowSelectedColor = Color(0x14FFFFFF)
val AccentColor = Color(0xFFF2268A)
val AccentLight = Color(0xFFFF7CC3)
val AccentDeep = Color(0xFFB0127A)
val OnAccentColor = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFFFBF3F9)
val TextSecondary = Color(0xFFCDB8C9)
val TextTertiary = Color(0xFF9F8A9C)
val AccentText = Color(0xFFFF6FB5)
val FocusRingColor = Color(0xFFFFFFFF)
val LineColor = Color(0xFF33203A)
val ErrorColor = Color(0xFFFF6B7A)
val StarColor = Color(0xFFFFC93C)

/** Fondo "de vidrio" de botones y tarjetas sin foco (se ve el fondo detrás, más elegante). */
val GlassColor = Color(0x1AFFFFFF)
val GlassBorder = Color(0x1FFFFFFF)

/** Degradado rosa de la marca (botones principales, insignias). */
val AccentBrush = Brush.horizontalGradient(listOf(AccentLight, AccentColor, AccentDeep))

/** Letra de toda la app: Poppins (la de la marca Lumina). */
val LuminaFont = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_bold, FontWeight.ExtraBold),
    Font(R.font.poppins_bold, FontWeight.Black)
)

private fun TextStyle.lumina(weight: FontWeight? = null, tracking: Float? = null) = copy(
    fontFamily = LuminaFont,
    fontWeight = weight ?: fontWeight,
    letterSpacing = tracking?.sp ?: letterSpacing
)

private val LuminaTypography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.lumina(FontWeight.ExtraBold, -0.5f),
        displayMedium = t.displayMedium.lumina(FontWeight.ExtraBold, -0.5f),
        displaySmall = t.displaySmall.lumina(FontWeight.Bold, -0.25f),
        headlineLarge = t.headlineLarge.lumina(FontWeight.ExtraBold, -0.25f),
        headlineMedium = t.headlineMedium.lumina(FontWeight.ExtraBold, -0.25f),
        headlineSmall = t.headlineSmall.lumina(FontWeight.Bold),
        titleLarge = t.titleLarge.lumina(FontWeight.Bold),
        titleMedium = t.titleMedium.lumina(FontWeight.Bold),
        titleSmall = t.titleSmall.lumina(FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.lumina(tracking = 0f),
        bodyMedium = t.bodyMedium.lumina(tracking = 0f),
        bodySmall = t.bodySmall.lumina(tracking = 0f),
        labelLarge = t.labelLarge.lumina(FontWeight.SemiBold),
        labelMedium = t.labelMedium.lumina(FontWeight.SemiBold),
        labelSmall = t.labelSmall.lumina(FontWeight.Medium)
    )
}

@Composable
fun MiTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = LuminaTypography,
        colorScheme = darkColorScheme(
            primary = AccentColor,
            onPrimary = OnAccentColor,
            background = BackgroundColor,
            onBackground = TextPrimary,
            surface = PanelColor,
            onSurface = TextPrimary,
            surfaceVariant = PanelFocusedColor,
            onSurfaceVariant = TextSecondary,
            error = ErrorColor
        ),
        content = content
    )
}

private val artCache = HashMap<Int, ImageBitmap>()

/** Imagen de res/drawable decodificada una sola vez (las pantallas cambian seguido y así no se vuelve a leer). */
@Composable
fun rememberArt(@DrawableRes id: Int): ImageBitmap {
    val resources = LocalContext.current.applicationContext.resources
    return remember(id) { artCache.getOrPut(id) { ImageBitmap.imageResource(resources, id) } }
}

/**
 * Fondo común a todas las pantallas: una imagen (por defecto el fondo morado de Lumina TV)
 * oscurecida un poco, [dim] de 0 a 1, para que el texto se lea bien encima.
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    @DrawableRes art: Int = R.drawable.bg_home,
    dim: Float = 0.30f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Image(
            bitmap = rememberArt(art),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SidebarColor.copy(alpha = dim * 0.7f),
                            SidebarColor.copy(alpha = dim)
                        )
                    )
                )
        )
        content()
    }
}
